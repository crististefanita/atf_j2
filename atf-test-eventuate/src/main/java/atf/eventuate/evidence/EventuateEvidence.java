package atf.eventuate.evidence;
import atf.core.http.response.ApiExecutionResult;
import atf.eventuate.await.OrderConvergenceSnapshot;
import java.util.List;
import java.util.ArrayList;

/** Invocation-owned captured facts; the report consumes snapshots after execution. */
public final class EventuateEvidence {
    private record Entry(String phase, EventuateExchange exchange) { }
    private record ConvergenceEntry(String phase, OrderConvergenceSnapshot snapshot) { }
    private record SqlEntry(String phase, atf.eventuate.sql.SqlEffectsSnapshot snapshot) { }
    private final List<Entry> captured = new ArrayList<>();
    private final List<ConvergenceEntry> convergences = new ArrayList<>();
    private final List<SqlEntry> sql = new ArrayList<>();
    private String phase = "TEST BODY";
    public void record(EventuateExchange exchange) { captured.add(new Entry(phase, java.util.Objects.requireNonNull(exchange))); }
    public void record(ApiExecutionResult result) { record(new EventuateExchange.Completed(result)); }
    public void recordConvergence(OrderConvergenceSnapshot snapshot) {
        convergences.add(new ConvergenceEntry(phase, java.util.Objects.requireNonNull(snapshot)));
    }
    public List<OrderConvergenceSnapshot> convergences() {
        return convergences.stream().map(ConvergenceEntry::snapshot).toList();
    }
    public void recordSql(atf.eventuate.sql.SqlEffectsSnapshot snapshot) {
        sql.add(new SqlEntry(phase, java.util.Objects.requireNonNull(snapshot)));
    }
    public List<atf.eventuate.sql.SqlEffectsSnapshot> sql() { return sql.stream().map(SqlEntry::snapshot).toList(); }
    public List<atf.eventuate.sql.SqlEffectsSnapshot> sql(String phase) {
        return sql.stream().filter(entry -> entry.phase().equals(phase)).map(SqlEntry::snapshot).toList();
    }
    public List<OrderConvergenceSnapshot> convergences(String phase) {
        return convergences.stream().filter(entry -> entry.phase().equals(phase)).map(ConvergenceEntry::snapshot).toList();
    }
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
