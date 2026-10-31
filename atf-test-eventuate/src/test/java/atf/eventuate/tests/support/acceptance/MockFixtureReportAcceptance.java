package atf.eventuate.tests.support.acceptance;

import atf.core.http.request.ApiRequest;
import atf.core.http.response.ApiResponse;
import atf.core.http.response.JsonBodyPolicy;
import atf.mock.AtfMockConfiguration;
import atf.mock.AtfMockConfigurationLoader;
import atf.mock.AtfMockTransport;
import atf.mock.MockRoute;
import atf.reporting.artifact.ReportArtifactInspector;
import atf.reporting.audit.AssertionAuditReport;
import atf.reporting.configuration.ReportingSettings;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.html.HtmlReportSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpRequest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/** Consumer proof of Mock's public guard and transport on the shipped Reporting binary. */
public final class MockFixtureReportAcceptance {
    @FunctionalInterface private interface Factory { MockRoute create() throws Exception; }
    private record Probe(String name, Factory factory, String body, String rejection, int status, boolean controlledFailure) { }
    private record Outcome(Probe probe, List<AtfAssertion> rows) {
        AtfReportStatus status() { return rows.stream().allMatch(AtfAssertion::passed) ? AtfReportStatus.PASS : AtfReportStatus.FAIL; }
    }
    private static final Pattern TABLE = Pattern.compile("(?is)<table\\b[^>]*class=[\"'][^\"']*\\b(?:helm-validation-table|assertions)\\b[^\"']*[\"'][^>]*>(.*?)</table>");
    private MockFixtureReportAcceptance() { }

    public static void main(String[] args) throws Exception {
        Path output = Path.of(args[0]).toAbsolutePath().normalize();
        if (Files.exists(output)) throw new IllegalArgumentException("Fresh report directory required");
        Files.createDirectories(output);
        var observations = new ArrayList<Outcome>();
        for (var probe : probes(output)) observations.add(observe(probe));
        var receipts = new ArrayList<Map<String, Object>>();
        for (String renderer : List.of("extent", "fallback")) {
            for (var observation : observations)
                publish(output.resolve(renderer + "-" + observation.probe().name() + ".html"), renderer, List.of(observation), receipts);
            publish(output.resolve(renderer + "-combined.html"), renderer, observations, receipts);
        }
        var mapper = new ObjectMapper();
        mapper.writerWithDefaultPrettyPrinter().writeValue(output.resolve("acceptance.json").toFile(), receipts);
        mapper.writerWithDefaultPrettyPrinter().writeValue(output.resolve("core-policy-observation.json").toFile(),
                Map.of("binary", "Core C1", "completeDocumentExpected", false,
                        "trailingActual", JsonBodyPolicy.validDocument("{} trailing"),
                        "secondDocumentActual", JsonBodyPolicy.validDocument("{} {}"),
                        "duplicateKeysActual", JsonBodyPolicy.validDocument("{\"x\":1,\"x\":2}"),
                        "writeExchangeViolations", JsonBodyPolicy.validateWriteExchange("{} trailing", "{}")));
        System.out.println("MOCK_FIXTURE_ACCEPTANCE reports=" + receipts.size() + "; per-row and mutation oracles=PASS");
    }

    private static List<Probe> probes(Path output) throws Exception {
        Path yaml = output.resolve("invalid-constraint.yaml");
        Files.writeString(yaml, "mock:\n  routes:\n    - method: POST\n      path: /fixture\n      status: 201\n"
                + "      body: '{}'\n      request-body: {id: 1}\n");
        return List.of(
                new Probe("expected-rejected", () -> route("{}", "{} trailing"), "{}", "IllegalArgumentException", 0, false),
                new Probe("response-rejected", () -> route("{} trailing", null), "{}", "IllegalArgumentException", 0, false),
                new Probe("yaml-rejected", () -> AtfMockConfigurationLoader.load(yaml).routes().getFirst(), "{}", "IllegalStateException", 0, false),
                new Probe("nominal", () -> route("{\"accepted\":true}", "{\"id\":1}"), " { \"id\" : 1 } ", "none", 201, false),
                new Probe("malformed-request", () -> route("{}", null), "{} trailing", "none", 499, false),
                new Probe("controlled-mismatch", () -> route("{}", "{\"id\":1}"), "{\"id\":2}", "none", 201, true));
    }
    private static MockRoute route(String body, String expected) {
        return new MockRoute("POST", "/fixture", 201, body, Map.of(), Duration.ZERO,
                "*", Map.of(), Map.of(), "json", expected);
    }
    private static Outcome observe(Probe probe) throws Exception {
        var dispatches = new AtomicInteger();
        ApiResponse response = null;
        String rejection = "none";
        try {
            var route = probe.factory().create();
            var fallback = new MockRoute("*", "/*", 499, "fallback", Map.of(), Duration.ZERO);
            var transport = new AtfMockTransport(new AtfMockConfiguration(List.of(route), fallback));
            var request = new ApiRequest(HttpRequest.BodyPublishers.ofString(probe.body()), "POST", "/fixture",
                    Map.of(), Map.of("Content-Type", "application/json"), "none", null, probe.body());
            dispatches.incrementAndGet();
            response = transport.send(request, "/fixture", "none").join();
        } catch (IllegalArgumentException | IllegalStateException expected) {
            rejection = expected.getClass().getSimpleName();
        }
        var rows = new ArrayList<AtfAssertion>();
        rows.add(check(probe, "Fixture rejection", probe.rejection(), rejection));
        if (!probe.rejection().equals("none")) {
            rows.add(check(probe, "Dispatch count", "0", Integer.toString(dispatches.get())));
            rows.add(check(probe, "Response available", "false", Boolean.toString(response != null)));
        } else {
            rows.add(check(probe, "HTTP outcome", Integer.toString(probe.status()), response == null ? "unavailable" : Integer.toString(response.status())));
            rows.add(check(probe, "Dispatch count", "1", Integer.toString(dispatches.get())));
        }
        var outcome = new Outcome(probe, List.copyOf(rows));
        long failures = rows.stream().filter(row -> !row.passed()).count();
        if (failures != (probe.controlledFailure() ? 1 : 0)) throw new AssertionError("Mock contract failed: " + probe.name());
        return outcome;
    }
    private static AtfAssertion check(Probe probe, String title, String expected, String actual) {
        return new AtfAssertion(probe.name() + " · " + title, expected, actual, expected.equals(actual));
    }
    private static void publish(Path path, String renderer, List<Outcome> observations, List<Map<String, Object>> receipts) throws Exception {
        var settings = ReportingSettings.defaults();
        try (AtfReportPorts.Session session = renderer.equals("extent") ? new ExtentReportSession(path, settings) : new HtmlReportSession(path, settings)) {
            for (var observation : observations) try (var scenario = session.startScenario("MockJsonFixtures", "atf.eventuate.acceptance",
                    observation.probe().name(), List.of("component", "mock")); var phase = scenario.startPhase("TEST BODY")) {
                phase.detail("Evidence source", "MOCK; in-memory transport; no network");
                AssertionAuditReport.publishStep(phase, "Component assertions", observation.rows(), AtfReportStatus.INFO);
                phase.complete(observation.status()); scenario.complete(observation.status());
            }
        }
        String html = Files.readString(path).replaceAll("(?is)<script\\b[^>]*>.*?</script>|<style\\b[^>]*>.*?</style>", "");
        audit(html, observations);
        reject(html.replaceFirst("<th>ACTUAL</th>", "<th>LOST</th>"), observations);
        reject(html.replaceFirst("<td>(?:✅|❌|<span class=\"status [^\"]+\">[^<]+</span>)</td>", "<td>UNKNOWN</td>"), observations);
        var inspection = ReportArtifactInspector.inspect(path);
        receipts.add(Map.of("report", path.getFileName().toString(), "renderer", renderer, "scenarios", observations.size(),
                "rows", observations.size() * 3, "controlledFailures", observations.stream().filter(o -> o.probe().controlledFailure()).count(),
                "consumerOracle", "PASS", "genericInspectorCompliant", inspection.isCompliant(), "genericInspectorChecks", inspection.checks()));
    }
    private static void audit(String html, List<Outcome> observations) {
        var tables = TABLE.matcher(html).results().toList();
        if (tables.size() != observations.size()) throw new AssertionError("Table ownership/cardinality lost");
        for (int index = 0; index < tables.size(); index++) {
            String table = tables.get(index).group(1);
            for (String heading : List.of("ASSERTION", "EXPECTED", "ACTUAL", "RESULT"))
                if (!table.contains("<th>" + heading + "</th>")) throw new AssertionError("Missing column " + heading);
            var rows = Pattern.compile("(?is)<tr\\b[^>]*>(.*?)</tr>").matcher(table).results().filter(r -> r.group(1).contains("<td")).toList();
            if (rows.size() != 3) throw new AssertionError("Comparison lost");
            for (int ordinal = 0; ordinal < 3; ordinal++) {
                var cells = Pattern.compile("(?is)<td\\b[^>]*>(.*?)</td>").matcher(rows.get(ordinal).group(1)).results().toList();
                var expected = observations.get(index).rows().get(ordinal);
                if (cells.size() != 4 || !plain(cells.get(0).group(1)).equals(expected.name())
                        || !plain(cells.get(1).group(1)).equals(expected.expected()) || !plain(cells.get(2).group(1)).equals(expected.actual()))
                    throw new AssertionError("Comparison identity/value changed");
                String verdict = plain(cells.get(3).group(1));
                if (!(expected.passed() ? List.of("✅", "Pass") : List.of("❌", "Fail")).contains(verdict))
                    throw new AssertionError("Per-row verdict changed");
            }
        }
    }
    private static String plain(String html) {
        return html.replaceAll("<[^>]+>", "").replace("&quot;", "\"").replace("&#39;", "'")
                .replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&");
    }
    private static void reject(String html, List<Outcome> observations) {
        try { audit(html, observations); } catch (AssertionError expected) { return; }
        throw new AssertionError("Degraded evidence escaped the oracle");
    }
}
