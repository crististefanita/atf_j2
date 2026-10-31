package atf.eventuate.tests.component;
import atf.eventuate.api.*;
import atf.eventuate.evidence.EventuateEvidence;
import atf.eventuate.mock.ScriptedApiTransport;
import atf.eventuate.validation.OrderContract;
import atf.eventuate.tests.support.EventuateComponentTest;
import atf.eventuate.tests.support.fixtures.EventuateFixtures;
import atf.testkit.assertions.RecordedAssertions;
import org.junit.jupiter.api.Test;
import java.util.List;
import static atf.eventuate.tests.support.fixtures.EventuateFixtures.route;

@EventuateComponentTest
public class EventuateApiContractComponentTest extends atf.eventuate.tests.support.EventuateComponentSupport {
    @Test @org.junit.jupiter.api.DisplayName("API contract · approved order")
    void validatesObservedOrderAgainstIndependentContract(RecordedAssertions checks, EventuateEvidence evidence) throws java.io.IOException {
        try (var transport = new ScriptedApiTransport(List.of(route("GET", "/orders/42", 200, EventuateFixtures.order("APPROVED"))))) {
            var observed = new EventuateApiClient(transport, evidence::record).observe(42);
            checks.equals("Execution provenance", "ACCEPTANCE_MOCK", evidence.exchanges().getFirst().provenance().name());
            checks.equals("Order identity", 42L, observed.orderId());
            checks.equals("Order state", "APPROVED", observed.state());
            String synthetic = new com.fasterxml.jackson.databind.ObjectMapper().readTree(evidence.exchanges().getFirst().response().body()).path("auditDescription").asText();
            checks.equals("Full synthetic audit value before display preview", EventuateFixtures.DESCRIPTION, synthetic);
            checks.isTrue("Independent complete order contract", new OrderContract(42, "APPROVED", null).matches(observed));
            checks.isFalse("Negative control rejects a different order", new OrderContract(99, "APPROVED", null).matches(observed));
            checks.equals("All expected exchanges consumed", 0, transport.remaining());
        }
    }
}
