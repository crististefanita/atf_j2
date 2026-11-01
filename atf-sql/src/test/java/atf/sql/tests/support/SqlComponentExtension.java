package atf.sql.tests.support;

import atf.testkit.assertions.*;
import atf.reporting.audit.AssertionAuditReport;
import atf.reporting.configuration.AtfReportConfigurationLoader;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.html.HtmlReportSession;
import atf.reporting.model.ReportField;
import atf.reporting.model.ReportObservation;
import atf.reporting.model.diagnostic.ReportPhaseDiagnostic;
import atf.sql.result.SqlSnapshot;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.extension.*;
import java.nio.file.Path;
import java.nio.file.Files;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Test-only JUnit/JAR bridge; production atf-sql has no Reporting dependency. */
public final class SqlComponentExtension implements ComponentAuditAdapter {
    private static final ObjectMapper JSON = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private static final ExtensionContext.Namespace DATA = ExtensionContext.Namespace.create(SqlComponentExtension.class);
    private final ComponentAuditExtension audit = ComponentAuditExtension.scoped(SqlComponentExtension.class, Owner.class,
            context -> new Owner(), SqlComponentExtension::publication);
    @Override public ComponentAuditExtension auditExtension() { return audit; }
    private static ComponentAuditExtension.Publication publication(Owner owner, ExtensionContext context) {
        var facts = new SqlTestEvidence(); context.getStore(DATA).put(SqlTestEvidence.class, facts);
        return new ComponentAuditExtension.Publication() {
            @Override public AssertionValueDisplay display() { return new AssertionValueDisplay(owner.preview, owner.redactor::redactField); }
            @Override public ComponentAssertionAudit.AssertionRequirement assertionRequirement() { return ComponentAssertionAudit.AssertionRequirement.TEST_BODY; }
            @Override public void publish(ComponentAssertionAudit.Result result) throws Exception {
                try (var scenario = owner.session.startScenario(context.getRequiredTestClass().getSimpleName(), context.getRequiredTestClass().getPackageName(),
                        context.getDisplayName(), List.of("component", "sql", "embedded"))) {
                    for (var data : result.phases()) try (var phase = scenario.startPhase(data.phase().name().replace('_', ' '))) {
                        data.timing().ifPresent(value -> phase.executionTiming(new atf.reporting.model.timing.ReportPhaseTiming(value.elapsed(), value.intervals())));
                        if (data.phase() == ComponentAssertionAudit.Phase.TEST_BODY) for (var snapshot : facts.snapshots()) {
                            phase.observation(new ReportObservation("SQL capture · " + snapshot.outcome(), List.of(
                                    new ReportField("SQL execution ID", snapshot.executionId()), new ReportField("SQL provenance", "SQL_EMBEDDED")),
                                    List.of(new ReportObservation.Body("Query, typed binds and captured rows", projection(snapshot), ReportObservation.Format.JSON)), List.of()));
                        }
                        AssertionAuditReport.publishStep(phase, "Component assertions", data.checks().stream().map(check ->
                                new atf.reporting.contract.AtfAssertion(check.name(), check.expected(), check.actual(), check.passed())).toList(), AtfReportStatus.INFO);
                        if (!data.diagnostic().isBlank()) phase.diagnostic(ReportPhaseDiagnostic.Kind.EXECUTION_FAILURE, owner.redactor.redactText(data.diagnostic()));
                        phase.complete(status(data.outcome()));
                    }
                    scenario.complete(status(result.outcome()));
                }
            }
            @Override public void close() { context.getStore(DATA).remove(SqlTestEvidence.class); }
        };
    }
    private static String projection(SqlSnapshot snapshot) throws java.io.IOException {
        var fields = new java.util.LinkedHashMap<String, Object>();
        fields.put("executionId", snapshot.executionId()); fields.put("query", snapshot.query().template());
        fields.put("parameters", snapshot.query().parameters().stream().map(parameter -> {
            var value = new java.util.LinkedHashMap<String, Object>(); value.put("type", parameter.type().name());
            value.put("sensitive", parameter.sensitive()); value.put("value", parameter.sensitive() ? "[REDACTED]" : parameter.value()); return value;
        }).toList());
        fields.put("columns", snapshot.columns()); fields.put("rows", snapshot.rows());
        fields.put("outcome", snapshot.outcome().name()); fields.put("moreRows", snapshot.moreRows());
        fields.put("readOnlyAcknowledged", snapshot.readOnlyAcknowledged()); fields.put("resourcesClosed", snapshot.resourcesClosed());
        fields.put("elapsed", snapshot.elapsed().toString()); fields.put("failure", snapshot.failure());
        return JSON.writeValueAsString(fields);
    }
    private static AtfReportStatus status(ComponentAssertionAudit.Outcome value) {
        return switch (value) { case PASS -> AtfReportStatus.PASS; case FAIL -> AtfReportStatus.FAIL; case ABORTED -> AtfReportStatus.SKIP; };
    }
    @Override public boolean supportsParameter(ParameterContext parameter, ExtensionContext context) {
        return parameter.getParameter().getType() == SqlTestEvidence.class || audit.supportsParameter(parameter, context);
    }
    @Override public Object resolveParameter(ParameterContext parameter, ExtensionContext context) {
        return parameter.getParameter().getType() == SqlTestEvidence.class ? context.getStore(DATA).get(SqlTestEvidence.class, SqlTestEvidence.class)
                : audit.resolveParameter(parameter, context);
    }
    private static final class Owner implements AutoCloseable {
        private final AtfReportPorts.Session session;
        private final int preview;
        private final atf.reporting.security.SensitiveDataRedactor redactor = new atf.reporting.security.SensitiveDataRedactor();
        private Owner() {
            var settings = AtfReportConfigurationLoader.load().reportingSettings(); preview = settings.assertionMaxDisplayCharacters();
            Path directory = Path.of(System.getProperty("atf.sql.report.directory", "target/sql-reports")).toAbsolutePath();
            try { Files.createDirectories(directory); } catch (java.io.IOException failure) { throw new java.io.UncheckedIOException(failure); }
            Path file = directory.resolve("sql-" + Instant.now().toString().replace(':', '-') + "-" + UUID.randomUUID().toString().substring(0, 8) + ".html");
            session = System.getProperty("atf.sql.report.renderer", "EXTENT").equals("FALLBACK") ? new HtmlReportSession(file, settings) : new ExtentReportSession(file, settings);
        }
        @Override public void close() { session.close(); }
    }
}
