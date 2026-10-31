package atf.eventuate.tests.support.acceptance;

import atf.core.http.execution.evidence.ApiExecutionFailure;
import atf.eventuate.tests.component.EventuateApiContractComponentTest;
import atf.eventuate.tests.component.EventuateConvergenceComponentTest;
import atf.eventuate.tests.component.EventuateMessageEvidenceComponentTest;
import atf.eventuate.tests.suites.EventuateComponentSuite;
import atf.eventuate.tests.support.fixtures.TerminalFailureFixtures;
import atf.eventuate.tests.support.fixtures.ExpectedTerminalProbe;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Original Core exception through JUnit, with one terminal request per fresh consumer artifact. */
public final class TerminalEvidenceReportAcceptance {
    private TerminalEvidenceReportAcceptance() { }
    public static void main(String[] args) throws Exception {
        Path output = Path.of(args[0]).toAbsolutePath().normalize();
        if (Files.exists(output)) throw new IllegalArgumentException("Fresh output required");
        Files.createDirectories(output);
        var receipts = new ArrayList<Map<String, Object>>();
        var previous = new LinkedHashMap<String, String>();
        for (String key : List.of("atf.eventuate.report.renderer", "atf.eventuate.report.directory", "atf.eventuate.probe", "atf.eventuate.terminal", "atf.eventuate.terminal.phase"))
            previous.put(key, System.getProperty(key));
        try {
            for (String renderer : List.of("EXTENT", "FALLBACK")) {
                for (String name : TerminalFailureFixtures.CASES)
                    for (boolean suite : List.of(false, true)) receipts.add(launch(output, renderer, name, suite, "TEST BODY", false));
                for (String phase : List.of("SETUP", "TEARDOWN"))
                    for (boolean suite : List.of(false, true)) receipts.add(launch(output, renderer, "exhausted", suite, phase, false));
                for (boolean suite : List.of(false, true)) receipts.add(launch(output, renderer, "transport", suite, "TEST BODY", true));
            }
        } finally {
            TerminalFailureFixtures.consumeFailure();
            previous.forEach((key, value) -> { if (value == null) System.clearProperty(key); else System.setProperty(key, value); });
        }
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(output.resolve("acceptance.json").toFile(), receipts);
        System.out.println("TERMINAL_ACCEPTANCE reports=" + receipts.size() + "; original exceptions and artifact mutations=PASS");
    }
    private static Map<String, Object> launch(Path output, String renderer, String name, boolean suite, String phase, boolean expected) throws Exception {
        Path directory = output.resolve(renderer.toLowerCase(Locale.ROOT)).resolve((expected ? "expected-" : "") + name + "-" + phase.replace(' ', '-').toLowerCase(Locale.ROOT) + (suite ? "-suite" : "-individual"));
        Files.createDirectories(directory);
        System.setProperty("atf.eventuate.report.renderer", renderer);
        System.setProperty("atf.eventuate.report.directory", directory.toString());
        System.setProperty("atf.eventuate.probe", "none");
        System.setProperty("atf.eventuate.terminal", name);
        System.setProperty("atf.eventuate.terminal.phase", phase);
        TerminalFailureFixtures.consumeFailure();
        var summary = new SummaryGeneratingListener();
        var builder = LauncherDiscoveryRequestBuilder.request().configurationParameter("junit.jupiter.execution.parallel.enabled", "false");
        if (expected) {
            builder.selectors(DiscoverySelectors.selectClass(ExpectedTerminalProbe.class));
            if (suite) builder.selectors(DiscoverySelectors.selectClass(EventuateConvergenceComponentTest.class),
                    DiscoverySelectors.selectClass(EventuateMessageEvidenceComponentTest.class));
            builder.configurationParameter("atf.test.run.scope", suite ? "suite" : "class");
        } else builder.selectors(DiscoverySelectors.selectClass(suite ? EventuateComponentSuite.class : EventuateApiContractComponentTest.class));
        var request = builder.build();
        LauncherFactory.create().execute(request, summary);
        var result = summary.getSummary();
        int count = suite ? 3 : 1;
        if (result.getTestsFoundCount() != count || result.getTestsStartedCount() != count || result.getTestsFailedCount() != (expected ? 0 : 1)
                || result.getTotalFailureCount() != (expected ? 0 : 1) || result.getTestsAbortedCount() != 0 || result.getTestsSkippedCount() != 0)
            throw new AssertionError("Unexpected JUnit outcome: " + result.getFailures());
        Throwable original = TerminalFailureFixtures.consumeFailure();
        if (original == null || !expected && result.getFailures().getFirst().getException() != original)
            throw new AssertionError("Original execution exception was replaced or no snapshot was collected");
        if (original.getSuppressed().length != 0) throw new AssertionError("Unexpected secondary failure", original);
        var snapshot = ApiExecutionFailure.find(original).orElseThrow();
        if (snapshot.response().isPresent()) throw new AssertionError("Fixture has no terminal HTTP response");
        List<Path> reports;
        try (var files = Files.list(directory)) { reports = files.filter(path -> path.toString().endsWith(".html")).toList(); }
        if (reports.size() != 1) throw new AssertionError("Expected one report per launch");
        String html = Files.readString(reports.getFirst());
        TerminalArtifactAudit.require(html, name, suite, snapshot.termination().name(), snapshot.attempts(), phase, expected);
        TerminalArtifactAudit.rejectMutants(html, name, suite, snapshot.termination().name(), snapshot.attempts(), phase, expected);
        var row = new LinkedHashMap<String, Object>();
        row.put("renderer", renderer); row.put("case", name); row.put("suite", suite);
        row.put("phase", phase);
        row.put("report", output.relativize(reports.getFirst()).toString().replace('\\', '/'));
        row.put("tests", count); row.put("failures", expected ? 0 : 1); row.put("sameOriginalException", true);
        row.put("expectedFailure", expected);
        row.put("failureType", original.getClass().getName()); row.put("cause", String.valueOf(original.getCause()));
        row.put("termination", snapshot.termination().name()); row.put("attempts", snapshot.attempts());
        row.put("elapsedNanos", snapshot.totalDuration().toNanos()); row.put("responsePresent", false);
        return row;
    }
}
