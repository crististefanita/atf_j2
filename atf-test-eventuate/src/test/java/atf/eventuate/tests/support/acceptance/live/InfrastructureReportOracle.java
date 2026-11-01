package atf.eventuate.tests.support.acceptance.live;

import atf.eventuate.live.kafka.KafkaEvidence;
import atf.sql.result.SqlSnapshot;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;

/** Captured facts versus published bodies; a green test badge cannot hide truncated SQL/Kafka evidence. */
public final class InfrastructureReportOracle {
    private static final ObjectMapper JSON=new ObjectMapper().registerModule(new JavaTimeModule())
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .disable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS);
    private InfrastructureReportOracle() { }
    public static void require(String html,List<SqlSnapshot> sql,List<KafkaEvidence> kafka,boolean failed) {
        String body=html.replaceAll("(?is)<script\\b[^>]*>.*?</script>|<style\\b[^>]*>.*?</style>","");
        var values=new ArrayList<JsonNode>();
        var matches=Pattern.compile("(?is)<pre\\b[^>]*>(.*?)</pre>").matcher(body);
        while(matches.find()) {
            String text=decode(matches.group(1));
            try { values.add(JSON.readTree(text)); }
            catch(java.io.IOException malformed) {throw new AssertionError("Infrastructure body is incomplete or invalid JSON",malformed);}
        }
        if(values.size()!=sql.size()+kafka.size())throw new AssertionError("Infrastructure body population differs from captured facts");
        for(var snapshot:sql) requireOne(values,"executionId",snapshot.executionId(),wireValue(snapshot));
        for(var snapshot:kafka) requireOne(values,"groupId",snapshot.groupId(),wireValue(snapshot));
        var roots=Pattern.compile("(?is)<li\\b[^>]*class=\"test-item\"[^>]*status=\"([^\"]+)\"").matcher(body).results().map(m->m.group(1)).toList();
        if(roots.isEmpty())roots=Pattern.compile("(?is)<article\\b[^>]*class=\"scenario\"[^>]*>.*?<span class=\"status status-([^\"]+)\"")
                .matcher(body).results().map(m->m.group(1)).toList();
        if(!roots.equals(List.of(failed?"fail":"pass")))throw new AssertionError("Infrastructure HTML verdict differs from execution");
    }
    private static void requireOne(List<JsonNode> values,String key,String id,JsonNode expected) {
        var selected=values.stream().filter(value->value.path(key).asText().equals(id)).toList();
        if(selected.size()!=1 || !selected.getFirst().equals(expected))throw new AssertionError("Infrastructure capture changed in HTML: "+key+"="+id);
    }
    private static JsonNode wireValue(Object captured) {
        // JSON has one integer vocabulary: Java Long(0) and parsed IntNode(0) are the same fact.
        // Normalize through exact JSON tokens, never Double or a lossy numeric comparison.
        try { return JSON.readTree(JSON.writeValueAsString(captured)); }
        catch(java.io.IOException failure) {throw new AssertionError("Cannot project captured infrastructure facts",failure);}
    }
    private static String decode(String value) {
        return value.replace("&quot;","\"").replace("&#39;","'").replace("&lt;","<").replace("&gt;",">").replace("&amp;","&");
    }
}
