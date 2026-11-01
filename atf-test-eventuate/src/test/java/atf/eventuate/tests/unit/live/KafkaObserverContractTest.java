package atf.eventuate.tests.unit.live;

import atf.eventuate.live.LiveDeadline;
import atf.eventuate.live.kafka.KafkaObserver;
import atf.eventuate.live.kafka.EventuateKafkaDecoder;
import atf.eventuate.live.kafka.KafkaClientSettings;
import atf.testkit.AtfUnitTest;
import org.apache.kafka.clients.consumer.MockConsumer;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.Node;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
class KafkaObserverContractTest {
    @Test void effectiveKafkaClientBudgetsRemainValidWithoutNetworkAccess() {
        var admin=new org.apache.kafka.clients.admin.AdminClientConfig(KafkaClientSettings.admin("unused:9092"));
        assertTrue(admin.getInt("default.api.timeout.ms")>=admin.getInt("request.timeout.ms"));
        var consumer=new org.apache.kafka.clients.consumer.ConsumerConfig(KafkaClientSettings.observer("unused:9092","owned"));
        assertFalse(consumer.getBoolean("enable.auto.commit"));assertTrue(consumer.getInt("default.api.timeout.ms")>=consumer.getInt("request.timeout.ms"));
        var producer=new org.apache.kafka.clients.producer.ProducerConfig(KafkaClientSettings.probeProducer("unused:9092"));
        assertEquals(0,producer.getInt("retries"));assertEquals(1048576L,producer.getLong("buffer.memory"));
    }
    private static final String EVENT = "{\"headers\":{\"ID\":\"message-1\",\"event-aggregate-id\":\"42\",\"event-type\":\"example.OrderApprovedEvent\"},\"payload\":\"{}\"}";
    @Test void preparesAtBrokerEndAndNeverCommitsApplicationOffsets() {
        var mock = new MockConsumer<String,String>(OffsetResetStrategy.NONE);
        var partition = new TopicPartition("orders",0);
        mock.updatePartitions("orders",List.of(new PartitionInfo("orders",0,null,new Node[0],new Node[0])));
        mock.updateEndOffsets(Map.of(partition,7L));
        try(var observer = new KafkaObserver("unused:9092",properties -> {
            assertEquals("false",properties.getProperty("enable.auto.commit"));
            assertEquals("false",properties.getProperty("allow.auto.create.topics")); return mock;
        })) {
            observer.prepare(List.of("orders"),new LiveDeadline(Duration.ofSeconds(2)));
            assertEquals(7L,observer.snapshot().baseline().getFirst().offset());
            mock.schedulePollTask(() -> mock.addRecord(new ConsumerRecord<>("orders",0,7,"42",EVENT)));
            observer.await(events -> events.size()==1,new LiveDeadline(Duration.ofSeconds(2)));
            assertEquals("message-1",observer.snapshot().events().getFirst().messageId());
            assertEquals("42",observer.snapshot().events().getFirst().aggregateId());
            assertTrue(mock.committed(java.util.Set.of(partition)).values().stream().allMatch(java.util.Objects::isNull));
            var ticks = new AtomicLong(); var expired = new LiveDeadline(Duration.ofNanos(1),ticks::get); ticks.set(1);
            assertThrows(LiveDeadline.Expired.class,()->observer.await(events -> true,expired));
        }
    }
    @Test void rejectsMissingIdentityAndReadinessWithoutTopics() {
        assertThrows(IllegalArgumentException.class,()->EventuateKafkaDecoder.decode(new ConsumerRecord<>("orders",0,1,"42","{\"headers\":{},\"payload\":\"{}\"}")));
        var mock = new MockConsumer<String,String>(OffsetResetStrategy.NONE);
        try(var observer = new KafkaObserver("unused:9092",properties -> mock)) {
            assertThrows(IllegalArgumentException.class,()->observer.prepare(List.of(),new LiveDeadline(Duration.ofSeconds(1))));
            assertFalse(observer.snapshot().readyBeforeMutation());
            assertThrows(IllegalStateException.class,()->observer.await(events -> true,new LiveDeadline(Duration.ofSeconds(1))));
        }
    }
}
