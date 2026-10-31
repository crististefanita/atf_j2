package atf.eventuate.tests.unit;

import atf.core.http.execution.evidence.ApiExecutionFailure;
import atf.core.http.response.ApiAttemptOutcome;
import atf.eventuate.api.EventuateApiClient;
import atf.eventuate.evidence.EventuateEvidence;
import atf.eventuate.evidence.EventuateExchange;
import atf.eventuate.mock.ScriptedApiTransport;
import atf.eventuate.tests.support.fixtures.TerminalFailureFixtures;
import atf.reporting.api.ReportModelAdapters;
import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
class EventuateTerminalEvidenceTest {
    @ParameterizedTest @ValueSource(strings = {"exhausted", "transport", "timeout", "cancel", "retry-transport"})
    void capturesTheOriginalTerminalSnapshotOnceWithoutAResponse(String name) {
        var evidence = new EventuateEvidence();
        var fixture = TerminalFailureFixtures.create(name, evidence::record);
        var original = assertThrows(RuntimeException.class, () -> fixture.client().observe(42));
        var captured = assertInstanceOf(EventuateExchange.Terminal.class, evidence.entries().getFirst());
        assertSame(original, captured.failure());
        assertSame(ApiExecutionFailure.find(original).orElseThrow(), captured.snapshot());
        assertEquals(1, evidence.entries().size());
        assertTrue(evidence.exchanges().isEmpty());
        assertEquals(fixture.outcome(), captured.snapshot().termination());
        assertEquals(fixture.attempts(), captured.snapshot().attempts());
        assertEquals(fixture.attempts(), fixture.dispatches().get());
        assertEquals("/orders/42", captured.request().path());
        assertEquals("ACCEPTANCE_MOCK", captured.provenance().name());
        assertEquals("ACCEPTANCE_MOCK", evidence.source());
        assertTrue(captured.response().isEmpty());
        assertFalse(captured.snapshot().totalDuration().isNegative());
        assertThrows(UnsupportedOperationException.class, () -> evidence.entries().clear());
        if (name.contains("transport")) assertSame(fixture.original(), original.getCause());
        var projected = ReportModelAdapters.partialExecution(captured.snapshot());
        assertNull(projected.response(), "Missing HTTP response must remain absent at the library boundary");
        assertEquals(captured.snapshot().attempts(), projected.attempts());
        assertEquals(captured.snapshot().totalDuration(), projected.totalDuration());
        if (name.equals("retry-transport")) {
            assertEquals(1, captured.snapshot().retryEvents().size());
            assertEquals(List.of(ApiAttemptOutcome.RETRYABLE_RESPONSE, ApiAttemptOutcome.FAILED),
                    captured.snapshot().attemptHistory().stream().map(attempt -> attempt.outcome()).toList());
        }
    }
    @Test void preservesCompletedThenTerminalOrderAndDoesNotLeakIntoAnotherInvocation() {
        var evidence = new EventuateEvidence();
        var fixture = TerminalFailureFixtures.create("after-success", evidence::record);
        fixture.client().observe(42);
        var failure = assertThrows(RuntimeException.class, () -> fixture.client().observe(42));
        assertInstanceOf(EventuateExchange.Completed.class, evidence.entries().get(0));
        assertSame(failure, assertInstanceOf(EventuateExchange.Terminal.class, evidence.entries().get(1)).failure());
        assertEquals(1, evidence.exchanges().size());
        assertEquals(2, fixture.dispatches().get());
        assertEquals("NO_HTTP_EXECUTION", new EventuateEvidence().source());
    }
    @Test void captureFailureIsSuppressedOnTheSameOriginalExceptionWithoutReexecution() {
        var captureFailure = new IllegalStateException("controlled evidence sink failure");
        var captured = new AtomicReference<EventuateExchange.Terminal>();
        var fixture = TerminalFailureFixtures.create("transport", exchange -> {
            captured.set((EventuateExchange.Terminal) exchange); throw captureFailure;
        });
        var original = assertThrows(RuntimeException.class, () -> fixture.client().observe(42));
        assertSame(captured.get().failure(), original);
        assertSame(fixture.original(), original.getCause());
        assertArrayEquals(new Throwable[]{captureFailure}, original.getSuppressed());
        assertEquals(1, fixture.dispatches().get());
    }
    @Test void selfSuppressionIsAvoidedAndMutationIsNotReplayed() {
        var fixture = TerminalFailureFixtures.create("transport", exchange -> { throw (RuntimeException) ((EventuateExchange.Terminal) exchange).failure(); });
        var original = assertThrows(RuntimeException.class, () -> fixture.client().createOrder(7, "70.00"));
        assertSame(fixture.original(), original.getCause());
        assertEquals(0, original.getSuppressed().length);
        assertEquals(1, fixture.dispatches().get());
        assertEquals("POST", ApiExecutionFailure.find(original).orElseThrow().request().method());
    }
    @Test void encodingErrorsWithoutSnapshotDoNotInventTerminalEvidence() {
        var evidence = new EventuateEvidence();
        try (var transport = new ScriptedApiTransport(List.of())) {
            var client = new EventuateApiClient(transport, evidence::record);
            var failure = assertThrows(IllegalArgumentException.class, () -> client.createOrder(7, "invalid amount"));
            assertTrue(ApiExecutionFailure.find(failure).isEmpty());
            assertTrue(evidence.entries().isEmpty());
            assertTrue(transport.requests().isEmpty());
        }
    }
    @Test void phaseScopesRestoreOwnershipAfterFailureAndKeepSnapshotsInTheirOwnPhase() {
        var evidence = new EventuateEvidence();
        var fixture = TerminalFailureFixtures.create("transport", evidence::record);
        for (String phase : List.of("SETUP", "TEARDOWN")) {
            try (var scope = evidence.inPhase(phase)) {
                assertThrows(RuntimeException.class, () -> fixture.client().observe(42));
            }
            assertEquals(1, evidence.entries(phase).size());
        }
        assertTrue(evidence.entries("TEST BODY").isEmpty());
        assertThrows(RuntimeException.class, () -> fixture.client().observe(42));
        assertEquals(1, evidence.entries("TEST BODY").size());
        assertEquals(3, evidence.entries().size());
    }
    @Test void successfulResultSinkFailureIsNotRecapturedAsATransportFailure() {
        var publication = new IllegalStateException("controlled completed-result sink");
        var entry = new AtomicReference<EventuateExchange>();
        var fixture = TerminalFailureFixtures.create("after-success", value -> { entry.set(value); throw publication; });
        var failure = assertThrows(RuntimeException.class, () -> fixture.client().observe(42));
        assertSame(publication, failure);
        assertInstanceOf(EventuateExchange.Completed.class, entry.get());
        assertTrue(ApiExecutionFailure.find(failure).isEmpty());
        assertEquals(1, fixture.dispatches().get());
    }
}
