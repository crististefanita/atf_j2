package atf.sql.tests.support.acceptance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Pattern;

/** Independent expected data over bounded observation bodies; assertions elsewhere cannot satisfy SQL facts. */
final class SqlReportOracle {
    private static final ObjectMapper JSON=new ObjectMapper().enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY).enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    private static final List<String> METHODS=List.of("capturesTypedValuesDuplicateLabelsAndRows","witnessesCommitAndRollbackFromIndependentConnections",
            "rejectsAggregateCaptureOverflowWithoutReturningACompleteResult","capturesBinaryAndTemporalValuesWithoutLeakingMutableJdbcObjects",
            "marksPartialCaptureAndPreservesSqlFailure");
    private record Captured(JsonNode data,String owner,int start,int end) { }
    private SqlReportOracle() { }
    static void require(String html,String selected,boolean negative) throws Exception {
        String body=html.replaceAll("(?is)<script\\b[^>]*>.*?</script>|<style\\b[^>]*>.*?</style>","");
        var captured=new ArrayList<Captured>();
        var matcher=Pattern.compile("(?is)<pre\\b[^>]*>(.*?)</pre>").matcher(body);
        while(matcher.find()) {
            String text=unescape(matcher.group(1));if(!text.stripLeading().startsWith("{"))continue;
            JsonNode data;try{data=JSON.readTree(text);}catch(java.io.IOException malformed){throw new AssertionError("Malformed captured SQL JSON",malformed);}
            if(!data.has("executionId") || !data.has("query"))continue;
            String prefix=body.substring(0,matcher.start()),owner=null;int position=-1;
            for(String method:METHODS){int found=prefix.lastIndexOf(method);if(found>position){position=found;owner=method;}}
            captured.add(new Captured(data,owner,matcher.start(1),matcher.end(1)));
        }
        int expected=selected.equals("suite")?9:selected.equals("results")?2:selected.equals("transactions")?3:4;
        if(captured.size()!=expected)throw new AssertionError("SQL snapshot population "+captured.size()+" != "+expected);
        var metadata=Pattern.compile("(?is)<td>SQL execution ID</td>\\s*<td[^>]*>(.*?)</td>").matcher(body).results()
                .map(match->unescape(match.group(1).replaceAll("<[^>]+>","")).strip()).toList();
        if(metadata.size()!=captured.size())throw new AssertionError("SQL identity metadata missing");
        for(int index=0;index<metadata.size();index++)equal(metadata.get(index),captured.get(index).data.path("executionId").asText());
        var ids=new HashSet<String>();var observedOwners=new HashSet<String>();int transaction=0;
        for(var item:captured) {
            var data=item.data;String query=data.path("query").asText(),owner;
            if(!ids.add(data.path("executionId").asText()) || data.path("executionId").asText().isBlank())throw new AssertionError("Lost execution identity");
            if(query.startsWith("SELECT CAST(? AS DECIMAL")) {
                owner=METHODS.get(0);equal(4,data.path("columns").size());equal("SAME",data.path("columns").get(0).path("label").asText());
                equal("SAME",data.path("columns").get(1).path("label").asText());
                decimal(data.path("parameters").get(0).path("value"));decimal(data.path("rows").get(0).get(0).path("value"));
                equal("null",data.path("rows").get(0).get(1).path("value").asText());
                if(!data.path("rows").get(0).get(2).path("value").isNull())throw new AssertionError("SQL NULL changed");
                equal("",data.path("rows").get(0).get(3).path("value").textValue());equal("COMPLETE",data.path("outcome").asText());
            } else if(query.equals("SELECT * FROM (VALUES (1), (1))")) {
                owner=METHODS.get(0);equal(2,data.path("rows").size());
                for(var row:data.path("rows"))equal(1,row.get(0).path("value").intValue());
            } else if(query.equals("SELECT id FROM effects ORDER BY id")) {
                owner=METHODS.get(1);int count=transaction++==0?0:1;equal(count,data.path("rows").size());
                if(count==1)equal(42L,data.path("rows").get(0).get(0).path("value").longValue());
            } else if(query.contains("AS value_text")) {
                owner=METHODS.get(2);equal("FAILED",data.path("outcome").asText());
                if(!data.path("failure").path("message").asText().contains("Aggregate SQL capture budget"))throw new AssertionError("Wrong aggregate failure");
            } else if(query.contains("AS raw_bytes")) {
                owner=METHODS.get(3);equal("INCOMPLETE",data.path("outcome").asText());
                equal("AQI=",data.path("rows").get(0).get(0).path("value").asText());
                equal("2026-11-01",data.path("rows").get(0).get(1).path("value").asText());
            } else if(query.equals("SELECT * FROM absent_table")) {
                owner=METHODS.get(4);equal("FAILED",data.path("outcome").asText());
                if(!data.path("failure").path("type").asText().endsWith("SQLSyntaxErrorException"))throw new AssertionError("Original JDBC type lost");
            } else if(query.contains("'abcdefgh'")) {
                owner=METHODS.get(4);equal("INCOMPLETE",data.path("outcome").asText());
                equal(true,data.path("moreRows").asBoolean());equal("abcd",data.path("rows").get(0).get(0).path("value").asText());
                equal(false,data.path("rows").get(0).get(0).path("complete").asBoolean());
            } else throw new AssertionError("Unexpected query snapshot");
            equal(owner,item.owner);observedOwners.add(owner);
        }
        int owners=selected.equals("suite")?5:selected.equals("failures")?3:1;equal(owners,observedOwners.size());
        var root=Pattern.compile("(?is)<li\\b[^>]*class=\"test-item\"[^>]*status=\"([^\"]+)\"").matcher(body).results().map(match->match.group(1)).toList();
        boolean extent=!root.isEmpty();
        if(!extent)root=Pattern.compile("(?is)<article\\b[^>]*class=\"scenario\"[^>]*>.*?<span class=\"status status-([^\"]+)\"").matcher(body).results().map(match->match.group(1)).toList();
        int rootCount=extent?(selected.equals("suite")?3:1):(selected.equals("suite")?5:selected.equals("failures")?3:1);
        if(root.size()!=rootCount || root.stream().anyMatch(value->!List.of("pass","fail").contains(value))
                || root.stream().filter("fail"::equals).count()!=(negative?1:0))throw new AssertionError("SQL root verdict differs from JUnit");
        if(!body.toUpperCase(Locale.ROOT).contains("<TH>ASSERTION</TH>") || !body.toUpperCase(Locale.ROOT).contains("<TH>EXPECTED</TH>"))throw new AssertionError("Standard assertion columns missing");
    }
    static void rejectMutants(String html,String selected,boolean negative) throws Exception {
        var pre=Pattern.compile("(?is)<script\\b[^>]*>.*?</script>|<style\\b[^>]*>.*?</style>|<pre\\b[^>]*>(.*?)</pre>").matcher(html);
        ObjectNode data=null;
        while(pre.find()) {
            if(pre.group(1)==null)continue;
            try{var candidate=JSON.readTree(unescape(pre.group(1)));if(candidate instanceof ObjectNode object && object.has("executionId") && object.has("query")){data=object;break;}}
            catch(java.io.IOException nonJson){ }
        }
        if(data==null)throw new AssertionError("No SQL body to mutate");
        data.put("executionId","");mustReject(replace(html,pre.start(1),pre.end(1),escape(JSON.writeValueAsString(data))),selected,negative);
        data=(ObjectNode)JSON.readTree(unescape(pre.group(1)));data.remove("query");
        mustReject(replace(html,pre.start(1),pre.end(1),escape(JSON.writeValueAsString(data))),selected,negative);
        if(selected.equals("results") || selected.equals("suite")) {
            String rounded=html.replace("9007199254740993.01","9007199254740992.00");mustReject(rounded,selected,negative);
        }
    }
    private static void mustReject(String html,String selected,boolean negative) throws Exception {
        try{require(html,selected,negative);}catch(AssertionError|java.io.IOException expected){return;}
        throw new AssertionError("SQL artifact mutant was accepted");
    }
    private static String replace(String value,int start,int end,String replacement){return value.substring(0,start)+replacement+value.substring(end);}
    private static void equal(Object expected,Object actual){if(!Objects.equals(expected,actual))throw new AssertionError(expected+" != "+actual);}
    private static void decimal(JsonNode value){if(value.decimalValue().compareTo(new BigDecimal("9007199254740993.01"))!=0)throw new AssertionError("SQL decimal changed");}
    private static String unescape(String value){return value.replace("&quot;","\"").replace("&#39;","'").replace("&lt;","<").replace("&gt;",">").replace("&amp;","&");}
    private static String escape(String value){return value.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");}
}
