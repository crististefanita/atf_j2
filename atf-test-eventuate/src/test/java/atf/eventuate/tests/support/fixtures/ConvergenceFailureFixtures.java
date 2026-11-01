package atf.eventuate.tests.support.fixtures;

import atf.core.config.ApiSettings;
import atf.core.config.ApiTransportMode;
import atf.core.http.execution.ApiExecutor;
import atf.core.http.transport.ApiTransport;
import atf.eventuate.api.EventuateApiClient;
import atf.eventuate.await.OrderAwaiter;
import atf.eventuate.await.OrderConvergenceSnapshot;
import atf.eventuate.evidence.EventuateEvidence;
import atf.eventuate.mock.ScriptedApiTransport;
import atf.eventuate.validation.OrderContract;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import static atf.eventuate.tests.support.fixtures.EventuateFixtures.*;

/** Explicit offline fault selection; the real client/Mock execute one creation and bounded reads. */
public final class ConvergenceFailureFixtures {
    public static final List<String> CASES = List.of("read", "pause", "empty", "sink", "expired");
    public record Capture(Throwable original, OrderConvergenceSnapshot snapshot, int creations, int reads, int dispatches) { }
    private static final AtomicReference<Capture> LAST = new AtomicReference<>();
    private ConvergenceFailureFixtures() { }
    public static Capture consume() { return LAST.getAndSet(null); }
    public static String selected() { return System.getProperty("atf.eventuate.convergence", "none"); }
    public static void failIfSelected(EventuateEvidence evidence) {
        String name = selected();
        if (name.equals("none")) return;
        if (!CASES.contains(name)) throw new IllegalArgumentException("Unknown convergence fixture: " + name);
        LAST.set(null);
        var clock = new ManualClock(); var creations = new AtomicInteger(); var reads = new AtomicInteger();
        var dispatches = new AtomicInteger(); var getCalls = new AtomicInteger(); var ticks = new AtomicInteger();
        var snapshot = new AtomicReference<OrderConvergenceSnapshot>();
        var originalCause = new IOException("controlled convergence transport cause; password=CONVERGENCE_SECRET_108");
        try (var script = new ScriptedApiTransport(List.of(creation(), route("GET", "/orders/42", 200, order("PENDING"))))) {
            ApiTransport transport = (request, url, auth) -> {
                dispatches.incrementAndGet();
                if (request.method().equals("POST")) creations.incrementAndGet();
                if (request.method().equals("GET") && (getCalls.incrementAndGet() == 2 || name.equals("empty")))
                    return CompletableFuture.failedFuture(originalCause);
                return script.send(request, url, auth);
            };
            var settings = new ApiSettings(URI.create("http://eventuate.mock.invalid"), "none", Duration.ofSeconds(2), Set.of(), 1).snapshot();
            var executor = new ApiExecutor(transport, settings, ApiTransportMode.ACCEPTANCE);
            var client = new EventuateApiClient(executor, executor, evidence::record);
            client.createOrder(7, "70.00");
            var awaiter = new OrderAwaiter(() -> name.equals("expired") && ticks.getAndIncrement() > 0
                    ? Duration.ofMillis(25).toNanos() : clock.nanoTime(), delay -> {
                if (name.equals("pause")) {
                    clock.advance(Duration.ofMillis(2));
                    throw new IllegalStateException("controlled convergence pause failure", new InterruptedException("controlled interruption"));
                }
                clock.advance(delay);
            }, captured -> {
                snapshot.set(captured); evidence.recordConvergence(captured);
                if (name.equals("sink")) throw new IllegalArgumentException("controlled convergence sink failure");
            });
            try {
                var result = awaiter.await(() -> { reads.incrementAndGet(); return client.observe(42); },
                        new OrderContract(42, "APPROVED", null), Duration.ofMillis(25), Duration.ofMillis(10));
                throw new AssertionError(result.converged() ? "Fixture unexpectedly converged" : "Controlled convergence deadline reached");
            } catch (RuntimeException | Error original) {
                LAST.set(new Capture(original, snapshot.get(), creations.get(), reads.get(), dispatches.get()));
                throw original;
            }
        }
    }
}
