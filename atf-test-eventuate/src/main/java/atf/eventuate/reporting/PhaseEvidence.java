package atf.eventuate.reporting;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.model.timing.ReportPhaseTiming;
import java.util.List;
/** Renderer-facing projection of checks that JUnit has already evaluated. */
public record PhaseEvidence(String name, List<AtfAssertion> checks, AtfReportStatus status,
                            String diagnostic, ReportPhaseTiming timing) {
    public PhaseEvidence { checks = List.copyOf(checks); }
}
