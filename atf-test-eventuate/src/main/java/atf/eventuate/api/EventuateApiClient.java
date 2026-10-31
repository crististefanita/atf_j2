package atf.eventuate.api;
import atf.core.config.ApiSettings;
import atf.core.config.ApiTransportMode;
import atf.core.http.execution.ApiExecutor;
import atf.core.http.execution.evidence.ApiExecutionFailure;
import atf.core.http.request.ApiRequest;
import atf.core.http.response.ApiExecutionResult;
import atf.eventuate.mock.ScriptedApiTransport;
import atf.eventuate.evidence.EventuateExchange;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/** Mock-only binding with explicit mutation/read policies; only eligible GET responses may retry. */
public final class EventuateApiClient {
    private final ApiExecutor writes;
    private final ApiExecutor reads;
    private final Consumer<EventuateExchange> evidence;
    private final ObjectMapper json = new ObjectMapper();
    public EventuateApiClient(ScriptedApiTransport transport, Consumer<EventuateExchange> evidence) {
        URI base = URI.create("http://eventuate.mock.invalid");
        writes = new ApiExecutor(transport, new ApiSettings(base, "none", Duration.ofSeconds(2), Set.of(), 1).snapshot(), ApiTransportMode.ACCEPTANCE);
        reads = new ApiExecutor(transport, new ApiSettings(base, "none", Duration.ofSeconds(2), Set.of(503), 2).snapshot(), ApiTransportMode.ACCEPTANCE);
        this.evidence = java.util.Objects.requireNonNull(evidence);
    }
    /** Explicit composition seam; the caller owns executor policies and truthful provenance. */
    public EventuateApiClient(ApiExecutor writes, ApiExecutor reads, Consumer<EventuateExchange> evidence) {
        this.writes = java.util.Objects.requireNonNull(writes, "writes");
        this.reads = java.util.Objects.requireNonNull(reads, "reads");
        this.evidence = java.util.Objects.requireNonNull(evidence, "evidence");
    }
    public ApiExecutionResult createOrder(long customerId, String amount) {
        String body;
        try { body = json.writeValueAsString(Map.of("customerId", customerId, "orderTotal", Map.of("amount", new java.math.BigDecimal(amount)))); }
        catch (java.io.IOException failure) { throw new IllegalArgumentException("Cannot encode order", failure); }
        return execute(writes, new ApiRequest(HttpRequest.BodyPublishers.ofString(body), "POST", "/orders",
                Map.of(), Map.of("Content-Type", "application/json"), "none", null, body));
    }
    public ApiExecutionResult getOrder(long orderId) { return execute(reads, ApiRequest.of("GET", "/orders/" + orderId)); }
    public OrderObservation observe(long orderId) {
        var result = getOrder(orderId);
        if (result.response().status() != 200) throw new IllegalStateException("Order observation HTTP status: " + result.response().status());
        return OrderResponseDecoder.decode(result.response().body());
    }
    private ApiExecutionResult execute(ApiExecutor executor, ApiRequest request) {
        final ApiExecutionResult result;
        try {
            result = executor.execute(request);
        } catch (RuntimeException | Error failure) {
            try {
                ApiExecutionFailure.find(failure).ifPresent(snapshot -> evidence.accept(new EventuateExchange.Terminal(snapshot, failure)));
            } catch (RuntimeException | Error captureFailure) {
                if (captureFailure != failure) failure.addSuppressed(captureFailure);
            }
            throw failure;
        }
        evidence.accept(new EventuateExchange.Completed(result));
        return result;
    }
}
