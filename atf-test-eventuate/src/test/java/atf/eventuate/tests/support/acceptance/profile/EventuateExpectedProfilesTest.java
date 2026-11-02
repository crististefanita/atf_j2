package atf.eventuate.tests.support.acceptance.profile;

import atf.eventuate.tests.component.*;
import atf.eventuate.tests.suites.EventuateComponentSuite;
import atf.reporting.artifact.ReportArtifactProfile;
import atf.reporting.contract.AtfReportStatus;
import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
class EventuateExpectedProfilesTest {
    @Test void messageOnlyIntentContainsThreePhasesAndNoInventedHttp() {
        var scenario = EventuateExpectedProfiles.expected("FALLBACK", EventuateMessageEvidenceComponentTest.class, "none").scenarios().getFirst();
        assertEquals(3, scenario.phases().size());
        assertEquals(1, scenario.phases().get(1).evidence().size());
        assertInstanceOf(ReportArtifactProfile.Assertions.class, scenario.phases().get(1).evidence().getFirst());
        assertEquals(AtfReportStatus.PASS, scenario.status());
    }
    @Test void controlledFailureRequiresTheExactStoppedComparisonAndFailVerdicts() {
        var scenario = EventuateExpectedProfiles.expected("EXTENT", EventuateApiContractComponentTest.class, "wrong-identity").scenarios().getFirst();
        var rows = (ReportArtifactProfile.Assertions) scenario.phases().get(1).evidence().getLast();
        assertEquals(2, rows.comparisons().size());
        assertEquals("Order identity", rows.comparisons().getLast().name());
        assertEquals(AtfReportStatus.FAIL, rows.comparisons().getLast().status());
        assertEquals(AtfReportStatus.FAIL, scenario.status());
    }
    @Test void combinedSuiteRetainsEveryDeclaredScenarioAndItsOwnEvidence() {
        var profile = EventuateExpectedProfiles.expected("EXTENT", EventuateComponentSuite.class, "none");
        assertEquals(3, profile.scenarios().size());
        assertEquals(8, profile.scenarios().get(1).phases().get(1).evidence().size());
        assertEquals(1, profile.scenarios().get(2).phases().get(1).evidence().size());
    }
}
