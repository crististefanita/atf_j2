package atf.eventuate.live.sql;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

/** Independent read-only facts from the application's schema, including partial results on failure. */
public record LiveSqlSnapshot(long orderId, long customerId, List<OrderRow> orders,
        List<ReservationRow> reservations, List<Query> queries, boolean readOnly, Duration elapsed, String failure) {
    public record OrderRow(long id, long customerId, BigDecimal amount, String state, String rejectionReason) { }
    public record ReservationRow(long customerId, long orderId, BigDecimal amount) { }
    public record Query(String template, List<Long> parameters) { public Query { parameters = List.copyOf(parameters); } }
    public LiveSqlSnapshot {
        orders = List.copyOf(orders); reservations = List.copyOf(reservations); queries = List.copyOf(queries);
    }
    public boolean complete() { return failure.isEmpty(); }
}
