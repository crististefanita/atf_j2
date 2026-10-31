package atf.eventuate.tests.unit;

import atf.eventuate.api.EventuateApiClient;
import atf.eventuate.api.OrderObservation;
import atf.eventuate.await.OrderAwaiter;
import atf.eventuate.mock.ScriptedApiTransport;
import atf.eventuate.tests.support.fixtures.ManualClock;
import atf.eventuate.validation.OrderContract;
import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import static atf.eventuate.tests.support.fixtures.EventuateFixtures.route;
import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
class EventuateOrderContractTest {
    @ParameterizedTest @ValueSource(strings = {"9223372036854775808", "-9223372036854775809", "18446744073709551658", "42.0", "\"42\"", "null"})
    void rejectsInvalidIdentityBeforeItCanAliasAValidOrder(String id) {
        try (var transport = new ScriptedApiTransport(List.of(route("GET", "/orders/42", 200, body(id))))) {
            var exchanges = new AtomicInteger();
            var failure = assertThrows(IllegalArgumentException.class,
                    () -> new EventuateApiClient(transport, result -> exchanges.incrementAndGet()).observe(42));
            assertTrue(failure.getMessage().contains("signed long range"));
            assertEquals(1, transport.requests().size());
            assertEquals(1, exchanges.get(), "Decoded failures retain the successful HTTP evidence");
        }
    }
    @ParameterizedTest @ValueSource(longs = {Long.MIN_VALUE, -1, 0, 42, Long.MAX_VALUE})
    void preservesEverySupportedLongIdentityWithoutNarrowing(long id) {
        try (var transport = new ScriptedApiTransport(List.of(route("GET", "/orders/42", 200, body(Long.toString(id)))))) {
            assertEquals(id, new EventuateApiClient(transport, result -> { }).observe(42).orderId());
        }
    }
    @ParameterizedTest @ValueSource(strings = {
            "{\"orderId\":42,\"orderState\":\"APPROVED\",\"rejectionReason\":null} trailing",
            "{\"orderId\":99,\"orderId\":42,\"orderState\":\"APPROVED\",\"rejectionReason\":null}",
            "{\"orderId\":42,\"orderState\":\"APPROVED\"}",
            "{\"orderId\":42,\"orderState\":null,\"rejectionReason\":null}", "null", "[]"})
    void rejectsIncompleteOrAmbiguousOrderDocuments(String body) {
        try (var transport = new ScriptedApiTransport(List.of(route("GET", "/orders/42", 200, body)))) {
            assertThrows(IllegalArgumentException.class, () -> new EventuateApiClient(transport, result -> { }).observe(42));
        }
    }
    @Test void observesFullIndependentContractAndPreservesRejectedHistory() {
        var clock = new ManualClock();
        var index = new AtomicInteger();
        var orders = List.of(new OrderObservation(99, "APPROVED", null), new OrderObservation(42, "APPROVED", "stale"),
                new OrderObservation(42, "PENDING", null), new OrderObservation(42, "APPROVED", null));
        var result = new OrderAwaiter(clock::nanoTime, clock::advance).await(() -> orders.get(index.getAndIncrement()),
                new OrderContract(42, "APPROVED", null), Duration.ofMillis(40), Duration.ofMillis(10));
        assertTrue(result.converged());
        assertEquals(4, result.observations().size());
        assertEquals(orders, result.observations().stream().map(OrderAwaiter.Observation::order).toList());
        assertEquals(Duration.ofMillis(30), result.elapsed());
        assertEquals(42, result.last().orderId());
    }
    @Test void wrongIdentityAndWrongReasonNeverConvergeAndLateCorrectResultStillExpires() {
        for (var observed : List.of(new OrderObservation(99, "APPROVED", null), new OrderObservation(42, "APPROVED", "old"))) {
            var clock = new ManualClock();
            var result = new OrderAwaiter(clock::nanoTime, clock::advance).await(() -> observed,
                    new OrderContract(42, "APPROVED", null), Duration.ofMillis(20), Duration.ofMillis(10));
            assertFalse(result.converged());
            assertEquals(2, result.observations().size());
            assertEquals(Duration.ofMillis(20), result.elapsed());
        }
        var clock = new ManualClock();
        var late = new OrderAwaiter(clock::nanoTime, clock::advance).await(() -> {
            clock.advance(Duration.ofMillis(40));
            return new OrderObservation(42, "APPROVED", null);
        }, new OrderContract(42, "APPROVED", null), Duration.ofMillis(25), Duration.ofMillis(10));
        assertFalse(late.converged());
        assertEquals(Duration.ofMillis(40), late.elapsed());
        assertEquals(1, late.observations().size());
        assertFalse(new OrderContract(42, "APPROVED", null).matches(null));
    }
    private static String body(String id) { return "{\"orderId\":" + id + ",\"orderState\":\"APPROVED\",\"rejectionReason\":null}"; }
}
