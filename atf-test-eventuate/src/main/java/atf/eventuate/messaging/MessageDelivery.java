package atf.eventuate.messaging;
import java.util.Objects;
/** Simulated delivery identity; position is a fixture ordinal, never a Kafka offset. */
public record MessageDelivery(String messageId, long orderId, String eventType, int ordinal) {
    public MessageDelivery {
        Objects.requireNonNull(messageId); Objects.requireNonNull(eventType);
        if (messageId.isBlank() || ordinal < 1) throw new IllegalArgumentException("Message ID and positive fixture ordinal required");
    }
    public String provenance() { return "MOCK_EVENT_FEED"; }
}
