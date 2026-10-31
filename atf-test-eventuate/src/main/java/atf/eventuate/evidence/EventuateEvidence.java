package atf.eventuate.evidence;
import atf.core.http.response.ApiExecutionResult;
import java.util.List;
import java.util.ArrayList;

/** Invocation-owned captured facts; the report consumes snapshots after execution. */
public final class EventuateEvidence {
    private final List<ApiExecutionResult> exchanges = new ArrayList<>();
    public void record(ApiExecutionResult result) { exchanges.add(java.util.Objects.requireNonNull(result)); }
    public List<ApiExecutionResult> exchanges() { return List.copyOf(exchanges); }
    public String source() { return "MOCK"; }
}
