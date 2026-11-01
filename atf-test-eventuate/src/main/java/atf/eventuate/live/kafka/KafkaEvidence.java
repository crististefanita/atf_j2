package atf.eventuate.live.kafka;

import java.util.List;
import java.util.Objects;

/** Broker coordinates are captured facts, distinct from MOCK_EVENT_FEED ordinals. */
public record KafkaEvidence(String groupId, boolean readyBeforeMutation, List<Position> baseline,
        List<Event> events) {
    public record Position(String topic, int partition, long offset) { }
    public record Event(String topic, int partition, long offset, String key, String messageId,
                        String aggregateId, String eventType, String payload) {
        public Event { Objects.requireNonNull(messageId); Objects.requireNonNull(aggregateId); Objects.requireNonNull(eventType); }
    }
    public KafkaEvidence { baseline = List.copyOf(baseline); events = List.copyOf(events); }
}
