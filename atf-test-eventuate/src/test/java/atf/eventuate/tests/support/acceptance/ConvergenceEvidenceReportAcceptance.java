package atf.eventuate.tests.support.acceptance;

import atf.eventuate.await.OrderConvergenceFailure;
import atf.eventuate.tests.component.*;
import atf.eventuate.tests.suites.EventuateComponentSuite;
import atf.eventuate.tests.support.fixtures.ConvergenceFailureFixtures;
import atf.eventuate.tests.support.fixtures.ExpectedConvergenceProbe;
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

/** Actual JUnit launches on the consumer binary; captured facts, identity and no replay are checked independently. */
public final class ConvergenceEvidenceReportAcceptance {
    private ConvergenceEvidenceReportAcceptance() { }
    public static void main(String[] args) throws Exception {
        Path output = Path.of(args[0]).toAbsolutePath().normalize();
        if (Files.exists(output)) throw new IllegalArgumentException("Fresh output required");
        Files.createDirectories(output);
        var previous = new LinkedHashMap<String, String>();
        for (String key : List.of("atf.eventuate.report.renderer", "atf.eventuate.report.directory", "atf.eventuate.convergence"))
            previous.put(key, System.getProperty(key));
        var receipts = new ArrayList<Map<String, Object>>();
        try {
            for (String renderer : List.of("EXTENT", "FALLBACK")) for (boolean suite : List.of(false, true)) {
                for (String name : List.of("read", "pause", "empty", "sink")) receipts.add(launch(output, renderer, name, suite, false));
                for (String name : List.of("read", "expired")) receipts.add(launch(output, renderer, name, suite, true));
            }
        } finally {
            ConvergenceFailureFixtures.consume();
            previous.forEach((key, value) -> { if (value == null) System.clearProperty(key); else System.setProperty(key, value); });
        }
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(output.resolve("acceptance.json").toFile(), receipts);
        System.out.println("CONVERGENCE_ACCEPTANCE reports=" + receipts.size() + "; original failure, no replay and artifact mutants=PASS");
    }
    private static Map<String, Object> launch(Path output, String renderer, String name, boolean suite, boolean expected) throws Exception {
        Path directory = output.resolve(renderer.toLowerCase(Locale.ROOT)).resolve((expected ? "expected-" : "") + name + (suite ? "-suite" : "-individual"));
        Files.createDirectories(directory);
        System.setProperty("atf.eventuate.report.renderer", renderer);
        System.setProperty("atf.eventuate.report.directory", directory.toString());
        System.setProperty("atf.eventuate.convergence", name); ConvergenceFailureFixtures.consume();
        var summary = new SummaryGeneratingListener();
        var builder = LauncherDiscoveryRequestBuilder.request().configurationParameter("junit.jupiter.execution.parallel.enabled", "false");
        if (expected) {
            builder.selectors(DiscoverySelectors.selectClass(ExpectedConvergenceProbe.class));
            if (suite) builder.selectors(DiscoverySelectors.selectClass(EventuateApiContractComponentTest.class),
                    DiscoverySelectors.selectClass(EventuateMessageEvidenceComponentTest.class));
            builder.configurationParameter("atf.test.run.scope", suite ? "suite" : "class");
        } else builder.selectors(DiscoverySelectors.selectClass(suite ? EventuateComponentSuite.class : EventuateConvergenceComponentTest.class));
        LauncherFactory.create().execute(builder.build(), summary);
        var result = summary.getSummary(); int count = suite ? 3 : 1;
        if (result.getTestsFoundCount() != count || result.getTestsStartedCount() != count || result.getTestsFailedCount() != (expected ? 0 : 1)
                || result.getTotalFailureCount() != (expected ? 0 : 1) || result.getTestsSkippedCount() != 0 || result.getTestsAbortedCount() != 0)
            throw new AssertionError("Unexpected JUnit outcome: " + result.getFailures());
        var capture = ConvergenceFailureFixtures.consume();
        if (capture == null || capture.snapshot() == null || !expected && result.getFailures().getFirst().getException() != capture.original())
            throw new AssertionError("Original failure or terminal snapshot was replaced");
        if (!name.equals("expired") && OrderConvergenceFailure.find(capture.original()).orElseThrow() != capture.snapshot())
            throw new AssertionError("Failure metadata differs from the captured snapshot");
        int reads = name.equals("expired") ? 0 : List.of("empty", "pause").contains(name) ? 1 : 2;
        if (capture.creations() != 1 || capture.reads() != reads || capture.dispatches() != reads + 1)
            throw new AssertionError("Mutation/read/capture replay detected");
        List<Path> reports;
        try (var files = Files.list(directory)) { reports = files.filter(path -> path.toString().endsWith(".html")).toList(); }
        if (reports.size() != 1) throw new AssertionError("Expected one class/suite artifact");
        String html = Files.readString(reports.getFirst());
        ConvergenceArtifactAudit.require(html, name, suite, expected);
        ConvergenceArtifactAudit.rejectMutants(html, name, suite, expected);
        var receipt = new LinkedHashMap<String, Object>();
        receipt.put("renderer", renderer); receipt.put("case", name); receipt.put("suite", suite); receipt.put("expectedFailure", expected);
        receipt.put("tests", count); receipt.put("failures", expected ? 0 : 1); receipt.put("sameOriginalException", true);
        receipt.put("report", output.relativize(reports.getFirst()).toString().replace('\\', '/'));
        receipt.put("creations", capture.creations()); receipt.put("reads", capture.reads()); receipt.put("dispatches", capture.dispatches());
        receipt.put("termination", capture.snapshot().termination().name()); receipt.put("observations", capture.snapshot().observations().size());
        return receipt;
    }
}
