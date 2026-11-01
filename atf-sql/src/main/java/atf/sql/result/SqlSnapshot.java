package atf.sql.result;

import atf.sql.query.SqlQuery;
import java.time.Duration;
import java.util.List;

/** Immutable execution evidence; column labels and duplicate rows are ordered lists, never map keys. */
public record SqlSnapshot(String executionId, SqlQuery query, List<Column> columns, List<List<SqlValue>> rows,
        Outcome outcome, boolean moreRows, boolean readOnlyAcknowledged, boolean resourcesClosed,
        Duration elapsed, Failure failure) {
    public enum Outcome { COMPLETE, INCOMPLETE, FAILED }
    public record Column(int ordinal, String label, int jdbcType, String typeName) { }
    public record Failure(String type, String message, String sqlState, int vendorCode, List<String> suppressed) {
        public Failure { suppressed = List.copyOf(suppressed); }
        public static Failure from(Throwable original) {
            return new Failure(original.getClass().getName(), java.util.Objects.toString(original.getMessage(), ""),
                    original instanceof java.sql.SQLException sql ? sql.getSQLState() : null,
                    original instanceof java.sql.SQLException sql ? sql.getErrorCode() : 0,
                    java.util.Arrays.stream(original.getSuppressed()).map(Throwable::toString).toList());
        }
    }
    public SqlSnapshot {
        if (java.util.Objects.requireNonNull(executionId).isBlank()) throw new IllegalArgumentException("SQL execution identity required");
        java.util.Objects.requireNonNull(query); java.util.Objects.requireNonNull(outcome); java.util.Objects.requireNonNull(elapsed);
        columns = List.copyOf(columns); rows = rows.stream().map(List::copyOf).toList();
        for (int index = 0; index < columns.size(); index++) if (columns.get(index).ordinal() != index + 1)
            throw new IllegalArgumentException("SQL columns must preserve consecutive JDBC ordinals");
        int width = columns.size();
        if (rows.stream().anyMatch(row -> row.size() != width)) throw new IllegalArgumentException("SQL row width differs from metadata");
        boolean partial = moreRows || rows.stream().flatMap(List::stream).anyMatch(value -> !value.complete());
        if (elapsed.isNegative() || (outcome == Outcome.FAILED) != (failure != null)
                || (outcome == Outcome.COMPLETE && (partial || !resourcesClosed))
                || (outcome == Outcome.INCOMPLETE && !partial)) throw new IllegalArgumentException("Inconsistent SQL execution outcome");
    }
    public boolean complete() { return outcome == Outcome.COMPLETE; }
    @Override public String toString() { return "SqlSnapshot[id=" + executionId + ", outcome=" + outcome + ", columns=" + columns.size() + ", rows=" + rows.size() + "]"; }
    public SqlSnapshot requireComplete() {
        if (!complete()) throw new IllegalStateException("SQL execution " + executionId + " is " + outcome + "; full-result assertion refused");
        return this;
    }
}
