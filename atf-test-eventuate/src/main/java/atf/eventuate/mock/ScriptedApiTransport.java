package atf.eventuate.mock;
import atf.core.http.request.ApiRequest;
import atf.core.http.response.ApiResponse;
import atf.core.http.transport.ApiTransport;
import atf.mock.AtfMockConfiguration;
import atf.mock.AtfMockTransport;
import atf.mock.MockRoute;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

/** Finite expected exchanges, delegated to the real ATF mock; not an application simulator. */
public final class ScriptedApiTransport implements ApiTransport, AutoCloseable {
    private final List<MockRoute> script;
    private final List<ApiRequest> requests = new ArrayList<>();
    private boolean closed;
    public ScriptedApiTransport(List<MockRoute> script) { this.script = List.copyOf(script); }
    @Override public CompletableFuture<ApiResponse> send(ApiRequest request, String url, String auth) {
        if (closed || requests.size() == script.size())
            return CompletableFuture.failedFuture(new IllegalStateException("Mock script exhausted or closed"));
        var route = script.get(requests.size());
        if (!route.matches(request)) return CompletableFuture.failedFuture(new IllegalStateException("Unexpected mock exchange: " + request.method() + " " + request.path()));
        requests.add(request);
        return new AtfMockTransport(new AtfMockConfiguration(List.of(route), route)).send(request, url, auth);
    }
    public List<ApiRequest> requests() { return List.copyOf(requests); }
    public int remaining() { return script.size() - requests.size(); }
    public boolean isClosed() { return closed; }
    @Override public void close() { closed = true; }
}
