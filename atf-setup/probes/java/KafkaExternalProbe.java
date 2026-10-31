import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/** Explicit infrastructure probe, launched from Windows with the pinned image's Kafka client JARs. */
public final class KafkaExternalProbe {
    private record Inputs(String bootstrap, String expectedHost, int port, String probeId, Path output) {
        String topic() { return "atf-j2-external-" + probeId; }
        String group() { return topic(); }
        String payload() { return "{\"probeId\":\"" + probeId + "\",\"source\":\"REAL_KAFKA_EXTERNAL\"}"; }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 5 || !args[3].matches("[a-f0-9]{32}")) throw new IllegalArgumentException("Expected bootstrap, host, port, probeId and output.");
        var inputs = new Inputs(args[0], args[1], Integer.parseInt(args[2]), args[3], Path.of(args[4]));
        var receipt = new LinkedHashMap<String, Object>();
        receipt.put("source", "REAL_KAFKA_EXTERNAL"); receipt.put("status", "RUNNING");
        receipt.put("bootstrapServers", inputs.bootstrap()); receipt.put("probeId", inputs.probeId());
        receipt.put("topic", inputs.topic()); receipt.put("group", inputs.group());
        receipt.put("topicCreated", false); receipt.put("producerAcknowledged", false); receipt.put("consumerMatched", false);
        int exit = 1;
        try {
            run(inputs, receipt);
            receipt.put("status", "PASS"); exit = 0;
        } catch (Exception failure) {
            receipt.put("status", "FAIL"); receipt.put("failureType", failure.getClass().getName());
            receipt.put("failure", failure.toString());
        } finally {
            Files.writeString(inputs.output(), json(receipt));
        }
        System.exit(exit);
    }

    private static void run(Inputs inputs, Map<String, Object> receipt) throws Exception {
        var adminProperties = common(inputs);
        adminProperties.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, "10000");
        adminProperties.put(AdminClientConfig.RETRIES_CONFIG, "0");
        Admin admin = Admin.create(adminProperties);
        try {
            var nodes = List.copyOf(admin.describeCluster().nodes().get(10, TimeUnit.SECONDS));
            if (nodes.size() != 1) throw new IllegalStateException("Expected one infrastructure broker; observed " + nodes);
            Node broker = nodes.getFirst();
            receipt.put("advertisedHost", broker.host()); receipt.put("advertisedPort", broker.port());
            if (!inputs.expectedHost().equals(broker.host()) || inputs.port() != broker.port()) {
                throw new IllegalStateException("Advertised listener differs: " + broker.host() + ":" + broker.port());
            }
            var topic = new NewTopic(inputs.topic(), 1, (short) 1).configs(Map.of("retention.ms", "3600000"));
            admin.createTopics(List.of(topic)).all().get(10, TimeUnit.SECONDS);
            receipt.put("topicCreated", true);
        } finally { admin.close(Duration.ofSeconds(1)); }
        RecordMetadata acknowledged = publish(inputs);
        receipt.put("producerAcknowledged", true); receipt.put("partition", acknowledged.partition()); receipt.put("offset", acknowledged.offset());
        consume(inputs, acknowledged);
        receipt.put("consumerMatched", true); receipt.put("payload", inputs.payload());
    }

    private static Properties common(Inputs inputs) {
        var properties = new Properties();
        properties.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, inputs.bootstrap());
        properties.put(CommonClientConfigs.CLIENT_ID_CONFIG, "atf-j2-probe-" + inputs.probeId());
        properties.put(CommonClientConfigs.REQUEST_TIMEOUT_MS_CONFIG, "5000");
        properties.put(CommonClientConfigs.SOCKET_CONNECTION_SETUP_TIMEOUT_MS_CONFIG, "3000");
        properties.put(CommonClientConfigs.SOCKET_CONNECTION_SETUP_TIMEOUT_MAX_MS_CONFIG, "3000");
        return properties;
    }

    private static RecordMetadata publish(Inputs inputs) throws Exception {
        var properties = common(inputs);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.ACKS_CONFIG, "all"); properties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
        properties.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, "10000"); properties.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, "10000");
        properties.put(ProducerConfig.LINGER_MS_CONFIG, "0"); properties.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "none");
        var producer = new KafkaProducer<String, String>(properties);
        try { return producer.send(new ProducerRecord<>(inputs.topic(), inputs.probeId(), inputs.payload())).get(10, TimeUnit.SECONDS); }
        finally { producer.close(Duration.ofSeconds(1)); }
    }

    private static void consume(Inputs inputs, RecordMetadata acknowledged) {
        var properties = common(inputs);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, inputs.group()); properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false"); properties.put(ConsumerConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, "10000");
        var consumer = new KafkaConsumer<String, String>(properties);
        try {
            consumer.subscribe(List.of(inputs.topic()));
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            while (System.nanoTime() < deadline) {
                for (var record : consumer.poll(Duration.ofMillis(200))) {
                    if (!inputs.probeId().equals(record.key()) || !inputs.payload().equals(record.value()) ||
                            record.partition() != acknowledged.partition() || record.offset() != acknowledged.offset()) {
                        throw new IllegalStateException("Consumer identity or acknowledged position differs.");
                    }
                    return;
                }
            }
            throw new IllegalStateException("No correlated delivery within the consumer budget.");
        } finally { consumer.close(Duration.ofSeconds(1)); }
    }

    private static String json(Map<String, Object> fields) {
        return fields.entrySet().stream().map(entry -> quote(entry.getKey()) + ":" +
                (entry.getValue() instanceof String text ? quote(text) : entry.getValue().toString()))
                .collect(java.util.stream.Collectors.joining(",", "{", "}"));
    }

    private static String quote(String text) {
        var result = new StringBuilder("\"");
        for (char character : text.toCharArray()) {
            switch (character) {
                case '"' -> result.append("\\\"");
                case '\\' -> result.append("\\\\");
                case '\n' -> result.append("\\n");
                case '\r' -> result.append("\\r");
                case '\t' -> result.append("\\t");
                default -> { if (character < 32) result.append(String.format("\\u%04x", (int) character)); else result.append(character); }
            }
        }
        return result.append('"').toString();
    }
}
