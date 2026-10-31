package atf.eventuate.tests.support.acceptance;

import atf.core.http.response.ApiResponse;
import atf.core.reporting.inventory.ValidationSink;
import atf.reporting.configuration.ReportingSettings;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.html.HtmlReportSession;
import atf.validation.contract.ResponseContract;
import atf.validation.contract.ContractValidators;
import atf.validation.contract.SchemaResourceResolver;
import atf.validation.publishing.ResponseContractReporter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/** Consumer probe of Validation's public neutral port on the shipped Reporting binary. */
public final class ValidationReportAcceptance {
    private record Probe(String name, String body, Supplier<ResponseContract> contract, boolean warning,
                         AtfReportStatus status, int rows, int failures, List<String> actuals, Map<String, String> headers) {
        private Probe(String name, String body, Supplier<ResponseContract> contract, boolean warning,
                      AtfReportStatus status, int rows, int failures, List<String> actuals) {
            this(name, body, contract, warning, status, rows, failures, actuals, Map.of());
        }
        private Probe(String name, String body, Supplier<ResponseContract> contract, boolean warning,
                      AtfReportStatus status, int rows, int failures) {
            this(name, body, contract, warning, status, rows, failures, List.of());
        }
    }
    private ValidationReportAcceptance() { }
    public static void main(String[] args) throws Exception {
        Path output = Path.of(args[0]).toAbsolutePath().normalize();
        if (Files.exists(output)) throw new IllegalArgumentException("Fresh output required");
        Files.createDirectories(output);
        var receipts = new ArrayList<Map<String, Object>>();
        for (String renderer : List.of("extent", "fallback")) for (var probe : probes(output)) {
            Path report = output.resolve(renderer + "-" + probe.name() + ".html");
            var settings = ReportingSettings.defaults();
            try (AtfReportPorts.Session session = renderer.equals("extent") ? new ExtentReportSession(report, settings)
                    : new HtmlReportSession(report, settings)) {
                try (var scenario = session.startScenario("ValidationConsumer", "atf.eventuate.acceptance", probe.name(), List.of("component", "mock"));
                     var phase = scenario.startPhase("TEST BODY");
                     var request = phase.startRequest("Synthetic contract fixture", "GET", "/contract-fixture")) {
                    request.detail("Evidence source", "SYNTHETIC; no network dispatch");
                    request.json("Response fixture", probe.body());
                    var decision = new ResponseContractReporter().validate(sink(request), "Response contract", new ApiResponse(
                            200, probe.body(), probe.headers(), Duration.ZERO, ""), probe.contract().get(), probe.warning(), false);
                    if (!decision.status().name().equals(probe.status().name()) || decision.rows().size() != probe.rows()
                            || decision.rows().stream().filter(row -> !row.passed()).count() != probe.failures())
                        throw new AssertionError("Unexpected Validation verdict: " + probe.name());
                    if (decision.rows().stream().anyMatch(row -> row.actual().length() > 240 || row.expected().length() > 240))
                        throw new AssertionError("Preview limit violated");
                    request.complete(probe.status()); phase.complete(probe.status()); scenario.complete(probe.status());
                }
            }
            String html = Files.readString(report).replaceAll("(?is)<script\\b[^>]*>.*?</script>|<style\\b[^>]*>.*?</style>", "");
            requireArtifact(html, probe);
            rejectMutation(html.replaceFirst("<th>ACTUAL</th>", "<th>REMOVED</th>"), probe);
            rejectMutation(html.replaceFirst("<td>(?:✅|❌|⚠️|<span class=\"status [^\"]+\">[^<]+</span>)</td>", "<td>UNKNOWN</td>"), probe);
            if (!probe.actuals().isEmpty())
                rejectMutation(html.replaceFirst("(?is)(<table\\b[^>]*class=[\"'][^\"']*\\b(?:helm-validation-table|assertions)\\b[^\"']*[\"'][^>]*>"
                                + ".*?<tr[^>]*>\\s*<td[^>]*>.*?</td>\\s*<td[^>]*>.*?</td>\\s*<td[^>]*>).*?(</td>)",
                        "$1LOST-ACTUAL$2"), probe);
            receipts.add(Map.of("report", report.getFileName().toString(), "renderer", renderer, "case", probe.name(),
                    "status", probe.status().name(), "rows", probe.rows(), "negativeRows", probe.failures()));
        }
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(output.resolve("acceptance.json").toFile(), receipts);
        System.out.println("VALIDATION_REPORT_ACCEPTANCE reports=" + receipts.size() + "; artifact and mutation oracles=PASS");
    }
    private static List<Probe> probes(Path output) throws Exception {
        Path additional = schema(output, "additional", "{\"additionalProperties\":false}");
        Path items = schema(output, "items", "{\"items\":false}");
        Path unknown = schema(output, "unknown", "{\"type\":\"unknown\"}");
        String prefix = "audit-value-".repeat(25);
        var cases = new ArrayList<>(List.of(
                new Probe("nominal", "{\"id\":42}", () -> new ResponseContract().jsonPathEquals("$.id", "42").jsonPathType("$.id", "integer"), false, AtfReportStatus.PASS, 2, 0),
                new Probe("invalid-json", "{} trailing", ValidationReportAcceptance::presence, false, AtfReportStatus.FAIL, 3, 3),
                new Probe("missing-null", "{}", ValidationReportAcceptance::presence, false, AtfReportStatus.FAIL, 3, 2),
                new Probe("explicit-null", "{\"x\":null}", () -> new ResponseContract().jsonPathRequired("$.x").jsonPathType("$.x", "null"), false, AtfReportStatus.PASS, 2, 0),
                new Probe("schema-additional", "{\"extra\":1}", () -> schemaContract(additional), false, AtfReportStatus.FAIL, 1, 1),
                new Probe("schema-items", "[1]", () -> schemaContract(items), false, AtfReportStatus.FAIL, 1, 1),
                new Probe("schema-invalid", "{}", () -> schemaContract(unknown), false, AtfReportStatus.FAIL, 1, 1),
                new Probe("warning-json", "not-json", ValidationReportAcceptance::presence, true, AtfReportStatus.WARNING, 3, 3),
                new Probe("preview-240", "{\"x\":\"" + prefix + "B\"}", () -> new ResponseContract().jsonPathEquals("$.x", prefix + "A"), false, AtfReportStatus.FAIL, 1, 1),
                new Probe("scalar-nominal", "{\"empty\":\"\",\"number\":42,\"bool\":true,\"nil\":null,\"items\":[{\"x\":\"\"}]}",
                        () -> new ResponseContract().jsonPathEquals("$.empty", "").jsonPathEquals("$.number", "42")
                                .jsonPathEquals("$.bool", "true").jsonPathEquals("$.nil", "null").everyArrayItemEquals("$.items", "x", ""),
                        false, AtfReportStatus.PASS, 5, 0, List.of("", "42", "true", "null", "[{\"x\":\"\"}]")),
                new Probe("scalar-containers", containers(), ValidationReportAcceptance::scalarContainers,
                        false, AtfReportStatus.FAIL, 3, 3, List.of("{}", "[]", "[{\"x\":{}}]")),
                new Probe("scalar-warning", containers(), ValidationReportAcceptance::scalarContainers,
                        true, AtfReportStatus.WARNING, 3, 3, List.of("{}", "[]", "[{\"x\":{}}]")),
                new Probe("scalar-structural-text", "{\"object\":{},\"array\":[]}",
                        () -> new ResponseContract().jsonPathEquals("$.object", "{}").jsonPathEquals("$.array", "[]"),
                        false, AtfReportStatus.FAIL, 2, 2, List.of("{}", "[]")),
                new Probe("scalar-mixed-items", "[{\"x\":\"\"},{\"x\":[]}]",
                        () -> new ResponseContract().everyArrayItemEquals("$", "x", ""),
                        false, AtfReportStatus.FAIL, 1, 1, List.of("[{\"x\":\"\"},{\"x\":[]}]"))));
        cases.add(media("media-nominal", "application/json; charset=UTF-8", false, true));
        cases.add(media("media-vendor", "application/vnd.atf.user+json", false, true));
        cases.add(media("media-parameter", "text/plain; profile=\"application/json\"", false, false));
        cases.add(media("media-warning", "text/plain; profile=\"application/json\"", true, false));
        cases.add(media("media-missing", null, false, false));
        cases.add(media("media-invalid-subtype", "application/jsonp", false, false));
        return List.copyOf(cases);
    }
    private static Probe media(String name, String header, boolean warning, boolean accepted) {
        return new Probe(name, "{\"id\":1,\"name\":\"ATF\"}", ContractValidators::userItem, warning,
                accepted ? AtfReportStatus.PASS : warning ? AtfReportStatus.WARNING : AtfReportStatus.FAIL,
                4, accepted ? 0 : 1, List.of("200", header == null ? "<missing>" : header, "1", "\"ATF\""),
                header == null ? Map.of() : Map.of("Content-Type", header));
    }
    private static String containers() { return "{\"object\":{},\"array\":[],\"items\":[{\"x\":{}}]}"; }
    private static ResponseContract scalarContainers() {
        return new ResponseContract().jsonPathEquals("$.object", "").jsonPathEquals("$.array", "")
                .everyArrayItemEquals("$.items", "x", "");
    }
    private static ResponseContract presence() {
        return new ResponseContract().jsonPathAbsent("$.x").jsonPathType("$.x", "null").jsonPathEquals("$.x", "<missing>");
    }
    private static Path schema(Path output, String name, String text) throws Exception {
        Path path = output.resolve(name + ".json"); Files.writeString(path, text); return path;
    }
    private static ResponseContract schemaContract(Path schema) {
        return new ResponseContract(new SchemaResourceResolver(schema.getParent())).jsonSchema(schema.getFileName().toString());
    }
    private static ValidationSink sink(AtfReportPorts.Request request) {
        return title -> {
            var target = request.startValidation(title);
            return new ValidationSink.Scope() {
                @Override public void assertions(Collection<atf.core.reporting.contract.AtfAssertion> rows) {
                    target.assertions(rows.stream().map(row -> new AtfAssertion(row.name(), row.expected(), row.actual(), row.passed(),
                            AtfReportStatus.valueOf(row.resultStatus().name()))).toList());
                }
                @Override public void complete(atf.core.reporting.contract.AtfReportStatus status) { target.complete(AtfReportStatus.valueOf(status.name())); }
                @Override public void close() { target.close(); }
            };
        };
    }
    private static void requireArtifact(String html, Probe probe) {
        String body = html.replaceAll("(?is)<script\\b[^>]*>.*?</script>|<style\\b[^>]*>.*?</style>", "");
        var tables = Pattern.compile("(?is)<table\\b[^>]*class=[\"'][^\"']*\\b(?:helm-validation-table|assertions)\\b[^\"']*[\"'][^>]*>(.*?)</table>").matcher(body).results().toList();
        if (tables.size() != 1) throw new AssertionError("Expected one aggregated validation table");
        String table = tables.getFirst().group(1);
        for (String name : List.of("ASSERTION", "EXPECTED", "ACTUAL", "RESULT"))
            if (!table.contains("<th>" + name + "</th>")) throw new AssertionError("Missing column " + name);
        var rows = Pattern.compile("(?is)<tr\\b[^>]*>(.*?)</tr>").matcher(table).results()
                .filter(row -> row.group(1).contains("<td")).toList();
        if (rows.size() != probe.rows()) throw new AssertionError("Missing comparison rows");
        int negatives = 0;
        var actuals = new ArrayList<String>();
        for (var row : rows) {
            var cells = Pattern.compile("(?is)<td\\b[^>]*>(.*?)</td>").matcher(row.group(1)).results().toList();
            if (cells.size() != 4) throw new AssertionError("Comparison must have four cells");
            actuals.add(cells.get(2).group(1).replaceAll("<[^>]+>", "").replace("&quot;", "\"")
                    .replace("&#39;", "'").replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&"));
            String marker = cells.getLast().group(1).replaceAll("<[^>]+>", "");
            if (List.of("❌", "Fail", "⚠️", "Warning", "⚠").contains(marker)) negatives++;
            else if (!List.of("✅", "Pass").contains(marker)) throw new AssertionError("Unknown verdict " + marker);
        }
        if (negatives != probe.failures()) throw new AssertionError("Negative verdict lost");
        if (!probe.actuals().isEmpty() && !actuals.equals(probe.actuals()))
            throw new AssertionError("Actual JSON/scalar evidence changed: " + actuals);
    }
    private static void rejectMutation(String html, Probe probe) {
        try { requireArtifact(html, probe); }
        catch (AssertionError expected) { return; }
        throw new AssertionError("Corrupted artifact escaped the consumer oracle");
    }
}
