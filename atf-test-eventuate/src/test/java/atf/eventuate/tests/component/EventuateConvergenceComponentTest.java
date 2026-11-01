package atf.eventuate.tests.component;
import atf.eventuate.api.EventuateApiClient;
import atf.eventuate.await.OrderAwaiter;
import atf.eventuate.validation.OrderContract;
import atf.eventuate.evidence.EventuateEvidence;
import atf.eventuate.mock.ScriptedApiTransport;
import atf.eventuate.tests.support.EventuateComponentTest;
import atf.eventuate.tests.support.fixtures.*;
import atf.testkit.assertions.RecordedAssertions;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.util.List;
import static atf.eventuate.tests.support.fixtures.EventuateFixtures.*;

@EventuateComponentTest
public class EventuateConvergenceComponentTest extends atf.eventuate.tests.support.EventuateComponentSupport {
    @Test @org.junit.jupiter.api.DisplayName("Convergence · bounded read retry")
    void separatesReadRetryFromConvergenceAndNeverReplaysCreation(RecordedAssertions checks, EventuateEvidence evidence) {
        ConvergenceFailureFixtures.failIfSelected(evidence);
        var clock = new ManualClock();
        try (var transport = new ScriptedApiTransport(List.of(creation(),
                route("GET", "/orders/42", 503, "{\"error\":\"temporarily unavailable\"}"),
                route("GET", "/orders/42", 200, order("PENDING")), route("GET", "/orders/42", 200, order("PENDING")),
                route("GET", "/orders/42", 200, order("APPROVED"))))) {
            var client = new EventuateApiClient(transport, evidence::record);
            client.createOrder(7, "70.00");
            var result = new OrderAwaiter(clock::nanoTime, clock::advance, evidence::recordConvergence).await(() -> client.observe(42), new OrderContract(42, "APPROVED", null), Duration.ofMillis(25), Duration.ofMillis(10));
            checks.isTrue("Order converges within one budget", result.converged());
            checks.equals("Convergence observations", 3, result.observations().size());
            checks.equals("Measured virtual wait", Duration.ofMillis(20), result.elapsed());
            checks.equals("HTTP attempts in first read", 2, evidence.exchanges().get(1).attempts());
            checks.equals("Creation dispatches", 1L, transport.requests().stream().filter(request -> request.method().equals("POST")).count());
            checks.equals("Total dispatched exchanges", 5, transport.requests().size());
            checks.equals("All expected exchanges consumed", 0, transport.remaining());
        }
        var expiredClock = new ManualClock();
        var expired = new OrderAwaiter(expiredClock::nanoTime, expiredClock::advance, evidence::recordConvergence).await(
                () -> new atf.eventuate.api.OrderObservation(42, "PENDING", ""), new OrderContract(42, "APPROVED", null), Duration.ofMillis(20), Duration.ofMillis(10));
        checks.isFalse("Negative control refuses nonconvergence", expired.converged());
        checks.equals("Exhausted budget is not renewed", Duration.ofMillis(20), expired.elapsed());
        checks.equals("No read at the expired deadline", 2, expired.observations().size());
        var identityClock = new ManualClock();
        var foreign = new OrderAwaiter(identityClock::nanoTime, identityClock::advance, evidence::recordConvergence).await(
                () -> new atf.eventuate.api.OrderObservation(99, "APPROVED", null), new OrderContract(42, "APPROVED", null),
                Duration.ofMillis(20), Duration.ofMillis(10));
        checks.isFalse("Foreign approved order cannot converge", foreign.converged());
        checks.equals("Foreign order observations", 2, foreign.observations().size());
        checks.equals("Observed foreign order identity", 99L, foreign.last().orderId());
        checks.equals("Foreign identity consumes only the original budget", Duration.ofMillis(20), foreign.elapsed());
    }
}
