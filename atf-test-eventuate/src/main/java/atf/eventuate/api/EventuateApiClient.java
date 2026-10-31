package atf.eventuate.api;
import atf.core.config.ApiSettings;
import atf.core.config.ApiTransportMode;
import atf.core.http.execution.ApiExecutor;
import atf.core.http.request.ApiRequest;
import atf.core.http.response.ApiExecutionResult;
import atf.eventuate.mock.ScriptedApiTransport;
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
    private final Consumer<ApiExecutionResult> evidence;
    private final ObjectMapper json = new ObjectMapper();
    public EventuateApiClient(ScriptedApiTransport transport, Consumer<ApiExecutionResult> evidence) {
        URI base = URI.create("http://eventuate.mock.invalid");
        writes = new ApiExecutor(transport, new ApiSettings(base, "none", Duration.ofSeconds(2), Set.of(), 1).snapshot(), ApiTransportMode.ACCEPTANCE);
        reads = new ApiExecutor(transport, new ApiSettings(base, "none", Duration.ofSeconds(2), Set.of(503), 2).snapshot(), ApiTransportMode.ACCEPTANCE);
        this.evidence = java.util.Objects.requireNonNull(evidence);
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
        try {
            var body = json.readTree(result.response().body());
            if (!body.path("orderId").isIntegralNumber() || !body.path("orderState").isTextual())
                throw new IllegalArgumentException("Order response requires integral orderId and textual orderState");
            var reason = body.get("rejectionReason");
            if (reason == null || !(reason.isNull() || reason.isTextual()))
                throw new IllegalArgumentException("Order response requires nullable textual rejectionReason");
            return new OrderObservation(body.path("orderId").longValue(), body.path("orderState").textValue(), reason.isNull() ? null : reason.textValue());
        } catch (java.io.IOException failure) { throw new IllegalArgumentException("Invalid order response JSON", failure); }
    }
    private ApiExecutionResult execute(ApiExecutor executor, ApiRequest request) {
        var result = executor.execute(request);
        evidence.accept(result);
        return result;
    }
}
