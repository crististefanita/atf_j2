package atf.eventuate.tests.support.fixtures;

import atf.eventuate.evidence.EventuateEvidence;
import atf.eventuate.tests.support.EventuateComponentTest;
import atf.eventuate.tests.support.EventuateComponentSupport;
import atf.testkit.assertions.RecordedAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Expected terminal conditions stay PASS while their non-assertion evidence remains observable. */
@EventuateComponentTest
public class ExpectedConvergenceProbe extends EventuateComponentSupport {
    @Test @DisplayName("Expected convergence terminal · captured facts")
    void expectedTerminal(RecordedAssertions checks, EventuateEvidence evidence) {
        try { ConvergenceFailureFixtures.failIfSelected(evidence); }
        catch (RuntimeException | Error original) {
            var snapshot = evidence.convergences().getFirst();
            boolean expired = ConvergenceFailureFixtures.selected().equals("expired");
            checks.equals("Captured convergence snapshots", 1, evidence.convergences().size());
            checks.equals("Terminal observation count", expired ? 0 : 1, snapshot.observations().size());
            checks.equals("Expected convergence termination", expired ? "DEADLINE_REACHED" : "READ_FAILURE", snapshot.termination().name());
            checks.equals("Original independent expected order", 42L, snapshot.expected().orderId());
            checks.isTrue("Original terminal condition remains visible", original.getMessage() != null);
            return;
        }
        throw new AssertionError("Terminal fixture did not run");
    }
}
