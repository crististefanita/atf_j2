package atf.sql.execution;

import atf.sql.result.SqlSnapshot;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Optional;

/** Supplemental evidence; the original SQLException/RuntimeException is thrown unchanged. */
public final class SqlExecutionFailure extends Exception {
    public enum Origin { EXECUTION, PUBLICATION }
    private final SqlSnapshot snapshot;
    private final Origin origin;
    private SqlExecutionFailure(SqlSnapshot snapshot, Origin origin) { super("SQL " + origin + " evidence " + snapshot.executionId(), null, false, false); this.snapshot = snapshot; this.origin = origin; }
    public SqlSnapshot snapshot() { return snapshot; }
    static void attach(Throwable original, SqlSnapshot snapshot) { original.addSuppressed(new SqlExecutionFailure(snapshot, Origin.EXECUTION)); }
    static void attachPublication(Throwable original, SqlSnapshot snapshot) { original.addSuppressed(new SqlExecutionFailure(snapshot, Origin.PUBLICATION)); }
    public static Optional<SqlSnapshot> find(Throwable failure) {
        return evidence(failure).map(SqlExecutionFailure::snapshot);
    }
    public static Optional<Origin> origin(Throwable failure) { return evidence(failure).map(value -> value.origin); }
    private static Optional<SqlExecutionFailure> evidence(Throwable failure) {
        var seen = Collections.newSetFromMap(new IdentityHashMap<Throwable, Boolean>());
        var pending = new java.util.ArrayDeque<Throwable>();
        if (failure != null) pending.add(failure);
        while (!pending.isEmpty() && seen.size() < 256) {
            var current = pending.removeFirst();
            if (!seen.add(current)) continue;
            if (current instanceof SqlExecutionFailure evidence) return Optional.of(evidence);
            if (current.getCause() != null) pending.add(current.getCause());
            Collections.addAll(pending, current.getSuppressed());
        }
        return Optional.empty();
    }
}
