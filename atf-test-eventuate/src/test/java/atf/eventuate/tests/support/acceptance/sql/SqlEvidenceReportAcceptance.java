package atf.eventuate.tests.support.acceptance.sql;

import atf.eventuate.tests.component.*;
import atf.eventuate.tests.component.sql.*;
import atf.eventuate.tests.suites.EventuateSqlComponentSuite;
import atf.eventuate.tests.support.sql.SqlReportProbe;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import java.nio.file.*;
import java.util.*;

/** Runs the real H2/JUnit/Reporting binaries individually, by suite and combined with MOCK peers. */
public final class SqlEvidenceReportAcceptance {
    private static final Map<String, Class<?>> CASES = Map.of("transactions", EventuateSqlTransactionComponentTest.class,
            "identity", EventuateSqlIdentityComponentTest.class, "failures", EventuateSqlFailureComponentTest.class);
    private SqlEvidenceReportAcceptance() { }
    public static void main(String[] args) throws Exception {
        Path output = Path.of(args[0]).toAbsolutePath().normalize();
        if (Files.exists(output)) throw new IllegalArgumentException("Fresh output required");
        Files.createDirectories(output); var previous = new LinkedHashMap<String,String>();
        for (String key : List.of("atf.eventuate.report.renderer", "atf.eventuate.report.directory", "atf.eventuate.sql.probe")) previous.put(key, System.getProperty(key));
        var receipts = new ArrayList<Map<String,Object>>();
        try {
            for (String renderer : List.of("EXTENT", "FALLBACK")) {
                for (String selection : List.of("transactions", "identity", "failures", "suite", "combined")) receipts.add(run(output,renderer,selection,"none"));
                for (String probe : List.of("duplicate", "sql-error")) {
                    receipts.add(run(output,renderer,probe.equals("duplicate") ? "identity" : "failures",probe));
                    receipts.add(run(output,renderer,"suite",probe));
                }
            }
        } finally {
            SqlReportProbe.consume(); previous.forEach((key,value) -> { if (value == null) System.clearProperty(key); else System.setProperty(key,value); });
        }
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(output.resolve("acceptance.json").toFile(),receipts);
        System.out.println("SQL_ACCEPTANCE reports=" + receipts.size() + "; JDBC facts, original failure, assertions and artifact mutants=PASS");
    }
    private static Map<String,Object> run(Path output, String renderer, String selected, String probe) throws Exception {
        Path directory = output.resolve(renderer.toLowerCase(Locale.ROOT)).resolve(selected + "-" + probe); Files.createDirectories(directory);
        System.setProperty("atf.eventuate.report.renderer", renderer); System.setProperty("atf.eventuate.report.directory",directory.toString());
        System.setProperty("atf.eventuate.sql.probe",probe); SqlReportProbe.consume();
        var builder = LauncherDiscoveryRequestBuilder.request().configurationParameter("junit.jupiter.execution.parallel.enabled","false");
        if (selected.equals("suite")) builder.selectors(DiscoverySelectors.selectClass(EventuateSqlComponentSuite.class));
        else if (selected.equals("combined")) {
            builder.selectors(DiscoverySelectors.selectClass(EventuateApiContractComponentTest.class), DiscoverySelectors.selectClass(EventuateConvergenceComponentTest.class),
                    DiscoverySelectors.selectClass(EventuateMessageEvidenceComponentTest.class), DiscoverySelectors.selectClass(EventuateSqlTransactionComponentTest.class),
                    DiscoverySelectors.selectClass(EventuateSqlIdentityComponentTest.class), DiscoverySelectors.selectClass(EventuateSqlFailureComponentTest.class));
            builder.configurationParameter("atf.test.run.scope","suite");
        } else builder.selectors(DiscoverySelectors.selectClass(CASES.get(selected)));
        var listener = new SummaryGeneratingListener(); LauncherFactory.create().execute(builder.build(),listener);
        var result = listener.getSummary(); int count = selected.equals("combined") ? 6 : selected.equals("suite") ? 3 : 1;
        int failed = probe.equals("none") ? 0 : 1;
        if (result.getTestsFoundCount() != count || result.getTestsStartedCount() != count || result.getTestsFailedCount() != failed
                || result.getTotalFailureCount() != failed || result.getTestsSkippedCount() != 0 || result.getTestsAbortedCount() != 0)
            throw new AssertionError("Unexpected SQL JUnit outcome: " + result.getFailures());
        var original = SqlReportProbe.consume();
        if (probe.equals("duplicate") && (!(result.getFailures().getFirst().getException() instanceof AssertionError)
                || !result.getFailures().getFirst().getException().getMessage().contains("Controlled duplicate effect mismatch")))
            throw new AssertionError("Unexpected cause for controlled duplicate failure");
        if (probe.equals("sql-error") && (original == null || result.getFailures().getFirst().getException() != original))
            throw new AssertionError("SQL exception was replaced before JUnit");
        List<Path> reports; try (var files = Files.list(directory)) { reports = files.filter(path -> path.toString().endsWith(".html")).toList(); }
        if (reports.size() != 1) throw new AssertionError("One SQL artifact required");
        String html = Files.readString(reports.getFirst()); SqlArtifactAudit.require(html,selected,probe); SqlArtifactAudit.rejectMutants(html,selected,probe);
        return Map.of("renderer",renderer,"selection",selected,"probe",probe,"tests",count,"failures",failed,
                "report",output.relativize(reports.getFirst()).toString().replace('\\','/'),"artifactAndMutants","PASS");
    }
}
