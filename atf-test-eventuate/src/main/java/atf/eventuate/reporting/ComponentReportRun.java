package atf.eventuate.reporting;
import atf.eventuate.evidence.EventuateEvidence;
import atf.reporting.audit.AssertionAuditReport;
import atf.reporting.configuration.ReportingSettings;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.html.HtmlReportSession;
import atf.reporting.model.diagnostic.ReportPhaseDiagnostic;
import atf.reporting.security.SensitiveDataRedactor;
import java.nio.file.Path;
import java.nio.file.Files;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Serial JUnit run owner; one session per selected class/suite and a fresh scenario per invocation. */
public final class ComponentReportRun implements AutoCloseable {
    public enum Renderer { EXTENT, FALLBACK }
    private final AtfReportPorts.Session session;
    private final ReportingSettings settings;
    private final SensitiveDataRedactor redactor = new SensitiveDataRedactor();
    private final Thread owner = Thread.currentThread();
    private boolean closed;
    public ComponentReportRun(Path directory, Renderer renderer) {
        try { Files.createDirectories(directory); } catch (java.io.IOException failure) { throw new java.io.UncheckedIOException(failure); }
        Path file = directory.resolve("eventuate-" + renderer.name().toLowerCase(java.util.Locale.ROOT) + "-"
                + Instant.now().toString().replace(':', '-') + "-" + UUID.randomUUID().toString().substring(0, 8) + ".html");
        settings = atf.reporting.configuration.AtfReportConfigurationLoader.load().reportingSettings();
        session = renderer == Renderer.EXTENT ? new ExtentReportSession(file, settings)
                : new HtmlReportSession(file, settings);
    }
    public String display(String name, String value) { return redactor.redactField(name, value); }
    public int previewLimit() { return settings.assertionMaxDisplayCharacters(); }
    public void publish(String testClass, String testPackage, String title, List<PhaseEvidence> phases,
                        AtfReportStatus status, EventuateEvidence evidence) {
        requireOwner();
        try (var scenario = session.startScenario(testClass, testPackage, title, List.of("component", "eventuate", "mock"))) {
            for (var data : phases) try (var phase = scenario.startPhase(data.name())) {
                phase.detail("Evidence source", evidence.source());
                if (data.timing() != null) phase.executionTiming(data.timing());
                if (data.name().equals("TEST BODY")) ApiEvidencePublisher.publish(phase, evidence.exchanges());
                AssertionAuditReport.publishStep(phase, "Component assertions", data.checks(), data.status());
                if (!data.diagnostic().isBlank()) phase.diagnostic(ReportPhaseDiagnostic.Kind.EXECUTION_FAILURE, redactor.redactText(data.diagnostic()));
                phase.complete(data.status());
            }
            scenario.complete(status);
        }
    }
    private void requireOwner() {
        if (closed || Thread.currentThread() != owner) throw new IllegalStateException("Closed report or concurrent publication; component runs are serial");
    }
    public Path reportPath() { return session.reportPath().toAbsolutePath().normalize(); }
    @Override public void close() {
        if (closed) return;
        requireOwner(); session.close(); closed = true;
        if (!Files.isRegularFile(reportPath())) throw new IllegalStateException("Report was not published");
    }
}
