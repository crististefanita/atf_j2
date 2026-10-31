package atf.eventuate.evidence;
import atf.core.http.response.ApiExecutionResult;
import java.util.List;
import java.util.ArrayList;

/** Invocation-owned captured facts; the report consumes snapshots after execution. */
public final class EventuateEvidence {
    private record Entry(String phase, EventuateExchange exchange) { }
    private final List<Entry> captured = new ArrayList<>();
    private String phase = "TEST BODY";
    public void record(EventuateExchange exchange) { captured.add(new Entry(phase, java.util.Objects.requireNonNull(exchange))); }
    public void record(ApiExecutionResult result) { record(new EventuateExchange.Completed(result)); }
    public List<EventuateExchange> entries() { return captured.stream().map(Entry::exchange).toList(); }
    public List<EventuateExchange> entries(String phase) {
        return captured.stream().filter(entry -> entry.phase().equals(phase)).map(Entry::exchange).toList();
    }
    /** Scoped JUnit phase ownership; closing restores the caller even after an exception. */
    public PhaseScope inPhase(String phase) {
        if (!List.of("SETUP", "TEST BODY", "TEARDOWN").contains(phase)) throw new IllegalArgumentException("Unknown evidence phase");
        return new PhaseScope(phase);
    }
    public final class PhaseScope implements AutoCloseable {
        private final String previous;
        private boolean closed;
        private PhaseScope(String selected) { previous = phase; phase = selected; }
        @Override public void close() { if (!closed) { phase = previous; closed = true; } }
    }
    /** Completed responses only; failures are available through the ordered entries view. */
    public List<ApiExecutionResult> exchanges() {
        return entries().stream().filter(EventuateExchange.Completed.class::isInstance)
                .map(EventuateExchange.Completed.class::cast).map(EventuateExchange.Completed::result).toList();
    }
    public String source() {
        return captured.isEmpty() ? "NO_HTTP_EXECUTION" : entries().stream().map(entry -> entry.provenance().name())
                .distinct().collect(java.util.stream.Collectors.joining(" / "));
    }
}
