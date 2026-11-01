package atf.eventuate.live;

import atf.core.config.ApiSettings;
import atf.core.config.ApiTransportMode;
import atf.core.http.auth.ApiAuthenticator;
import atf.core.http.execution.ApiExecutor;
import atf.core.http.response.ApiExecutionResult;
import atf.core.http.transport.HttpClientTransport;
import atf.core.http.transport.ApiTransport;
import atf.core.json.JsonDocuments;
import atf.eventuate.api.EventuateApiClient;
import atf.eventuate.api.OrderObservation;
import atf.eventuate.evidence.EventuateExchange;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.Set;
import java.util.function.Consumer;

/** One dispatch per mutation; each GET receives a fresh executor with the remaining budget. */
public final class LiveHttpClient implements AutoCloseable {
    private final HttpClient client;
    private final LiveSettings settings;
    private final Consumer<EventuateExchange> evidence;
    @FunctionalInterface public interface Transports { ApiTransport forTimeout(Duration timeout); }
    private final Transports transports;
    private int customerMutations, orderMutations;
    private static final ApiAuthenticator NO_AUTH = new ApiAuthenticator() {
        @Override public void apply(HttpRequest.Builder request, String profile) { }
        @Override public String describeMode(String profile) { return "none"; }
        @Override public String describeTokenState(String profile) { return "not required"; }
    };
    public LiveHttpClient(LiveSettings settings, Consumer<EventuateExchange> evidence) {
        this(settings, evidence, HttpClient.newBuilder().connectTimeout(settings.readTimeout()).followRedirects(HttpClient.Redirect.NEVER).build());
    }
    private LiveHttpClient(LiveSettings settings, Consumer<EventuateExchange> evidence, HttpClient client) {
        this.settings = settings; this.evidence = evidence;
        this.client = java.util.Objects.requireNonNull(client);
        transports = timeout -> new HttpClientTransport(client, NO_AUTH, timeout);
    }
    /** Explicit transport seam for pure boundary tests; no implicit substitution in the LIVE composition. */
    public LiveHttpClient(LiveSettings settings, Consumer<EventuateExchange> evidence, Transports transports) {
        this.settings = java.util.Objects.requireNonNull(settings); this.evidence = java.util.Objects.requireNonNull(evidence);
        this.transports = java.util.Objects.requireNonNull(transports); client = null;
    }
    private ApiExecutor executor(URI base, Duration remaining) {
        Duration timeout = remaining.compareTo(settings.readTimeout()) < 0 ? remaining : settings.readTimeout();
        return new ApiExecutor(transports.forTimeout(timeout),
                new ApiSettings(base, "none", timeout, Set.of(), 1).snapshot(), ApiTransportMode.LIVE);
    }
    private EventuateApiClient api(Duration remaining) {
        return new EventuateApiClient(executor(settings.orders(), remaining), executor(settings.orders(), remaining),
                executor(settings.customers(), remaining), evidence);
    }
    public long createCustomer(String name, String credit, Duration remaining) {
        if (customerMutations++ != 0) throw new IllegalStateException("Customer mutation already dispatched; replay forbidden");
        return identity(api(remaining).createCustomer(name, credit), "customerId");
    }
    public long createOrder(long customerId, String amount, Duration remaining) {
        if (orderMutations++ != 0) throw new IllegalStateException("Order mutation already dispatched; replay forbidden");
        return identity(api(remaining).createOrder(customerId, amount), "orderId");
    }
    private static long identity(ApiExecutionResult result, String field) {
        if (result.response().status() != 200 && result.response().status() != 201)
            throw new IllegalStateException("Mutation response HTTP " + result.response().status() + "; no replay");
        try {
            var value = JsonDocuments.read(result.response().body()).path(field);
            if (!value.isIntegralNumber() || !value.canConvertToLong() || value.longValue() <= 0)
                throw new IllegalStateException("Invalid mutation identity: " + field);
            return value.longValue();
        } catch (java.io.IOException failure) { throw new IllegalStateException("Invalid mutation JSON; no replay", failure); }
    }
    public OrderObservation observe(long orderId, Duration remaining) { return api(remaining).observe(orderId); }
    public int customerMutations() { return customerMutations; }
    public int orderMutations() { return orderMutations; }
    @Override public void close() { if (client != null) client.shutdownNow(); }
}
