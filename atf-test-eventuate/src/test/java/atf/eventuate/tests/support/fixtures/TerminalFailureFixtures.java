package atf.eventuate.tests.support.fixtures;

import atf.core.config.ApiSettings;
import atf.core.config.ApiTransportMode;
import atf.core.http.execution.ApiExecutor;
import atf.core.http.response.ApiAttemptOutcome;
import atf.core.http.response.ApiResponse;
import atf.core.http.transport.ApiTransport;
import atf.eventuate.api.EventuateApiClient;
import atf.eventuate.evidence.EventuateEvidence;
import atf.eventuate.evidence.EventuateExchange;
import atf.eventuate.mock.ScriptedApiTransport;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/** Finite offline fault fixtures, shared by the consumer unit and artifact probes. */
public final class TerminalFailureFixtures {
    public static final List<String> CASES = List.of("exhausted", "transport", "timeout", "cancel", "retry-transport", "after-success");
    private static final AtomicReference<Throwable> LAST = new AtomicReference<>();
    public record Fixture(EventuateApiClient client, AtomicInteger dispatches, Throwable original,
                          ApiAttemptOutcome outcome, int attempts) { }
    private TerminalFailureFixtures() { }

    public static Fixture create(String name, Consumer<EventuateExchange> sink) {
        if (!CASES.contains(name)) throw new IllegalArgumentException("Unknown terminal fixture: " + name);
        var dispatches = new AtomicInteger();
        var original = new IOException("controlled terminal transport");
        // Fixture serialization precedes the execution deadline; this case tests sink ownership.
        String completedBody = name.equals("after-success") ? EventuateFixtures.order("APPROVED") : "";
        ApiAttemptOutcome outcome = name.equals("timeout") ? ApiAttemptOutcome.TIMED_OUT
                : name.equals("cancel") ? ApiAttemptOutcome.CANCELLED : ApiAttemptOutcome.FAILED;
        ApiTransport transport;
        if (name.equals("exhausted")) {
            var script = new ScriptedApiTransport(List.of());
            transport = (request, url, auth) -> { dispatches.incrementAndGet(); return script.send(request, url, auth); };
        } else {
            transport = (request, url, auth) -> {
                int call = dispatches.incrementAndGet();
                if (name.equals("after-success") && call == 1)
                    return CompletableFuture.completedFuture(new ApiResponse(200, completedBody, Map.of(), Duration.ZERO, url));
                if (name.equals("retry-transport") && call == 1)
                    return CompletableFuture.completedFuture(new ApiResponse(503, "busy", Map.of(), Duration.ZERO, url));
                if (name.equals("timeout")) return new CompletableFuture<>();
                if (name.equals("cancel")) { var cancelled = new CompletableFuture<ApiResponse>(); cancelled.cancel(false); return cancelled; }
                return CompletableFuture.failedFuture(original);
            };
        }
        var settings = new ApiSettings(URI.create("http://eventuate.mock.invalid"), "none", Duration.ofMillis(250),
                name.equals("retry-transport") ? Set.of(503) : Set.of(), name.equals("retry-transport") ? 2 : 1).snapshot();
        var executor = new ApiExecutor(transport, settings, ApiTransportMode.ACCEPTANCE);
        return new Fixture(new EventuateApiClient(executor, executor, sink), dispatches, original, outcome,
                name.equals("retry-transport") ? 2 : 1);
    }
    public static void failIfSelected(EventuateEvidence evidence) {
        failIfSelected(evidence, "TEST BODY");
    }
    public static void failIfSelected(EventuateEvidence evidence, String phase) {
        String name = System.getProperty("atf.eventuate.terminal", "none");
        if (name.equals("none") || !phase.equals(System.getProperty("atf.eventuate.terminal.phase", "TEST BODY"))) return;
        LAST.set(null);
        var fixture = create(name, exchange -> {
            evidence.record(exchange);
            if (exchange instanceof EventuateExchange.Terminal terminal) LAST.set(terminal.failure());
        });
        if (name.equals("after-success")) fixture.client().observe(42);
        fixture.client().observe(42);
        throw new AssertionError("Terminal fixture unexpectedly completed");
    }
    public static Throwable consumeFailure() { return LAST.getAndSet(null); }
    public static boolean isCapturedOriginal(Throwable failure) { return failure == LAST.get(); }
}
