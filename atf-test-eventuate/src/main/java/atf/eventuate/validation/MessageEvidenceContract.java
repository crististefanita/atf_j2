package atf.eventuate.validation;
import atf.eventuate.messaging.MessageDelivery;
import atf.eventuate.messaging.OrderEffects;
import java.math.BigDecimal;
import java.util.List;

/** Checks evidence completeness and effects; never implements the application's deduplication. */
public record MessageEvidenceContract(String messageId, long orderId, BigDecimal amount) {
    public record Result(boolean complete, boolean singleEffect, boolean converged) {
        public boolean passed() { return complete && singleEffect && converged; }
    }
    public Result evaluate(List<MessageDelivery> deliveries, OrderEffects effects) {
        boolean complete = deliveries.size() >= 2 && deliveries.stream().allMatch(delivery ->
                delivery.messageId().equals(messageId) && delivery.orderId() == orderId
                        && delivery.eventType().equals("OrderCreated") && delivery.provenance().equals("MOCK_EVENT_FEED"));
        complete &= deliveries.stream().map(MessageDelivery::ordinal).distinct().count() == deliveries.size();
        boolean single = effects.orderId() == orderId && effects.reservations() == 1 && effects.reservedCredit().compareTo(amount) == 0;
        boolean converged = "APPROVED".equals(effects.state()) && effects.state().equals(effects.projectionState());
        return new Result(complete, single, converged);
    }
}
