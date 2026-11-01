package atf.eventuate.live;

import atf.eventuate.api.OrderObservation;
import atf.eventuate.await.OrderAwaiter;
import atf.eventuate.evidence.EventuateEvidence;
import atf.eventuate.live.kafka.KafkaObserver;
import atf.eventuate.live.kafka.KafkaEvidence;
import atf.eventuate.live.sql.LiveSqlSnapshot;
import atf.eventuate.live.sql.LiveSqlWitness;
import atf.eventuate.validation.OrderContract;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

/** Real application orchestration only; fixed expectations and assertions remain in the test. */
public final class LiveOrderScenario {
    public enum Case { APPROVED, INSUFFICIENT_CREDIT, UNKNOWN_CUSTOMER }
    public record Result(long orderId, long customerId, OrderObservation terminal, LiveSqlSnapshot sql,
                         KafkaEvidence kafka, int customerPosts, int orderPosts) { }
    private final LiveSettings settings;
    public LiveOrderScenario(LiveSettings settings) { this.settings = settings; }
    public Result execute(Case selected, EventuateEvidence evidence) throws java.sql.SQLException {
        var deadline = new LiveDeadline(settings.runBudget());
        try (var http = new LiveHttpClient(settings, evidence::record);
             var observer = new KafkaObserver(settings.bootstrap());
             var sql = LiveSqlWitness.mysql(settings, evidence::recordLiveSql)) {
            Throwable primary = null;
            try {
                observer.prepare(List.of(LiveSettings.ORDER_TOPIC, LiveSettings.CUSTOMER_TOPIC), deadline);
                long customerId = selected == Case.UNKNOWN_CUSTOMER ? Long.MAX_VALUE
                        : http.createCustomer("ATF-LIVE-" + UUID.randomUUID(), selected == Case.INSUFFICIENT_CREDIT ? "1.00" : "100.00", deadline.remaining());
                long orderId = http.createOrder(customerId, "12.34", deadline.remaining());
                String state = selected == Case.APPROVED ? "APPROVED" : "REJECTED";
                String reason = selected == Case.APPROVED ? null : selected.name();
                var expected = new OrderContract(orderId, state, reason);
                var awaiter = new OrderAwaiter(System::nanoTime, LiveOrderScenario::pause, evidence::recordConvergence);
                var result = awaiter.awaitBounded(remaining -> http.observe(orderId, remaining), expected,
                        deadline.remaining(), Duration.ofMillis(100));
                if (!result.converged()) throw new AssertionError("LIVE order did not converge before the original deadline");
                String eventType = selected == Case.APPROVED ? "OrderApprovedEvent" : "OrderRejectedEvent";
                observer.await(events -> events.stream().anyMatch(event -> event.aggregateId().equals(Long.toString(orderId))
                        && event.eventType().endsWith("." + eventType)), deadline);
                var witness = sql.read(orderId, customerId, deadline.cap(settings.readTimeout()));
                return new Result(orderId, customerId, result.last(), witness, observer.snapshot(), http.customerMutations(), http.orderMutations());
            } catch (java.sql.SQLException | RuntimeException | Error failure) { primary = failure; throw failure; }
            finally {
                try { evidence.recordKafka(observer.snapshot()); }
                catch (RuntimeException | Error secondary) { if (primary == null) throw secondary; if (secondary != primary) primary.addSuppressed(secondary); }
            }
        }
    }
    private static void pause(Duration duration) {
        try { Thread.sleep(duration); }
        catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new IllegalStateException("LIVE convergence interrupted", interrupted); }
    }
}
