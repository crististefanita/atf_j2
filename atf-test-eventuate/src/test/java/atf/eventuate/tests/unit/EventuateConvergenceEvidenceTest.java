package atf.eventuate.tests.unit;

import atf.eventuate.api.OrderObservation;
import atf.eventuate.await.*;
import atf.eventuate.evidence.EventuateEvidence;
import atf.eventuate.tests.support.fixtures.ManualClock;
import atf.eventuate.validation.OrderContract;
import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static atf.eventuate.await.OrderConvergenceSnapshot.Termination.*;

@AtfUnitTest
class EventuateConvergenceEvidenceTest {
    private static final OrderContract EXPECTED = new OrderContract(42, "APPROVED", null);
    private static final Duration BUDGET = Duration.ofMillis(25), INTERVAL = Duration.ofMillis(10);
    private static final OrderObservation PENDING = new OrderObservation(42, "PENDING", null);

    @Test void preservesOriginalReadFailureWithOrderedImmutableHistoryAndOneCapture() {
        var clock = new ManualClock(); var reads = new AtomicInteger();
        var original = new IllegalStateException("read failed", new java.io.IOException("original cause"));
        var captured = new ArrayList<OrderConvergenceSnapshot>();
        var thrown = assertThrows(IllegalStateException.class, () -> new OrderAwaiter(clock::nanoTime, clock::advance, captured::add)
                .await(() -> { if (reads.incrementAndGet() == 2) throw original; return PENDING; }, EXPECTED, BUDGET, INTERVAL));
        assertSame(original, thrown); assertSame(original.getCause(), thrown.getCause());
        var snapshot = OrderConvergenceFailure.find(new RuntimeException(thrown)).orElseThrow();
        assertSame(snapshot, captured.getFirst()); assertEquals(1, captured.size()); assertEquals(2, reads.get());
        assertEquals(READ_FAILURE, snapshot.termination()); assertEquals(EXPECTED, snapshot.expected());
        assertEquals(Duration.ofMillis(10), snapshot.elapsed().orElseThrow()); assertEquals(BUDGET, snapshot.budget());
        assertEquals(List.of(new OrderConvergenceSnapshot.Observation(1, java.util.Optional.of(Duration.ZERO), PENDING)), snapshot.observations());
        assertEquals(PENDING, snapshot.lastObservation().orElseThrow().order());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.observations().clear());
        assertTrue(snapshot.failure().orElseThrow().originalCause().contains("original cause"));
    }
    @Test void zeroObservationReadFailureIsKnownZeroRatherThanAnInventedOrder() {
        var original = new IllegalArgumentException("first read");
        var thrown = assertThrows(IllegalArgumentException.class, () -> new OrderAwaiter(() -> 0, d -> fail("No pause"))
                .await(() -> { throw original; }, EXPECTED, BUDGET, INTERVAL));
        assertSame(original, thrown);
        var snapshot = OrderConvergenceFailure.find(thrown).orElseThrow();
        assertEquals(Duration.ZERO, snapshot.elapsed().orElseThrow()); assertTrue(snapshot.lastObservation().isEmpty());
    }
    @Test void pauseFailurePreservesInterruptionAndDoesNotReadAgain() {
        var reads = new AtomicInteger(); var clock = new ManualClock();
        var original = new IllegalStateException("interrupted pause", new InterruptedException("original interrupt"));
        try {
            var thrown = assertThrows(IllegalStateException.class, () -> new OrderAwaiter(clock::nanoTime, d -> {
                Thread.currentThread().interrupt(); throw original;
            }).await(() -> { reads.incrementAndGet(); return PENDING; }, EXPECTED, BUDGET, INTERVAL));
            assertSame(original, thrown); assertTrue(Thread.currentThread().isInterrupted());
            var snapshot = OrderConvergenceFailure.find(thrown).orElseThrow();
            assertEquals(PAUSE_FAILURE, snapshot.termination()); assertEquals(1, snapshot.observations().size());
            assertEquals(1, reads.get()); assertEquals(Duration.ZERO, snapshot.elapsed().orElseThrow());
        } finally { Thread.interrupted(); }
    }
    @Test void secondarySinkFailureDoesNotReplaceReadFailureOrReplayCapture() {
        var original = new IllegalStateException("read"); var secondary = new IllegalArgumentException("sink");
        var callbacks = new AtomicInteger();
        var thrown = assertThrows(IllegalStateException.class, () -> new OrderAwaiter(() -> 0, d -> { }, snapshot -> {
            callbacks.incrementAndGet(); throw secondary;
        }).await(() -> { throw original; }, EXPECTED, BUDGET, INTERVAL));
        assertSame(original, thrown); assertEquals(1, callbacks.get());
        assertTrue(List.of(thrown.getSuppressed()).contains(secondary)); assertTrue(OrderConvergenceFailure.find(thrown).isPresent());
    }
    @Test void successfulExecutionWithFailingSinkIsNotReexecutedOrMisclassified() {
        var original = new IllegalStateException("sink"); var callbacks = new AtomicInteger(); var reads = new AtomicInteger();
        var thrown = assertThrows(IllegalStateException.class, () -> new OrderAwaiter(() -> 0, d -> fail("No pause"), snapshot -> {
            callbacks.incrementAndGet(); throw original;
        }).await(() -> { reads.incrementAndGet(); return new OrderObservation(42, "APPROVED", null); }, EXPECTED, BUDGET, INTERVAL));
        assertSame(original, thrown); assertEquals(1, callbacks.get()); assertEquals(1, reads.get());
        assertEquals(CONVERGED, OrderConvergenceFailure.find(thrown).orElseThrow().termination());
    }
    @Test void absentClockEvidenceIsUnknownAndOriginalReadFailureStillWins() {
        var clockFailure = new IllegalStateException("clock"); var original = new IllegalArgumentException("read");
        var calls = new AtomicInteger();
        var thrown = assertThrows(IllegalArgumentException.class, () -> new OrderAwaiter(() -> {
            if (calls.incrementAndGet() == 3) throw clockFailure; return 0;
        }, d -> { }).await(() -> { throw original; }, EXPECTED, BUDGET, INTERVAL));
        assertSame(original, thrown); assertTrue(List.of(thrown.getSuppressed()).contains(clockFailure));
        assertTrue(OrderConvergenceFailure.find(thrown).orElseThrow().elapsed().isEmpty());
    }
    @Test void initialClockFailureCapturesNoFabricatedReadOrElapsed() {
        var original = new IllegalStateException("initial clock");
        var thrown = assertThrows(IllegalStateException.class, () -> new OrderAwaiter(() -> { throw original; }, d -> { })
                .await(() -> fail("No read"), EXPECTED, BUDGET, INTERVAL));
        assertSame(original, thrown); var snapshot = OrderConvergenceFailure.find(thrown).orElseThrow();
        assertEquals(CLOCK_FAILURE, snapshot.termination()); assertTrue(snapshot.elapsed().isEmpty());
        assertTrue(snapshot.observations().isEmpty());
    }
    @Test void clockFailureAfterACompletedReadKeepsThatObservationWithUnknownTime() {
        var calls = new AtomicInteger(); var original = new IllegalStateException("clock after read");
        var thrown = assertThrows(IllegalStateException.class, () -> new OrderAwaiter(() -> {
            if (calls.incrementAndGet() == 3) throw original; return 0;
        }, d -> fail("No pause")).await(() -> PENDING, EXPECTED, BUDGET, INTERVAL));
        assertSame(original, thrown); var snapshot = OrderConvergenceFailure.find(thrown).orElseThrow();
        assertEquals(CLOCK_FAILURE, snapshot.termination()); assertTrue(snapshot.elapsed().isEmpty());
        assertEquals(1, snapshot.observations().size()); assertEquals(PENDING, snapshot.lastObservation().orElseThrow().order());
        assertTrue(snapshot.lastObservation().orElseThrow().elapsed().isEmpty());
    }
    @Test void expiryBeforeFirstReadHasSafeOptionalAndOneSnapshot() {
        var calls = new AtomicInteger(); var snapshots = new ArrayList<OrderConvergenceSnapshot>();
        var result = new OrderAwaiter(() -> calls.getAndIncrement() == 0 ? 0 : BUDGET.toNanos(), d -> fail("No pause"), snapshots::add)
                .await(() -> fail("No read"), EXPECTED, BUDGET, INTERVAL);
        assertFalse(result.converged()); assertTrue(result.lastObservedOrder().isEmpty());
        assertThrows(java.util.NoSuchElementException.class, result::last);
        assertEquals(1, snapshots.size()); assertEquals(DEADLINE_REACHED, snapshots.getFirst().termination());
        assertTrue(snapshots.getFirst().lastObservation().isEmpty());
    }
    @Test void lateCorrectReadKeepsHistoryAndCannotTurnDeadlineIntoPass() {
        var clock = new ManualClock(); var snapshots = new ArrayList<OrderConvergenceSnapshot>();
        var result = new OrderAwaiter(clock::nanoTime, clock::advance, snapshots::add).await(() -> {
            clock.advance(Duration.ofMillis(40)); return new OrderObservation(42, "APPROVED", null);
        }, EXPECTED, BUDGET, INTERVAL);
        assertFalse(result.converged()); assertEquals(DEADLINE_REACHED, snapshots.getFirst().termination());
        assertEquals(Duration.ofMillis(40), snapshots.getFirst().elapsed().orElseThrow());
        assertEquals(result.last(), snapshots.getFirst().lastObservation().orElseThrow().order());
        assertEquals(result.elapsed(), snapshots.getFirst().lastObservation().orElseThrow().elapsed().orElseThrow());
    }
    @Test void collectorsKeepPhaseAndInvocationIsolationEvenWhenSuppressionIsDisabled() {
        var evidence = new EventuateEvidence();
        var original = new QuietFailure();
        try (var phase = evidence.inPhase("SETUP")) {
            assertSame(original, assertThrows(QuietFailure.class, () -> new OrderAwaiter(() -> 0, d -> { }, evidence::recordConvergence)
                    .await(() -> { throw original; }, EXPECTED, BUDGET, INTERVAL)));
        }
        assertEquals(0, original.getSuppressed().length); assertEquals(1, evidence.convergences("SETUP").size());
        assertTrue(evidence.convergences("TEST BODY").isEmpty()); assertTrue(evidence.entries().isEmpty());
        assertTrue(new EventuateEvidence().convergences().isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> evidence.convergences().clear());
    }
    @Test void metadataLookupHandlesCyclesAndSelfSuppressionWithoutLosingTheSnapshot() {
        var original = new IllegalStateException("cycle"); var cause = new RuntimeException(original); original.initCause(cause);
        var thrown = assertThrows(IllegalStateException.class, () -> new OrderAwaiter(() -> 0, d -> { }, snapshot -> { throw original; })
                .await(() -> { throw original; }, EXPECTED, BUDGET, INTERVAL));
        assertSame(original, thrown); assertEquals(1, thrown.getSuppressed().length);
        assertTrue(OrderConvergenceFailure.find(cause).isPresent()); assertTrue(OrderConvergenceFailure.find(null).isEmpty());
        assertTrue(OrderConvergenceFailure.find(new RuntimeException()).isEmpty());
    }
    @Test void preflightFailureDoesNotPretendThatExecutionOrCaptureStarted() {
        var capture = new AtomicReference<OrderConvergenceSnapshot>();
        var awaiter = new OrderAwaiter(() -> fail("No clock"), d -> fail("No pause"), capture::set);
        assertThrows(IllegalArgumentException.class, () -> awaiter.await(() -> PENDING, EXPECTED, Duration.ZERO, INTERVAL));
        assertThrows(ArithmeticException.class, () -> awaiter.await(() -> PENDING, EXPECTED, Duration.ofSeconds(Long.MAX_VALUE), INTERVAL));
        assertNull(capture.get());
    }
    @Test void executionErrorKeepsItsIdentityAndOriginalCause() {
        var cause = new IllegalArgumentException("error cause"); var original = new AssertionError("read error", cause);
        var reads = new AtomicInteger();
        var thrown = assertThrows(AssertionError.class, () -> new OrderAwaiter(() -> 0, d -> fail("No pause"))
                .await(() -> { reads.incrementAndGet(); throw original; }, EXPECTED, BUDGET, INTERVAL));
        assertSame(original, thrown); assertSame(cause, thrown.getCause()); assertEquals(1, reads.get());
        assertEquals(READ_FAILURE, OrderConvergenceFailure.find(thrown).orElseThrow().termination());
    }
    @Test void successiveInvocationsNeverShareHistoryAndLookupFindsTheLatestSnapshot() {
        var clock = new ManualClock(); var snapshots = new ArrayList<OrderConvergenceSnapshot>();
        var original = new IllegalStateException("reused failure");
        var awaiter = new OrderAwaiter(clock::nanoTime, clock::advance, snapshots::add);
        assertSame(original, assertThrows(IllegalStateException.class,
                () -> awaiter.await(() -> { throw original; }, EXPECTED, BUDGET, INTERVAL)));
        var reads = new AtomicInteger();
        assertSame(original, assertThrows(IllegalStateException.class, () -> awaiter.await(() -> {
            if (reads.incrementAndGet() == 2) throw original; return PENDING;
        }, EXPECTED, BUDGET, INTERVAL)));
        assertEquals(2, snapshots.size()); assertTrue(snapshots.getFirst().observations().isEmpty());
        assertEquals(1, snapshots.getLast().observations().size());
        assertSame(snapshots.getLast(), OrderConvergenceFailure.find(original).orElseThrow());
    }
    private static final class QuietFailure extends RuntimeException {
        private QuietFailure() { super("suppression disabled", null, false, true); }
    }
}
