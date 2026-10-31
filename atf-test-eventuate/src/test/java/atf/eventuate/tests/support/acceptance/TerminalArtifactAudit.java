package atf.eventuate.tests.support.acceptance;

import java.util.List;
import java.util.regex.Pattern;

/** Consumer-specific checks over published markup, never scripts or style templates. */
final class TerminalArtifactAudit {
    private TerminalArtifactAudit() { }
    static void require(String html, String name, boolean suite, String outcome, int attempts, String phase, boolean expected) {
        String body = markup(html);
        for (String field : List.of("Terminal outcome", "Failure cause", "Original cause", "ACCEPTANCE_MOCK", "/orders/42", outcome))
            if (!body.toLowerCase(java.util.Locale.ROOT).contains(field.toLowerCase(java.util.Locale.ROOT)))
                throw new AssertionError("Missing terminal evidence: " + field);
        if (Pattern.compile("(?i)<td>Terminal outcome</td>").matcher(body).results().count() != 1)
            throw new AssertionError("Terminal snapshot missing or duplicated");
        int terminal = body.toLowerCase(java.util.Locale.ROOT).indexOf("<td>terminal outcome</td>");
        var phases = Pattern.compile("(?is)(?:<a[^>]*class=\"node[^\"]*\"[^>]*>\\s*<span>|<h3>)(SETUP|TEST BODY|TEARDOWN)<")
                .matcher(body.substring(0, terminal)).results().toList();
        if (phases.isEmpty() || !phases.getLast().group(1).equals(phase))
            throw new AssertionError("Terminal evidence belongs to the wrong phase");
        var roots = Pattern.compile("(?is)<li\\b[^>]*class=\"test-item\"[^>]*status=\"([^\"]+)\"").matcher(body).results()
                .map(match -> match.group(1)).toList();
        if (roots.isEmpty()) roots = Pattern.compile("(?is)<article\\b[^>]*class=\"scenario\"[^>]*>.*?<span class=\"status status-([^\"]+)\"")
                .matcher(body).results().map(match -> match.group(1)).toList();
        if (roots.size() != (suite ? 3 : 1) || !roots.getFirst().equals(expected ? "pass" : "fail")
                || roots.stream().skip(1).anyMatch(status -> !status.equals("pass"))) throw new AssertionError("JUnit/report verdict mismatch: " + roots);
        if (!body.contains("No HTTP response received for a terminal exchange")) throw new AssertionError("Absent response was fabricated or omitted");
        var tables = Pattern.compile("(?is)<table\\b[^>]*class=[\"'][^\"']*\\b(?:helm-validation-table|assertions)\\b[^\"']*[\"'][^>]*>(.*?)</table>")
                .matcher(body).results().toList();
        boolean teardown = phase.equals("TEARDOWN");
        int expectedTables = (teardown || expected ? 3 : 2) + (suite ? 6 : 0);
        if (tables.size() != expectedTables)
            throw new AssertionError("Lost setup/teardown or successful peer assertions: " + tables.size());
        var steps = assertionSteps(body);
        if (steps.size() != expectedTables || steps.stream().anyMatch(step -> !step.group(1).equals("pass")))
            throw new AssertionError("Execution failure was incorrectly assigned to passing assertions");
        int rows = 0;
        for (var table : tables) {
            for (String heading : List.of("ASSERTION", "EXPECTED", "ACTUAL", "RESULT"))
                if (!table.group(1).contains("<th>" + heading + "</th>")) throw new AssertionError("Missing column " + heading);
            rows += Pattern.compile("(?is)<tr\\b[^>]*>(?:(?!</tr>).)*<td>").matcher(table.group(1)).results().count();
        }
        int expectedRows = (expected ? 5 : teardown ? 9 : 2) + (suite ? 28 : 0);
        if (rows != expectedRows) throw new AssertionError("Unexpected published assertions: " + rows);
        if (!expected && !body.contains("Fail") && !body.contains("❌")) throw new AssertionError("Failure was published as PASS");
        if (name.equals("retry-transport") && (!body.contains("503") || !body.contains("RETRYABLE_RESPONSE")))
            throw new AssertionError("Retry history missing");
        if (attempts < 1 || !body.contains("Execution evidence")) throw new AssertionError("Execution history missing");
    }
    static void rejectMutants(String html, String name, boolean suite, String outcome, int attempts, String phase, boolean expected) {
        String body = markup(html);
        reject(body.replaceAll("(?i)<td>Terminal outcome</td>", "<td>Removed</td>"), name, suite, outcome, attempts, phase, expected);
        reject(body.replaceFirst("<th>ACTUAL</th>", "<th>REMOVED</th>"), name, suite, outcome, attempts, phase, expected);
        reject(body.replace(">" + phase + "<", ">WRONG PHASE<"), name, suite, outcome, attempts, phase, expected);
        var step = assertionSteps(body).getFirst();
        reject(body.substring(0, step.start(1)) + "fail" + body.substring(step.end(1)), name, suite, outcome, attempts, phase, expected);
    }
    private static String markup(String html) {
        return html.replaceAll("(?is)<script\\b[^>]*>.*?</script>|<style\\b[^>]*>.*?</style>", "");
    }
    private static List<java.util.regex.MatchResult> assertionSteps(String body) {
        var extent = Pattern.compile("(?is)<div class=\"card-header\"[^>]*>(?:(?!<div class=\"card-header\").)*?<span class=\"badge (pass|fail)-bg log [^\"]*\">(?:(?!<div class=\"card-header\").)*?Component assertions")
                .matcher(body).results().toList();
        return extent.isEmpty() ? Pattern.compile("<summary>Component assertions<span class=\"status status-([^\"]+)\"")
                .matcher(body).results().toList() : extent;
    }
    private static void reject(String html, String name, boolean suite, String outcome, int attempts, String phase, boolean expected) {
        try { require(html, name, suite, outcome, attempts, phase, expected); }
        catch (AssertionError rejected) { return; }
        throw new AssertionError("Corrupted terminal artifact escaped its oracle");
    }
}
