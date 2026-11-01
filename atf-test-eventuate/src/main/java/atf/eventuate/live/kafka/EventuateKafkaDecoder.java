package atf.eventuate.live.kafka;

import atf.core.json.JsonDocuments;
import org.apache.kafka.clients.consumer.ConsumerRecord;

/** Decodes the pinned Eventuate wire envelope; it never invents a message or aggregate ID. */
public final class EventuateKafkaDecoder {
    private EventuateKafkaDecoder() { }
    public static KafkaEvidence.Event decode(ConsumerRecord<String, String> record) {
        if (record.value() == null || record.value().length() > 65536)
            throw new IllegalArgumentException("Kafka event is absent or exceeds the capture budget");
        try {
            var envelope = JsonDocuments.read(record.value());
            var headers = envelope.path("headers");
            String message = required(headers, "ID"), aggregate = required(headers, "event-aggregate-id");
            String type = required(headers, "event-type");
            var payload = envelope.get("payload");
            if (payload == null || !payload.isTextual()) throw new IllegalArgumentException("Eventuate payload must be text");
            JsonDocuments.read(payload.textValue());
            return new KafkaEvidence.Event(record.topic(), record.partition(), record.offset(), record.key(),
                    message, aggregate, type, payload.textValue());
        } catch (java.io.IOException failure) { throw new IllegalArgumentException("Invalid Eventuate Kafka envelope", failure); }
    }
    private static String required(com.fasterxml.jackson.databind.JsonNode node, String key) {
        var value = node.get(key);
        if (value == null || !value.isTextual() || value.textValue().isBlank())
            throw new IllegalArgumentException("Missing Eventuate header " + key);
        return value.textValue();
    }
}
