package atf.eventuate.tests.e2e;

import atf.eventuate.evidence.EventuateEvidence;
import atf.eventuate.live.LiveOrderScenario;
import atf.eventuate.live.LiveSettings;
import atf.eventuate.tests.e2e.support.LiveOrderChecks;
import atf.eventuate.tests.support.EventuateLiveTest;
import atf.testkit.assertions.RecordedAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

@EventuateLiveTest
public class EventuateInsufficientCreditE2eTest {
    @Test @DisplayName("LIVE · insufficient credit · no persisted reservation")
    void rejectsWithoutReservingCredit(RecordedAssertions checks, EventuateEvidence evidence) throws Exception {
        var result = new LiveOrderScenario(LiveSettings.load()).execute(LiveOrderScenario.Case.INSUFFICIENT_CREDIT, evidence);
        LiveOrderChecks.verify(checks, evidence, result, "REJECTED", "INSUFFICIENT_CREDIT", 0, 1);
    }
}
