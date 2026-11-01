package atf.eventuate.sql;

import java.math.BigDecimal;
import java.util.Objects;

/** Independent oracle for one reservation, never a deduplication implementation. */
public record ReservationEffectContract(long orderId, BigDecimal amount, String state) {
    public ReservationEffectContract { Objects.requireNonNull(amount); Objects.requireNonNull(state); }
    public record Result(boolean complete, boolean identity, boolean singleEffect, boolean credit, boolean state) {
        public boolean passed() { return complete && identity && singleEffect && credit && state; }
    }
    public Result evaluate(SqlEffectsSnapshot actual) {
        var rows = actual.reservations();
        return new Result(actual.complete(), actual.requestedOrderId() == orderId
                && rows.stream().allMatch(row -> row.orderId() == orderId), rows.size() == 1,
                rows.stream().map(SqlEffectsSnapshot.Reservation::amount).reduce(BigDecimal.ZERO, BigDecimal::add).compareTo(amount) == 0,
                rows.stream().allMatch(row -> row.state().equals(state)));
    }
}
