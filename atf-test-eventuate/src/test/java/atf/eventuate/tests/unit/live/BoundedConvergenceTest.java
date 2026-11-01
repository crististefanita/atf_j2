package atf.eventuate.tests.unit.live;

import atf.eventuate.await.OrderAwaiter;
import atf.eventuate.api.OrderObservation;
import atf.eventuate.validation.OrderContract;
import atf.eventuate.live.LiveDeadline;
import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
class BoundedConvergenceTest {
    @Test void propagatesOnlyRemainingTimeWithoutRenewingTheBudget() {
        var clock = new AtomicLong(); var received = new ArrayList<Duration>();
        var awaiter = new OrderAwaiter(clock::get, pause -> clock.addAndGet(pause.toNanos()));
        var result = awaiter.awaitBounded(remaining -> {
            received.add(remaining); clock.addAndGet(50);
            return new OrderObservation(42, received.size() == 1 ? "PENDING" : "APPROVED", null);
        }, new OrderContract(42,"APPROVED",null), Duration.ofNanos(300), Duration.ofNanos(100));
        assertTrue(result.converged()); assertEquals(java.util.List.of(Duration.ofNanos(300),Duration.ofNanos(150)),received);
        assertEquals(Duration.ofNanos(200),result.elapsed());
    }
    @Test void expiredBudgetDispatchesNoReadAndFailureKeepsItsIdentity() {
        var ticks = new AtomicLong(); var calls = new AtomicInteger();
        var awaiter = new OrderAwaiter(() -> ticks.getAndAdd(100), pause -> { });
        var result = awaiter.awaitBounded(remaining -> {calls.incrementAndGet();return new OrderObservation(42,"APPROVED",null);},
                new OrderContract(42,"APPROVED",null),Duration.ofNanos(100),Duration.ofNanos(1));
        assertFalse(result.converged()); assertEquals(0,calls.get());
        var original = new IllegalStateException("read failed");
        var stable = new OrderAwaiter(() -> 0L,pause -> { });
        assertSame(original,assertThrows(IllegalStateException.class,()->stable.awaitBounded(remaining -> {throw original;},
                new OrderContract(42,"APPROVED",null),Duration.ofSeconds(1),Duration.ofMillis(10))));
    }
    @Test void oneDeadlineCapsOperationsAndRejectsExpiredSuccess() {
        var clock = new AtomicLong(); var deadline = new LiveDeadline(Duration.ofNanos(100),clock::get);
        assertEquals(Duration.ofNanos(60),deadline.cap(Duration.ofNanos(60)));
        clock.set(70); assertEquals(Duration.ofNanos(30),deadline.cap(Duration.ofNanos(60)));
        clock.set(100); assertThrows(LiveDeadline.Expired.class,deadline::remaining);
    }
}
