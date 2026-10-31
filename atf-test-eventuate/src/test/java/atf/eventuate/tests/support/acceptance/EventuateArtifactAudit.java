package atf.eventuate.tests.support.acceptance;

import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;

/** Consumer-specific published evidence contract; scripts/styles cannot satisfy its oracle. */
final class EventuateArtifactAudit {
    private static final Pattern TABLE = Pattern.compile("(?is)<table\\b[^>]*class=[\"'][^\"']*\\b(?:helm-validation-table|assertions)\\b[^\"']*[\"'][^>]*>(.*?)</table>");
    private static final Pattern ROW = Pattern.compile("(?is)<tr\\b[^>]*>(.*?)</tr>");
    private static final Pattern CELL = Pattern.compile("(?is)<td\\b[^>]*>(.*?)</td>");
    private EventuateArtifactAudit() { }

    static void require(String html, int scenarios, int rows, int failures, List<String> names) {
        String body = html.replaceAll("(?is)<script\\b[^>]*>.*?</script>", "")
                .replaceAll("(?is)<style\\b[^>]*>.*?</style>", "");
        var tables = TABLE.matcher(body).results().toList();
        if (tables.size() != scenarios * 3) throw new AssertionError("Published assertion phases: " + tables.size());
        var observed = new ArrayList<String>();
        int failed = 0;
        for (var table : tables) {
            for (String heading : List.of("ASSERTION", "EXPECTED", "ACTUAL", "RESULT"))
                if (!table.group(1).contains("<th>" + heading + "</th>")) throw new AssertionError("Missing assertion column: " + heading);
            for (var row : ROW.matcher(table.group(1)).results().toList()) {
                var cells = CELL.matcher(row.group(1)).results().map(match -> match.group(1).replaceAll("<[^>]+>", "")).toList();
                if (cells.isEmpty()) continue;
                if (cells.size() != 4) throw new AssertionError("Assertion row does not contain four cells");
                observed.add(cells.getFirst());
                if (List.of("❌", "Fail").contains(cells.getLast())) failed++;
                else if (!List.of("✅", "Pass").contains(cells.getLast())) throw new AssertionError("Unknown assertion marker: " + cells.getLast());
            }
        }
        if (observed.size() != rows || failed != failures) throw new AssertionError("Published rows/failures: " + observed.size() + "/" + failed);
        for (String name : names) if (!observed.contains(name)) throw new AssertionError("Missing published comparison: " + name);
        if (!observed.contains("Reporting loaded from a binary artifact")
                || !observed.contains("No captured HTTP exchange executed a network endpoint"))
            throw new AssertionError("Setup/teardown evidence missing");
    }

    static void requireCounterexamples(String html, int scenarios, int rows, int failures, List<String> names) {
        rejected(html.replaceFirst("class=\"(?:helm-evidence-table helm-validation-table|assertions)\"", "class=\"removed-table\""), scenarios, rows, failures, names);
        rejected(html.replaceFirst("<td>(?:✅|<span class=\"status status-pass\">Pass</span>)</td>", "<td>❌</td>"), scenarios, rows, failures, names);
    }
    private static void rejected(String mutant, int scenarios, int rows, int failures, List<String> names) {
        try { require(mutant, scenarios, rows, failures, names); }
        catch (AssertionError expected) { return; }
        throw new AssertionError("Published evidence degradation escaped the consumer oracle");
    }
}
