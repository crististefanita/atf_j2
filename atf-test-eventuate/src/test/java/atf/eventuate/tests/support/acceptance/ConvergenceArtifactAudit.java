package atf.eventuate.tests.support.acceptance;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.regex.Pattern;

/** Independent oracle over published facts; no fixture-side actual becomes an expected value. */
final class ConvergenceArtifactAudit {
    private static final ObjectMapper JSON = new ObjectMapper();
    private ConvergenceArtifactAudit() { }
    static void require(String html, String name, boolean suite, boolean expected) throws Exception {
        if (html.contains("CONVERGENCE_SECRET_108")) throw new AssertionError("Raw secret leaked from captured cause");
        String body = markup(html);
        var marker = Pattern.compile("(?i)<td>Convergence outcome</td>\\s*<td>([^<]+)</td>").matcher(body).results().toList();
        if (marker.size() != 1) throw new AssertionError("Convergence snapshot missing or duplicated");
        String outcome = name.equals("expired") ? "DEADLINE_REACHED" : name.equals("pause") ? "PAUSE_FAILURE" : "READ_FAILURE";
        if (!marker.getFirst().group(1).equals(outcome)) throw new AssertionError("Wrong terminal outcome");
        String facts = body.substring(marker.getFirst().start());
        pair(facts, "Expected order ID", "42"); pair(facts, "Expected order state", "APPROVED");
        pair(facts, "Convergence budget", "PT0.025S"); pair(facts, "Poll interval", "PT0.01S");
        int observations = List.of("empty", "expired").contains(name) ? 0 : 1;
        pair(facts, "Observation count", Integer.toString(observations));
        pair(facts, "Convergence elapsed", name.equals("expired") ? "PT0.025S" : name.equals("pause") ? "PT0.002S"
                : name.equals("empty") ? "PT0S" : "PT0.01S");
        if (!name.equals("expired")) {
            pairPresent(facts, "Failure type"); pairPresent(facts, "Failure message"); pairPresent(facts, "Original cause");
            if (!facts.contains(name.equals("pause") ? "controlled interruption" : "controlled convergence transport cause"))
                throw new AssertionError("Original cause missing from convergence snapshot");
        }
        var history = Pattern.compile("(?is)<pre\\b[^>]*>(.*?)</pre>").matcher(facts);
        if (!history.find()) throw new AssertionError("Missing convergence history body");
        var rows = JSON.readTree(unescape(history.group(1)));
        if (!rows.isArray() || rows.size() != observations) throw new AssertionError("Unexpected observation population");
        if (observations == 1) {
            var row = rows.get(0);
            if (row.path("number").asInt() != 1 || row.path("elapsedNanos").asLong() != 0 || row.path("orderId").asLong() != 42
                    || !row.path("orderState").asText().equals("PENDING") || !row.path("rejectionReason").isNull())
                throw new AssertionError("Captured observation was replaced or reevaluated");
        }
        var phases = Pattern.compile("(?is)(?:<a[^>]*class=\"node[^\"]*\"[^>]*>\\s*<span>|<h3>)(SETUP|TEST BODY|TEARDOWN)<")
                .matcher(body.substring(0, marker.getFirst().start())).results().toList();
        if (phases.isEmpty() || !phases.getLast().group(1).equals("TEST BODY")) throw new AssertionError("Wrong evidence phase");
        var roots = Pattern.compile("(?is)<li\\b[^>]*class=\"test-item\"[^>]*status=\"([^\"]+)\"").matcher(body).results()
                .map(match -> match.group(1)).toList();
        if (roots.isEmpty()) roots = Pattern.compile("(?is)<article\\b[^>]*class=\"scenario\"[^>]*>.*?<span class=\"status status-([^\"]+)\"")
                .matcher(body).results().map(match -> match.group(1)).toList();
        if (roots.size() != (suite ? 3 : 1) || roots.stream().filter("fail"::equals).count() != (expected ? 0 : 1))
            throw new AssertionError("Root verdict does not match JUnit");
        if (!expected && !roots.get(suite ? 1 : 0).equals("fail")) throw new AssertionError("Failure assigned to a successful peer");
        var tables = Pattern.compile("(?is)<table\\b[^>]*class=[\"'][^\"']*\\b(?:helm-validation-table|assertions)\\b[^\"']*[\"'][^>]*>(.*?)</table>")
                .matcher(body).results().toList();
        if (tables.size() != (expected ? 3 : 2) + (suite ? 6 : 0)) throw new AssertionError("Assertion tables lost or observations became assertions");
        for (var table : tables) for (String header : List.of("ASSERTION", "EXPECTED", "ACTUAL", "RESULT"))
            if (!table.group(1).contains("<th>" + header + "</th>")) throw new AssertionError("Missing comparison column " + header);
    }
    static void rejectMutants(String html, String name, boolean suite, boolean expected) throws Exception {
        String body = markup(html);
        reject(body.replaceAll("(?i)<td>Convergence outcome</td>", "<td>Removed</td>"), name, suite, expected);
        reject(body.replaceFirst("<th>ACTUAL</th>", "<th>REMOVED</th>"), name, suite, expected);
        reject(body.replace(">TEST BODY<", ">WRONG PHASE<"), name, suite, expected);
        reject(body.replaceAll("(?i)<td>Expected order ID</td>", "<td>REMOVED ID</td>"), name, suite, expected);
        if (!List.of("empty", "expired").contains(name)) reject(body.replace("PENDING", "APPROVED"), name, suite, expected);
    }
    private static void pair(String body, String field, String expected) {
        var value = Pattern.compile("(?i)<td>" + Pattern.quote(field) + "</td>\\s*<td>([^<]*)</td>").matcher(body);
        if (!value.find() || !value.group(1).equals(expected)) throw new AssertionError("Wrong captured field: " + field);
    }
    private static void pairPresent(String body, String field) {
        if (!Pattern.compile("(?i)<td>" + Pattern.quote(field) + "</td>").matcher(body).find())
            throw new AssertionError("Missing captured field: " + field);
    }
    private static String markup(String html) { return html.replaceAll("(?is)<script\\b[^>]*>.*?</script>|<style\\b[^>]*>.*?</style>", ""); }
    private static String unescape(String value) {
        return value.replace("&quot;", "\"").replace("&#39;", "'").replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&");
    }
    private static void reject(String html, String name, boolean suite, boolean expected) throws Exception {
        try { require(html, name, suite, expected); }
        catch (AssertionError | com.fasterxml.jackson.core.JsonProcessingException rejected) { return; }
        throw new AssertionError("Corrupted convergence artifact escaped its oracle");
    }
}
