package atf.eventuate.tests.support;
import atf.eventuate.evidence.EventuateEvidence;
import atf.eventuate.reporting.ComponentReportRun;
import atf.eventuate.reporting.PhaseEvidence;
import atf.testkit.assertions.*;
import atf.testkit.reporting.AtfReportPath;
import atf.testkit.reporting.AtfReportPathPolicyResolver;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import org.junit.jupiter.api.extension.*;
import java.nio.file.Path;
import java.lang.reflect.Method;

/** JUnit owns scope and outcome; main owns publication and reusable support. */
public final class EventuateComponentExtension implements ComponentAuditAdapter {
    private static final ExtensionContext.Namespace DATA = ExtensionContext.Namespace.create(EventuateComponentExtension.class);
    private final ComponentAuditExtension audit = ComponentAuditExtension.scoped(EventuateComponentExtension.class,
            Owner.class, EventuateComponentExtension::owner, EventuateComponentExtension::publication);
    @Override public ComponentAuditExtension auditExtension() { return audit; }
    @Override public void interceptBeforeEachMethod(Invocation<Void> call, ReflectiveInvocationContext<Method> method,
                                                    ExtensionContext context) throws Throwable {
        try (var scope = evidence(context).inPhase("SETUP")) { audit.interceptBeforeEachMethod(call, method, context); }
    }
    @Override public void interceptAfterEachMethod(Invocation<Void> call, ReflectiveInvocationContext<Method> method,
                                                   ExtensionContext context) throws Throwable {
        try (var scope = evidence(context).inPhase("TEARDOWN")) { audit.interceptAfterEachMethod(call, method, context); }
    }
    private static EventuateEvidence evidence(ExtensionContext context) {
        return context.getStore(DATA).get(EventuateEvidence.class, EventuateEvidence.class);
    }
    private static Owner owner(ExtensionContext context) {
        Path output = Path.of(System.getProperty("atf.eventuate.report.directory", "target/eventuate-reports")).toAbsolutePath().normalize();
        var renderer = ComponentReportRun.Renderer.valueOf(System.getProperty("atf.eventuate.report.renderer", "EXTENT").toUpperCase(java.util.Locale.ROOT));
        return new Owner(new ComponentReportRun(output, renderer));
    }
    private static final class Owner implements AutoCloseable {
        private final ComponentReportRun report;
        private boolean printPath;
        private Owner(ComponentReportRun report) { this.report = report; }
        @Override public void close() {
            report.close();
            if (printPath) System.out.println("ATF_REPORT_PATH=" + report.reportPath());
        }
    }
    private static ComponentAuditExtension.Publication publication(Owner owner, ExtensionContext context) {
        var evidence = new EventuateEvidence();
        context.getStore(DATA).put(EventuateEvidence.class, evidence);
        owner.printPath |= AtfReportPathPolicyResolver.resolve(context.getRequiredTestClass()) == AtfReportPath.Policy.ENABLED;
        return new ComponentAuditExtension.Publication() {
            @Override public AssertionValueDisplay display() { return new AssertionValueDisplay(owner.report.previewLimit(), owner.report::display); }
            @Override public void publish(ComponentAssertionAudit.Result result) {
                var phases = result.phases().stream().map(phase -> new PhaseEvidence(phase.phase().name().replace('_', ' '),
                        phase.checks().stream().map(check -> new AtfAssertion(check.name(), check.expected(), check.actual(), check.passed())).toList(),
                        status(phase.outcome()), executionDiagnostic(phase, result), phase.timing().map(timing ->
                        new atf.reporting.model.timing.ReportPhaseTiming(timing.elapsed(), timing.intervals())).orElse(null))).toList();
                owner.report.publish(context.getRequiredTestClass().getSimpleName(), context.getRequiredTestClass().getPackageName(),
                        context.getDisplayName(), phases, status(result.outcome()), evidence);
            }
            @Override public void close() { context.getStore(DATA).remove(EventuateEvidence.class); }
        };
    }
    private static AtfReportStatus status(ComponentAssertionAudit.Outcome outcome) {
        return switch (outcome) { case PASS -> AtfReportStatus.PASS; case FAIL -> AtfReportStatus.FAIL; case ABORTED -> AtfReportStatus.SKIP; };
    }
    private static String executionDiagnostic(ComponentAssertionAudit.PhaseResult phase, ComponentAssertionAudit.Result result) {
        boolean assertionOwnsFailure = result.failure() instanceof AssertionError
                && phase.checks().stream().anyMatch(check -> !check.passed());
        return assertionOwnsFailure ? "" : phase.diagnostic();
    }
    @Override public boolean supportsParameter(ParameterContext parameter, ExtensionContext context) {
        return parameter.getParameter().getType() == EventuateEvidence.class || audit.supportsParameter(parameter, context);
    }
    @Override public Object resolveParameter(ParameterContext parameter, ExtensionContext context) {
        return parameter.getParameter().getType() == EventuateEvidence.class ? context.getStore(DATA).get(EventuateEvidence.class, EventuateEvidence.class)
                : audit.resolveParameter(parameter, context);
    }
}
