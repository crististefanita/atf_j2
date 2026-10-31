package atf.eventuate.tests.support.fixtures;

import atf.core.http.execution.evidence.ApiExecutionFailure;
import atf.eventuate.evidence.EventuateEvidence;
import atf.eventuate.tests.support.EventuateComponentTest;
import atf.eventuate.tests.support.EventuateComponentSupport;
import atf.testkit.assertions.RecordedAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Technical acceptance fixture, selected explicitly; business oracles remain fixed. */
@EventuateComponentTest
public class ExpectedTerminalProbe extends EventuateComponentSupport {
    @Test @DisplayName("Expected terminal failure · consumer contract")
    void verifiesExpectedFailure(RecordedAssertions checks, EventuateEvidence evidence) {
        try { TerminalFailureFixtures.failIfSelected(evidence); }
        catch (RuntimeException failure) {
            var snapshot = ApiExecutionFailure.find(failure).orElseThrow();
            checks.equals("Expected terminal outcome", "FAILED", snapshot.termination().name());
            checks.isTrue("Expected exception keeps original identity", TerminalFailureFixtures.isCapturedOriginal(failure));
            checks.isTrue("Expected terminal has no HTTP response", snapshot.response().isEmpty());
            return;
        }
        throw new AssertionError("Expected terminal failure was not observed");
    }
}
