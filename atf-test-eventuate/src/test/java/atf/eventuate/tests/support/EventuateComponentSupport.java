package atf.eventuate.tests.support;

import atf.eventuate.evidence.EventuateEvidence;
import atf.testkit.assertions.RecordedAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import java.nio.file.Files;
import java.nio.file.Path;

/** Consumer preconditions and postconditions, evaluated in their own audit phases. */
public abstract class EventuateComponentSupport {
    @BeforeEach void reportingBoundary(RecordedAssertions checks) throws Exception {
        Path source = Path.of(atf.reporting.configuration.ReportingSettings.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI());
        checks.isTrue("Reporting loaded from a binary artifact", Files.isRegularFile(source));
    }

    @AfterEach void offlineBoundary(RecordedAssertions checks, EventuateEvidence evidence) {
        checks.isTrue("No captured HTTP exchange executed a network endpoint",
                evidence.entries().stream().noneMatch(exchange -> exchange.provenance().networkExecuted()));
    }
}
