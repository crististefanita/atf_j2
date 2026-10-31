package atf.eventuate.evidence;

import atf.core.http.execution.evidence.ApiExecutionSnapshot;
import atf.core.http.request.ApiRequest;
import atf.core.http.response.ApiExecutionResult;
import atf.core.http.response.ApiResponse;
import atf.core.http.transport.ApiTransportProvenance;
import java.util.Objects;
import java.util.Optional;

/** Ordered execution facts: a completed result or a terminal snapshot with its original exception. */
public sealed interface EventuateExchange {
    ApiRequest request();
    ApiTransportProvenance provenance();
    Optional<ApiResponse> response();

    record Completed(ApiExecutionResult result) implements EventuateExchange {
        public Completed { Objects.requireNonNull(result, "result"); }
        @Override public ApiRequest request() { return result.request(); }
        @Override public ApiTransportProvenance provenance() { return result.provenance(); }
        @Override public Optional<ApiResponse> response() { return Optional.of(result.response()); }
    }
    record Terminal(ApiExecutionSnapshot snapshot, Throwable failure) implements EventuateExchange {
        public Terminal { Objects.requireNonNull(snapshot, "snapshot"); Objects.requireNonNull(failure, "failure"); }
        @Override public ApiRequest request() { return snapshot.request(); }
        @Override public ApiTransportProvenance provenance() { return snapshot.provenance(); }
        @Override public Optional<ApiResponse> response() { return snapshot.response(); }
    }
}
