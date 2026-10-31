package atf.eventuate.tests.component;
import atf.eventuate.messaging.*;
import atf.eventuate.validation.MessageEvidenceContract;
import atf.eventuate.tests.support.EventuateComponentTest;
import atf.eventuate.tests.support.fixtures.EventuateFixtures;
import atf.testkit.assertions.RecordedAssertions;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;

@EventuateComponentTest
public class EventuateMessageEvidenceComponentTest extends atf.eventuate.tests.support.EventuateComponentSupport {
    @Test @org.junit.jupiter.api.DisplayName("Message evidence · duplicate witness")
    void auditsDuplicateDeliveryWitnessWithoutClaimingRealBrokerProcessing(RecordedAssertions checks) {
        var contract = new MessageEvidenceContract("evt-42", 42, new BigDecimal("70.00"));
        var deliveries = EventuateFixtures.deliveries();
        var effects = EventuateFixtures.effects();
        var verdict = contract.evaluate(deliveries, effects);
        checks.equals("Evidence provenance", "MOCK_EVENT_FEED", deliveries.getFirst().provenance());
        checks.equals("Observed fixture deliveries", 2, deliveries.size());
        checks.isTrue("Same message and order identity", verdict.complete());
        checks.equals("Independent reservation count", 1, effects.reservations());
        checks.equals("Independent reserved credit", new BigDecimal("70.00"), effects.reservedCredit());
        checks.isTrue("Single business effect witness", verdict.singleEffect());
        checks.isTrue("Business and projection states converge", verdict.converged());
        checks.isFalse("Negative control rejects duplicate effects", contract.evaluate(deliveries,
                new OrderEffects(42, 2, new BigDecimal("140.00"), "APPROVED", "APPROVED")).passed());
        checks.isFalse("Negative control rejects missing delivery", contract.evaluate(List.of(deliveries.getFirst()), effects).passed());
        checks.isFalse("Negative control rejects a stale projection", contract.evaluate(deliveries,
                new OrderEffects(42, 1, new BigDecimal("70.00"), "APPROVED", "PENDING")).passed());
    }
}
