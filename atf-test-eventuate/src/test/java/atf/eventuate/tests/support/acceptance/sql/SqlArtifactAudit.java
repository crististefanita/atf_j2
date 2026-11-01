package atf.eventuate.tests.support.acceptance.sql;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.nio.file.Files;
import java.nio.file.Path;

/** Oracle over published SQL facts with expected rows fixed independently of JDBC results. */
final class SqlArtifactAudit {
    private static final ObjectMapper JSON = new ObjectMapper();
    private record Expected(long order, boolean complete, List<String> rows) { }
    private SqlArtifactAudit() { }
    public static void main(String[] args) throws Exception {
        Path root = Path.of(args[0]).toAbsolutePath().normalize(); int count = 0;
        for (var entry : JSON.readTree(root.resolve("acceptance.json").toFile())) {
            Path report = root.resolve(entry.path("report").asText()).normalize();
            if (!report.startsWith(root)) throw new AssertionError("Report outside acceptance directory");
            String html = Files.readString(report), selected = entry.path("selection").asText(), probe = entry.path("probe").asText();
            require(html, selected, probe); rejectMutants(html, selected, probe); count++;
        }
        System.out.println("SQL_ARTIFACT_AUDIT reports=" + count + "; bounded observations, per-row verdict and mutants=PASS");
    }
    static void require(String html, String selected, String probe) throws Exception {
        if (html.contains("SQL_WITNESS_SECRET") || html.contains("fixture-only")) throw new AssertionError("SQL secret leaked");
        String body = markup(html);
        boolean suite = selected.equals("suite") || selected.equals("combined");
        int roots = selected.equals("combined") ? 6 : suite ? 3 : 1;
        var outcomes = Pattern.compile("(?is)<li\\b[^>]*class=\"test-item\"[^>]*status=\"([^\"]+)\"").matcher(body).results().map(m -> m.group(1)).toList();
        if (outcomes.isEmpty()) outcomes = Pattern.compile("(?is)<article\\b[^>]*class=\"scenario\"[^>]*>.*?<span class=\"status status-([^\"]+)\"")
                .matcher(body).results().map(m -> m.group(1)).toList();
        if (outcomes.size() != roots || outcomes.stream().filter("fail"::equals).count() != (probe.equals("none") ? 0 : 1))
            throw new AssertionError("SQL root verdict/population differs from JUnit");
        var expected = new ArrayList<Expected>();
        if (suite || selected.equals("transactions")) expected.addAll(transactions());
        if (suite || selected.equals("identity")) expected.addAll(identity());
        if (suite || selected.equals("failures")) { expected.addAll(failures()); if (probe.equals("sql-error")) expected.add(e(42, false)); }
        // Bound every observation before parsing fields: a removed field must fail,
        // never borrow a later snapshot's value or trigger cross-table backtracking.
        var snapshots = Pattern.compile("(?i)<td>SQL provenance</td>").matcher(body).results().toList();
        if (snapshots.size() != expected.size()) throw new AssertionError("SQL snapshots lost/duplicated: " + snapshots.size() + " != " + expected.size());
        for (int i = 0; i < expected.size(); i++) {
            var actual = snapshots.get(i); var fixed = expected.get(i);
            String block = body.substring(actual.start(), i + 1 < snapshots.size() ? snapshots.get(i + 1).start() : body.length());
            if (!field(block,"SQL provenance").equals("SQL_EMBEDDED") || !field(block,"Witness ID").matches("atf_j2_[a-f0-9]{32}")
                    || !field(block,"Requested order ID").equals(Long.toString(fixed.order)) || !field(block,"SQL outcome").equals(fixed.complete ? "COMPLETE" : "FAILED")
                    || !field(block,"SQL row count").equals(Integer.toString(fixed.rows.size())) || Duration.parse(field(block,"SQL elapsed")).isNegative())
                throw new AssertionError("Incorrect SQL snapshot metadata at " + i);
            var phases = Pattern.compile("(?is)(?:<a[^>]*class=\"node[^\"]*\"[^>]*>\\s*<span>|<h3>)(SETUP|TEST BODY|TEARDOWN)<")
                    .matcher(body.substring(0,actual.start())).results().toList();
            if (phases.isEmpty() || !phases.getLast().group(1).equals("TEST BODY")) throw new AssertionError("SQL snapshot in wrong phase");
            var history = Pattern.compile("(?is)<pre\\b[^>]*>(.*?)</pre>").matcher(block);
            if (!history.find()) throw new AssertionError("SQL row body missing");
            var rows = JSON.readTree(unescape(history.group(1)));
            if (!rows.isArray() || rows.size() != fixed.rows.size()) throw new AssertionError("SQL rows lost/duplicated at " + i);
            for (int j = 0; j < rows.size(); j++) {
                var row = rows.get(j);
                String value = row.path("reservationId").asText() + ":" + row.path("orderId").asText() + ":"
                        + row.path("amount").asText() + ":" + row.path("state").asText();
                if (!value.equals(fixed.rows.get(j))) throw new AssertionError("SQL row differs from fixed expected: " + value);
            }
            if (!fixed.complete) for (String name : List.of("SQL failure type", "SQL failure message", "SQL state", "SQL vendor code", "Original SQL cause", "SQL cleanup failures"))
                if (field(block,name).isBlank()) throw new AssertionError("Empty SQL failure field: " + name);
        }
        if ((suite || selected.equals("failures")) && (!body.contains("Original SQL cause".toUpperCase()) && !body.contains("Original SQL cause")))
            throw new AssertionError("SQL original cause field missing");
        var tables = Pattern.compile("(?is)<table\\b[^>]*class=[\"'][^\"']*\\b(?:helm-validation-table|assertions)\\b[^\"']*[\"'][^>]*>(.*?)</table>")
                .matcher(body).results().toList();
        if (tables.size() != roots * 3) throw new AssertionError("SQL phase assertion tables missing");
        for (var table : tables) for (String header : List.of("ASSERTION", "EXPECTED", "ACTUAL", "RESULT"))
            if (!table.group(1).contains("<th>" + header + "</th>")) throw new AssertionError("Missing SQL comparison column " + header);
        String failResult = "(?:❌|<span class=\"status status-fail\">Fail</span>)";
        long failedRows = tables.stream().mapToLong(table -> Pattern.compile("<td>" + failResult + "</td>").matcher(table.group(1)).results().count()).sum();
        if (failedRows != (probe.equals("duplicate") ? 1 : 0)) throw new AssertionError("SQL assertion verdict moved or corrupted");
        if (probe.equals("duplicate") && !Pattern.compile("<td>Controlled duplicate effect mismatch</td><td>1</td><td>2</td><td>" + failResult + "</td>").matcher(body).find())
            throw new AssertionError("Duplicate failure not assigned to its exact comparison");
        if (!selected.equals("combined") && body.contains("Observed mock exchange")) throw new AssertionError("SQL fabricated an HTTP exchange");
    }
    static void rejectMutants(String html, String selected, String probe) throws Exception {
        String body = markup(html);
        reject(body.replaceAll("(?i)<td>SQL provenance</td>", "<td>REMOVED</td>"), selected, probe);
        reject(body.replaceFirst("<th>ACTUAL</th>", "<th>REMOVED</th>"), selected, probe);
        reject(body.replaceFirst("SQL_EMBEDDED", "LIVE"), selected, probe);
        reject(body.replaceAll("(?i)<td>Requested order ID</td>", "<td>REMOVED</td>"), selected, probe);
        reject(body.replace(">TEST BODY<", ">WRONG PHASE<"), selected, probe);
        var comparison = Pattern.compile("(?is)<table\\b[^>]*class=[\"'][^\"']*\\b(?:helm-validation-table|assertions)\\b[^\"']*[\"'][^>]*>.*?</table>").matcher(body);
        if (!comparison.find()) throw new AssertionError("No comparison table to mutate");
        String table = comparison.group(), corrupted = table.replaceFirst("✅|<span class=\"status status-pass\">Pass</span>", "❌");
        if (table.equals(corrupted)) throw new AssertionError("Comparison verdict mutant was not applied");
        reject(body.substring(0,comparison.start()) + corrupted + body.substring(comparison.end()), selected, probe);
        if (selected.equals("identity") || selected.equals("suite") || selected.equals("combined"))
            reject(body.replace("9007199254740993.01", "9007199254740992.01"), selected, probe);
        if (selected.equals("failures") || selected.equals("suite") || selected.equals("combined"))
            reject(body.replaceAll("(?i)<td>SQL outcome</td>\\s*<td>FAILED</td>", "<td>SQL outcome</td><td>COMPLETE</td>"), selected, probe);
    }
    private static Expected e(long order, boolean complete, String... rows) { return new Expected(order, complete, List.of(rows)); }
    private static List<Expected> transactions() { return List.of(e(42,true),e(42,true),e(42,true,"1:42:70.00:APPROVED"),e(42,true,"1:42:70.00:APPROVED"),e(42,true,"1:42:70.00:APPROVED")); }
    private static List<Expected> identity() {
        return List.of(e(42,true),e(43,true,"1:43:70.00:APPROVED"),e(42,true,"2:42:70.00:APPROVED"),e(42,true,"2:42:70.00:APPROVED","3:42:70.00:APPROVED"),
                e(44,true,"4:44:70.01:APPROVED"),e(45,true,"5:45:70.00:PENDING"),e(46,true,"6:46:9007199254740993.01:APPROVED"),e(42,true));
    }
    private static List<Expected> failures() { return List.of(e(42,false),e(42,false),e(42,false,"1:42:70.00:APPROVED"),e(42,false,"1:42:70.00:APPROVED","2:42:70.00:APPROVED"),e(42,true,"1:42:70.00:APPROVED","2:42:70.00:APPROVED")); }
    private static String markup(String html) { return html.replaceAll("(?is)<script\\b[^>]*>.*?</script>|<style\\b[^>]*>.*?</style>", ""); }
    private static String field(String block, String name) {
        var value = Pattern.compile("(?i)<td>" + Pattern.quote(name) + "</td>\\s*<td>([^<]*)</td>").matcher(block);
        if (!value.find()) throw new AssertionError("SQL field missing: " + name);
        return value.group(1);
    }
    private static String unescape(String value) { return value.replace("&quot;","\"").replace("&#39;", "'").replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&"); }
    private static void reject(String html, String selected, String probe) throws Exception {
        try { require(html, selected, probe); } catch (AssertionError | com.fasterxml.jackson.core.JsonProcessingException refused) { return; }
        throw new AssertionError("Corrupted SQL artifact escaped its oracle");
    }
}
