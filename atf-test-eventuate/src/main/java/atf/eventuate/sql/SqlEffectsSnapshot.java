package atf.eventuate.sql;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Facts from one read, including partial rows on failure; no expected values or verdict. */
public record SqlEffectsSnapshot(long requestedOrderId, String witnessId, Provenance provenance,
        List<Reservation> reservations, Duration elapsed, Optional<Failure> failure) {
    public enum Provenance { SQL_EMBEDDED }
    public record Reservation(long reservationId, long orderId, BigDecimal amount, String state) {
        public Reservation { Objects.requireNonNull(amount); Objects.requireNonNull(state); }
    }
    public record Failure(String type, String message, String sqlState, int vendorCode,
                          String originalCause, List<String> suppressed) {
        public Failure { suppressed = List.copyOf(suppressed); }
        static Failure from(Throwable original) {
            return new Failure(original.getClass().getName(), Objects.toString(original.getMessage(), ""),
                    original instanceof SQLException sql ? Objects.toString(sql.getSQLState(), "unknown") : "not SQL",
                    original instanceof SQLException sql ? sql.getErrorCode() : 0,
                    Objects.toString(original.getCause(), "none"),
                    java.util.Arrays.stream(original.getSuppressed()).map(Throwable::toString).toList());
        }
    }
    public SqlEffectsSnapshot {
        if (Objects.requireNonNull(witnessId).isBlank()) throw new IllegalArgumentException("Witness ID required");
        Objects.requireNonNull(provenance); reservations = List.copyOf(reservations);
        Objects.requireNonNull(elapsed); Objects.requireNonNull(failure);
        if (elapsed.isNegative()) throw new IllegalArgumentException("Negative SQL elapsed time");
    }
    public boolean complete() { return failure.isEmpty(); }
}
