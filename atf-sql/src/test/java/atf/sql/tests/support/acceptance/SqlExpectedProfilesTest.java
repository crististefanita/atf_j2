package atf.sql.tests.support.acceptance;

import atf.reporting.artifact.ReportArtifactProfile;
import atf.reporting.contract.AtfReportStatus;
import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
class SqlExpectedProfilesTest {
    @Test void suiteDeclaresFiveMethodsNineSqlCapturesAndNoHttp() {
        var profile = SqlExpectedProfiles.expected("FALLBACK", "suite", false);
        assertEquals(5, profile.scenarios().size());
        var evidence = profile.scenarios().stream().flatMap(scenario -> scenario.phases().stream()).flatMap(phase -> phase.evidence().stream()).toList();
        assertEquals(9, evidence.stream().filter(ReportArtifactProfile.Observation.class::isInstance).count());
        assertEquals(5, evidence.stream().filter(ReportArtifactProfile.Assertions.class::isInstance).count());
        assertTrue(evidence.stream().noneMatch(ReportArtifactProfile.HttpExchange.class::isInstance));
    }
    @Test void unhandledSqlFailureDoesNotInventAnAssertionFailure() {
        var profile = SqlExpectedProfiles.expected("EXTENT", "failures", true);
        assertEquals(1, profile.scenarios().stream().filter(scenario -> scenario.status() == AtfReportStatus.FAIL).count());
        var failed = profile.scenarios().getLast();
        assertEquals(AtfReportStatus.FAIL, failed.phases().getFirst().status());
        var rows = (ReportArtifactProfile.Assertions) failed.phases().getFirst().evidence().getLast();
        assertTrue(rows.comparisons().stream().allMatch(row -> row.status() == AtfReportStatus.PASS));
    }
}
