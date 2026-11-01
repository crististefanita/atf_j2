package atf.eventuate.live.sql;

import atf.eventuate.live.LiveDeadline;
import atf.eventuate.live.LiveSettings;
import atf.sql.execution.SqlConnections;
import atf.sql.execution.SqlExecutor;
import atf.sql.query.SqlParameter;
import atf.sql.query.SqlQuery;
import atf.sql.result.SqlSnapshot;
import java.sql.DriverManager;
import java.sql.JDBCType;
import java.sql.SQLException;
import java.time.Duration;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;

/** Application schema and correlation only; bounded JDBC execution belongs to atf-sql. */
public final class LiveSqlWitness implements AutoCloseable {
    public static final String ORDER_QUERY = "SELECT id, customer_id, amount, state, rejection_reason FROM orders WHERE id = ?";
    public static final String RESERVATION_QUERY = "SELECT customer_id, credit_reservations_key, amount FROM customer_credit_reservations WHERE customer_id = ? AND credit_reservations_key = ?";
    private final Consumer<LiveSqlSnapshot> evidence;
    private final List<SqlSnapshot> executions = new ArrayList<>();
    private final SqlExecutor executor;
    public LiveSqlWitness(SqlConnections connections, Consumer<LiveSqlSnapshot> evidence) {
        this.evidence = java.util.Objects.requireNonNull(evidence);
        executor = new SqlExecutor(connections, executions::add);
    }
    public static LiveSqlWitness mysql(LiveSettings settings, Consumer<LiveSqlSnapshot> evidence) {
        return new LiveSqlWitness(remaining -> {
            var properties = new Properties();
            properties.setProperty("user", settings.jdbcUser()); properties.setProperty("password", settings.jdbcPassword());
            String millis = Long.toString(Math.max(1, remaining.toMillis()));
            properties.setProperty("connectTimeout", millis); properties.setProperty("socketTimeout", millis);
            return DriverManager.getConnection(settings.jdbcUrl(), properties);
        }, evidence);
    }
    public LiveSqlSnapshot read(long orderId, long customerId, Duration budget) throws SQLException {
        var deadline = new LiveDeadline(budget);
        long started = System.nanoTime();
        int before = executions.size();
        var orders = new ArrayList<LiveSqlSnapshot.OrderRow>();
        var reservations = new ArrayList<LiveSqlSnapshot.ReservationRow>();
        Throwable primary = null;
        try {
            var order = executor.query(new SqlQuery(ORDER_QUERY, List.of(new SqlParameter(JDBCType.BIGINT, orderId))), deadline.remaining()).requireComplete();
            for (var row : order.rows()) orders.add(new LiveSqlSnapshot.OrderRow(number(row.get(0).value()), number(row.get(1).value()),
                    decimal(row.get(2).value()), (String) row.get(3).value(), java.util.Objects.toString(row.get(4).value(), null)));
            var credit = executor.query(new SqlQuery(RESERVATION_QUERY, List.of(new SqlParameter(JDBCType.BIGINT, customerId),
                    new SqlParameter(JDBCType.BIGINT, orderId))), deadline.remaining()).requireComplete();
            for (var row : credit.rows()) reservations.add(new LiveSqlSnapshot.ReservationRow(number(row.get(0).value()),
                    number(row.get(1).value()), decimal(row.get(2).value())));
            return snapshot(orderId, customerId, before, orders, reservations, started, null);
        } catch (SQLException | RuntimeException | Error failure) { primary = failure; throw failure; }
        finally {
            try { evidence.accept(snapshot(orderId, customerId, before, orders, reservations, started, primary)); }
            catch (RuntimeException | Error secondary) { if (primary == null) throw secondary; if (secondary != primary) primary.addSuppressed(secondary); }
        }
    }
    private LiveSqlSnapshot snapshot(long orderId, long customerId, int before, List<LiveSqlSnapshot.OrderRow> orders,
            List<LiveSqlSnapshot.ReservationRow> reservations, long start, Throwable failure) {
        var selected = executions.subList(before, executions.size());
        var queries = selected.stream().map(value -> new LiveSqlSnapshot.Query(value.query().template(),
                value.query().parameters().stream().map(parameter -> number(parameter.value())).toList())).toList();
        return new LiveSqlSnapshot(orderId, customerId, orders, reservations, queries,
                !selected.isEmpty() && selected.stream().allMatch(SqlSnapshot::readOnlyAcknowledged),
                Duration.ofNanos(Math.max(0, System.nanoTime() - start)), failure == null ? "" : failure.toString());
    }
    private static long number(Object value) {
        if (value instanceof Long number) return number;
        if (value instanceof Integer number) return number.longValue();
        if (value instanceof BigDecimal number) return number.longValueExact();
        throw new IllegalStateException("Unsupported SQL identity type");
    }
    private static BigDecimal decimal(Object value) {
        if (value instanceof BigDecimal number) return number;
        throw new IllegalStateException("SQL money must be exact DECIMAL");
    }
    @Override public void close() { executor.close(); }
}
