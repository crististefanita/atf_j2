package atf.eventuate.sql;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/** One fresh connection and parameterized SELECT; JDBC owns resource suppression. */
public final class JdbcOrderEffectsReader implements OrderEffectsReader {
    private static final String SELECT = "SELECT reservation_id, order_id, amount, state "
            + "FROM atf_order_reservations WHERE order_id = ? ORDER BY reservation_id";
    private final DataSource source;
    private final String witnessId;
    private final SqlEffectsSnapshot.Provenance provenance;
    private final Consumer<SqlEffectsSnapshot> evidence;
    public JdbcOrderEffectsReader(DataSource source, String witnessId, SqlEffectsSnapshot.Provenance provenance,
                                 Consumer<SqlEffectsSnapshot> evidence) {
        this.source = Objects.requireNonNull(source); this.witnessId = Objects.requireNonNull(witnessId);
        if (witnessId.isBlank()) throw new IllegalArgumentException("Witness ID required");
        this.provenance = Objects.requireNonNull(provenance); this.evidence = Objects.requireNonNull(evidence);
    }
    @Override public SqlEffectsSnapshot read(long orderId) throws SQLException {
        long start = System.nanoTime();
        var rows = new ArrayList<SqlEffectsSnapshot.Reservation>();
        try (var connection = source.getConnection(); var query = connection.prepareStatement(SELECT)) {
            query.setLong(1, orderId);
            try (var result = query.executeQuery()) {
                while (result.next()) rows.add(new SqlEffectsSnapshot.Reservation(result.getLong("reservation_id"),
                        result.getLong("order_id"), result.getBigDecimal("amount"), result.getString("state")));
            }
        } catch (SQLException | RuntimeException | Error original) {
            var snapshot = new SqlEffectsSnapshot(orderId, witnessId, provenance, rows, elapsed(start),
                    Optional.of(SqlEffectsSnapshot.Failure.from(original)));
            try { evidence.accept(snapshot); }
            catch (RuntimeException | Error secondary) { if (secondary != original) original.addSuppressed(secondary); }
            throw original;
        }
        var snapshot = new SqlEffectsSnapshot(orderId, witnessId, provenance, rows, elapsed(start), Optional.empty());
        // A sink failure after success cannot enter the SQL catch or replay the SELECT.
        evidence.accept(snapshot);
        return snapshot;
    }
    private static Duration elapsed(long start) { return Duration.ofNanos(Math.max(0, System.nanoTime() - start)); }
}
