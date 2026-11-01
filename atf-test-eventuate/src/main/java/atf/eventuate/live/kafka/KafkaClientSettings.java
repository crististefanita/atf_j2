package atf.eventuate.live.kafka;

import java.util.Properties;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

/** Fresh client-owned profiles; effective Kafka request timeout must fit the API timeout. */
public final class KafkaClientSettings {
    private KafkaClientSettings() { }
    private static Properties base(String bootstrap) {
        if(bootstrap==null || bootstrap.isBlank())throw new IllegalArgumentException("Explicit Kafka bootstrap required");
        var properties=new Properties();properties.put("bootstrap.servers",bootstrap);return properties;
    }
    public static Properties admin(String bootstrap) {
        var properties=base(bootstrap);properties.put("default.api.timeout.ms","5000");properties.put("request.timeout.ms","3000");return properties;
    }
    public static Properties observer(String bootstrap,String group) {
        if(group==null || group.isBlank())throw new IllegalArgumentException("Owned observer group required");
        var properties=admin(bootstrap);properties.put("group.id",group);properties.put("client.id",group);
        properties.put("enable.auto.commit","false");properties.put("allow.auto.create.topics","false");properties.put("auto.offset.reset","none");
        properties.put("key.deserializer",StringDeserializer.class.getName());properties.put("value.deserializer",StringDeserializer.class.getName());
        properties.put("max.poll.records","100");properties.put("max.partition.fetch.bytes","1048576");properties.put("fetch.max.bytes","2097152");return properties;
    }
    /** Synthetic infrastructure probe only; this is not an application idempotency guarantee. */
    public static Properties probeProducer(String bootstrap) {
        var properties=base(bootstrap);properties.put("key.serializer",StringSerializer.class.getName());properties.put("value.serializer",StringSerializer.class.getName());
        properties.put("acks","all");properties.put("retries","0");properties.put("enable.idempotence","false");properties.put("buffer.memory","1048576");
        properties.put("request.timeout.ms","3000");properties.put("delivery.timeout.ms","5000");properties.put("max.block.ms","5000");return properties;
    }
}
