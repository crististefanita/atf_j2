package atf.eventuate.live.kafka;

import atf.eventuate.live.LiveDeadline;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

/** Thread-confined, read-only observer; manual assignment never changes application group offsets. */
public final class KafkaObserver implements AutoCloseable {
    private final Consumer<String, String> consumer;
    private final String group = "atf-j2-observer-" + UUID.randomUUID();
    private final List<KafkaEvidence.Position> baseline = new ArrayList<>();
    private final List<KafkaEvidence.Event> events = new ArrayList<>();
    private final Thread owner = Thread.currentThread();
    private boolean prepared, closed;
    public KafkaObserver(String bootstrap) { this(bootstrap, KafkaConsumer::new); }
    public KafkaObserver(String bootstrap, Function<Properties, Consumer<String, String>> factory) {
        var properties = KafkaClientSettings.observer(bootstrap, group);
        consumer = factory.apply(properties);
    }
    public void prepare(List<String> topics, LiveDeadline deadline) {
        requireOwner();
        if (prepared) throw new IllegalStateException("Observer already prepared");
        if (topics.isEmpty() || topics.stream().anyMatch(String::isBlank) || topics.stream().distinct().count() != topics.size())
            throw new IllegalArgumentException("Distinct, nonempty observer topics required");
        var partitions = new ArrayList<TopicPartition>();
        for (String topic : topics) {
            var metadata = consumer.partitionsFor(topic, deadline.cap(Duration.ofSeconds(5)));
            if (metadata == null || metadata.isEmpty()) throw new IllegalStateException("Observer topic not ready: " + topic);
            metadata.forEach(value -> partitions.add(new TopicPartition(topic, value.partition())));
        }
        consumer.assign(partitions);
        var offsets = consumer.endOffsets(partitions, deadline.cap(Duration.ofSeconds(5)));
        for (var partition : partitions) {
            Long offset = offsets.get(partition);
            if (offset == null) throw new IllegalStateException("Missing observer baseline");
            consumer.seek(partition, offset);
            baseline.add(new KafkaEvidence.Position(partition.topic(), partition.partition(), offset));
        }
        deadline.remaining(); prepared = true;
    }
    public void await(Predicate<List<KafkaEvidence.Event>> condition, LiveDeadline deadline) {
        requireOwner();
        if (!prepared) throw new IllegalStateException("Observer must be ready before mutation");
        while (true) {
            deadline.remaining();
            if (condition.test(List.copyOf(events))) return;
            var records = consumer.poll(deadline.cap(Duration.ofMillis(100)));
            for (var record : records) {
                if (events.size() >= 1000) throw new IllegalStateException("Kafka observer capture budget exhausted");
                events.add(EventuateKafkaDecoder.decode(record));
            }
        }
    }
    public KafkaEvidence snapshot() { requireOwner(); return new KafkaEvidence(group, prepared, baseline, events); }
    private void requireOwner() {
        if (closed || Thread.currentThread() != owner) throw new IllegalStateException("Observer is closed or used concurrently");
    }
    @Override public void close() { if (!closed) { requireOwner(); closed = true; consumer.close(Duration.ofSeconds(1)); } }
}
