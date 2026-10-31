package atf.eventuate.tests.support.acceptance;

import atf.eventuate.tests.component.*;
import atf.eventuate.tests.suites.EventuateComponentSuite;
import atf.reporting.artifact.ReportArtifactInspector;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.*;
import java.util.*;

/** Consumer acceptance matrix; controlled faults reuse the same three fixed test oracles. */
public final class EventuateReportAcceptance {
    private EventuateReportAcceptance() { }
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Expected a fresh report matrix directory");
        Path output = Path.of(args[0]).toAbsolutePath().normalize();
        if (Files.exists(output)) throw new IllegalArgumentException("Acceptance output already exists");
        Files.createDirectories(output);
        var receipts = new ArrayList<Map<String, Object>>();
        var previous = new HashMap<String, String>();
        for (String key : List.of("atf.eventuate.report.renderer", "atf.eventuate.report.directory", "atf.eventuate.probe"))
            previous.put(key, System.getProperty(key));
        try {
            for (String renderer : List.of("EXTENT", "FALLBACK")) {
                launch(output, renderer, "api-individual", EventuateApiContractComponentTest.class, "none", 1, 0, receipts);
                launch(output, renderer, "convergence-individual", EventuateConvergenceComponentTest.class, "none", 1, 0, receipts);
                launch(output, renderer, "message-individual", EventuateMessageEvidenceComponentTest.class, "none", 1, 0, receipts);
                launch(output, renderer, "combined-suite", EventuateComponentSuite.class, "none", 3, 0, receipts);
                launch(output, renderer, "controlled-late-mismatch", EventuateApiContractComponentTest.class, "late-mismatch", 1, 1, receipts);
                launch(output, renderer, "controlled-duplicate-effect", EventuateMessageEvidenceComponentTest.class, "duplicate-effect", 1, 1, receipts);
                launch(output, renderer, "controlled-wrong-identity", EventuateApiContractComponentTest.class, "wrong-identity", 1, 1, receipts);
            }
        } finally {
            previous.forEach((key, value) -> { if (value == null) System.clearProperty(key); else System.setProperty(key, value); });
        }
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(output.resolve("acceptance.json").toFile(), receipts);
        System.out.println("EVENTUATE_ACCEPTANCE reports=" + receipts.size() + "; invocations=18; controlledFailures=6");
    }

    private static void launch(Path output, String renderer, String name, Class<?> selected, String probe,
                               int expectedTests, int expectedFailures, List<Map<String, Object>> receipts) throws Exception {
        Path directory = output.resolve(renderer.toLowerCase(Locale.ROOT)).resolve(name);
        Files.createDirectories(directory);
        System.setProperty("atf.eventuate.report.renderer", renderer);
        System.setProperty("atf.eventuate.report.directory", directory.toString());
        System.setProperty("atf.eventuate.probe", probe);
        var summary = new SummaryGeneratingListener();
        var request = LauncherDiscoveryRequestBuilder.request().selectors(DiscoverySelectors.selectClass(selected))
                .configurationParameter("junit.jupiter.execution.parallel.enabled", "false").build();
        LauncherFactory.create().execute(request, summary);
        var result = summary.getSummary();
        if (result.getTestsFoundCount() != expectedTests || result.getTestsStartedCount() != expectedTests
                || result.getTestsFailedCount() != expectedFailures || result.getTestsSkippedCount() != 0
                || result.getTestsAbortedCount() != 0 || result.getTotalFailureCount() != expectedFailures) {
            result.getFailures().forEach(failure -> failure.getException().printStackTrace());
            throw new AssertionError(renderer + "/" + name + ": unexpected JUnit outcome");
        }
        List<Path> reports;
        try (var files = Files.list(directory)) { reports = files.filter(path -> path.toString().endsWith(".html")).toList(); }
        if (reports.size() != 1) throw new AssertionError("Expected one fresh report, found " + reports.size());
        Path report = reports.getFirst();
        var inspection = ReportArtifactInspector.inspect(report);
        String html = Files.readString(report);
        int rows = probe.equals("wrong-identity") ? 4 : expectedFailures > 0 ? 6 : expectedTests == 3 ? 37
                : selected == EventuateApiContractComponentTest.class ? 9 : selected == EventuateConvergenceComponentTest.class ? 16 : 12;
        List<String> comparisons = probe.equals("wrong-identity") ? List.of("Order identity") : selected == EventuateApiContractComponentTest.class
                ? List.of("Order identity", "Order state", "Full synthetic audit value before display preview")
                : selected == EventuateConvergenceComponentTest.class
                ? List.of("Creation dispatches", "HTTP attempts in first read", "No read at the expired deadline", "Foreign approved order cannot converge", "Observed foreign order identity")
                : selected == EventuateMessageEvidenceComponentTest.class
                ? List.of("Same message and order identity", "Independent reservation count")
                : List.of("Order identity", "Creation dispatches", "Independent reservation count");
        EventuateArtifactAudit.require(html, expectedTests, rows, expectedFailures, comparisons);
        EventuateArtifactAudit.requireCounterexamples(html, expectedTests, rows, expectedFailures, comparisons);
        if (expectedFailures > 0 && result.getFailures().stream().anyMatch(failure -> !(failure.getException() instanceof AssertionError)))
            throw new AssertionError("Controlled probe failed outside the assertion boundary");
        var row = new LinkedHashMap<String, Object>();
        row.put("renderer", renderer); row.put("case", name); row.put("report", output.relativize(report).toString().replace('\\', '/'));
        row.put("tests", expectedTests); row.put("failures", expectedFailures); row.put("publishedAssertionRows", rows);
        row.put("failureTypes", result.getFailures().stream().map(failure -> failure.getException().getClass().getName()).toList());
        row.put("consumerArtifactContract", "PASS; including removed-table and inverted-result counterexamples");
        row.put("genericInspectorCompliant", inspection.isCompliant()); row.put("genericInspectorChecks", inspection.checks());
        row.put("sha256", java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(report))));
        receipts.add(row);
    }
}
