package atf.sql.execution;

import atf.sql.query.SqlQuery;
import atf.sql.result.SqlSnapshot;
import atf.sql.result.SqlValue;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** One JDBC execution and one capture; timeout never renews its budget or replays SQL. */
public final class SqlExecutor implements AutoCloseable {
    private final SqlConnections connections;
    private final SqlCaptureLimits limits;
    private final Consumer<SqlSnapshot> evidence;
    private final boolean requireReadOnlyAcknowledgement;
    private final ExecutorService worker = Executors.newVirtualThreadPerTaskExecutor();
    private final AtomicReference<Connection> active = new AtomicReference<>();
    private final AtomicBoolean running = new AtomicBoolean();
    private volatile boolean poisoned, closed;
    public SqlExecutor(SqlConnections connections, Consumer<SqlSnapshot> evidence) {
        this(connections, SqlCaptureLimits.defaults(), SqlReadOnlyPolicy.REQUIRED, evidence);
    }
    public SqlExecutor(SqlConnections connections, SqlCaptureLimits limits, SqlReadOnlyPolicy policy, Consumer<SqlSnapshot> evidence) {
        this.connections = java.util.Objects.requireNonNull(connections); this.limits = java.util.Objects.requireNonNull(limits);
        this.requireReadOnlyAcknowledgement = java.util.Objects.requireNonNull(policy) == SqlReadOnlyPolicy.REQUIRED;
        this.evidence = java.util.Objects.requireNonNull(evidence);
    }
    public SqlSnapshot query(SqlQuery query, Duration budget) throws SQLException {
        java.util.Objects.requireNonNull(query); long nanos = java.util.Objects.requireNonNull(budget).toNanos();
        if (nanos <= 0 || nanos > TimeUnit.SECONDS.toNanos(Integer.MAX_VALUE - 1L))
            throw new IllegalArgumentException("Positive SQL budget within JDBC timeout range required");
        long inputUnits = query.template().length();
        for (var parameter : query.parameters()) inputUnits = Math.addExact(inputUnits, parameter.captureUnits());
        if (inputUnits > limits.payloadUnits()) throw new IllegalArgumentException("SQL input capture budget exceeded before dispatch");
        if (closed || poisoned || !running.compareAndSet(false, true)) throw new IllegalStateException("SQL executor closed, poisoned or already active");
        var capture = new Capture(query, nanos, limits.payloadUnits(), inputUnits);
        Future<SqlSnapshot> future = null;
        SqlSnapshot completed = null;
        Throwable primary = null;
        try {
            future = worker.submit(() -> execute(capture));
            completed = future.get(capture.remaining().toNanos(), TimeUnit.NANOSECONDS);
            return completed;
        } catch (ExecutionException failure) {
            primary = failure.getCause();
            if (primary instanceof SQLTimeoutException) poisoned = true;
            if (primary instanceof SQLException sql) throw sql;
            if (primary instanceof RuntimeException runtime) throw runtime;
            if (primary instanceof Error error) throw error;
            throw new IllegalStateException(primary);
        } catch (TimeoutException timeout) {
            poisoned = true; primary = new SQLTimeoutException("SQL deadline exhausted; execution not replayed", timeout);
            throw (SQLException) primary;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt(); poisoned = true;
            primary = new SQLException("SQL execution interrupted", interrupted); throw (SQLException) primary;
        } catch (SQLException failure) { primary = failure; poisoned = true; throw failure; }
        catch (RuntimeException | Error failure) { primary = failure; throw failure; }
        finally {
            if (primary != null && future != null && !future.isDone()) {
                future.cancel(true); poisoned = true;
                var connection = active.get();
                if (connection != null) try { connection.abort(command -> Thread.ofVirtual().name("atf-sql-abort").start(command)); }
                catch (SQLException | RuntimeException secondary) { if (primary != secondary) primary.addSuppressed(secondary); }
            }
            var snapshot = primary == null && completed != null ? completed : capture.snapshot(primary);
            if (primary != null) SqlExecutionFailure.attach(primary, snapshot);
            try { evidence.accept(snapshot); }
            catch (RuntimeException | Error secondary) {
                if (primary == null) { SqlExecutionFailure.attachPublication(secondary, snapshot); throw secondary; }
                if (primary != secondary) primary.addSuppressed(secondary);
            } finally { running.set(false); }
        }
    }
    private SqlSnapshot execute(Capture capture) throws SQLException {
        try (var connection = connections.open(capture.remaining())) {
            active.set(connection);
            if (Thread.currentThread().isInterrupted()) throw new SQLException("SQL cancelled before dispatch");
            connection.setReadOnly(true); capture.readOnly = connection.isReadOnly();
            if (requireReadOnlyAcknowledgement && !capture.readOnly) throw new SQLException("Driver did not acknowledge read-only connection");
            try (var statement = connection.prepareStatement(capture.query.template())) {
                statement.setQueryTimeout(Math.toIntExact(Math.max(1, (capture.remaining().toMillis() + 999) / 1000)));
                statement.setMaxRows(limits.rows() + 1);
                statement.setFetchSize(Math.min(128, limits.rows() + 1));
                for (int index = 0; index < capture.query.parameters().size(); index++) capture.query.parameters().get(index).bind(statement, index + 1);
                try (var rows = statement.executeQuery()) {
                    var metadata = rows.getMetaData(); int count = metadata.getColumnCount();
                    if (count > limits.columns()) throw new java.sql.SQLDataException("SQL column capture budget exceeded");
                    for (int index = 1; index <= count; index++) {
                        String label = metadata.getColumnLabel(index), typeName = metadata.getColumnTypeName(index);
                        capture.account((label == null ? 0 : label.length()) + (typeName == null ? 0 : typeName.length()) + 32L);
                        capture.columns.add(new SqlSnapshot.Column(index, label, metadata.getColumnType(index), typeName));
                    }
                    while (rows.next()) {
                        capture.remaining();
                        if (capture.rows.size() == limits.rows()) { capture.moreRows = true; break; }
                        capture.account(32); capture.rows.add(SqlRowCapture.read(rows, count, limits, capture::account));
                    }
                }
            }
        } finally { active.set(null); }
        capture.resourcesClosed = true; capture.remaining();
        return capture.snapshot(null);
    }
    private static final class Capture {
        private final SqlQuery query;
        private final String id = UUID.randomUUID().toString();
        private final long start = System.nanoTime(), budget, payloadLimit;
        private long payload;
        private final List<SqlSnapshot.Column> columns = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        private final List<List<SqlValue>> rows = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        private volatile boolean readOnly, moreRows, resourcesClosed;
        private Capture(SqlQuery query, long budget, long payloadLimit, long inputUnits) { this.query = query; this.budget = budget; this.payloadLimit = payloadLimit; this.payload = inputUnits; }
        private void account(long units) throws SQLException {
            if (units < 0 || units > payloadLimit - payload) throw new java.sql.SQLDataException("Aggregate SQL capture budget exhausted");
            payload += units;
        }
        private Duration remaining() throws SQLTimeoutException {
            long remaining = budget - Math.max(0, System.nanoTime() - start);
            if (remaining <= 0) throw new SQLTimeoutException("SQL execution exhausted the original budget");
            return Duration.ofNanos(remaining);
        }
        private SqlSnapshot snapshot(Throwable failure) {
            List<SqlSnapshot.Column> columnCopy;
            List<List<SqlValue>> rowCopy;
            synchronized (columns) { columnCopy = List.copyOf(columns); }
            synchronized (rows) { rowCopy = List.copyOf(rows); }
            boolean partial = moreRows || rowCopy.stream().flatMap(List::stream).anyMatch(value -> !value.complete());
            var outcome = failure != null ? SqlSnapshot.Outcome.FAILED : partial ? SqlSnapshot.Outcome.INCOMPLETE : SqlSnapshot.Outcome.COMPLETE;
            return new SqlSnapshot(id, query, columnCopy, rowCopy, outcome, moreRows, readOnly, resourcesClosed,
                    Duration.ofNanos(Math.max(0, System.nanoTime() - start)), failure == null ? null : SqlSnapshot.Failure.from(failure));
        }
    }
    @Override public void close() {
        if (closed) return;
        closed = true; worker.shutdownNow();
        try { if (!worker.awaitTermination(1, TimeUnit.SECONDS)) throw new IllegalStateException("SQL worker cleanup is still pending after one second"); }
        catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new IllegalStateException("SQL cleanup interrupted", interrupted); }
    }
}
