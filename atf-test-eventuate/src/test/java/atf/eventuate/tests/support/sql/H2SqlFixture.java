package atf.eventuate.tests.support.sql;

import atf.eventuate.evidence.EventuateEvidence;
import atf.eventuate.sql.JdbcOrderEffectsReader;
import atf.eventuate.sql.SqlEffectsSnapshot;
import org.h2.jdbcx.JdbcDataSource;
import javax.sql.DataSource;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

/** Owns one named in-memory DB and its writer; readers use distinct IFEXISTS connections. */
public final class H2SqlFixture implements AutoCloseable {
    private final String id = "atf_j2_" + UUID.randomUUID().toString().replace("-", "");
    private final JdbcDataSource readers = new JdbcDataSource();
    private final Connection writer;
    private boolean closed;
    public H2SqlFixture() throws Exception { this(true); }
    public H2SqlFixture(boolean schema) throws Exception {
        var owner = new JdbcDataSource();
        String url = "jdbc:h2:mem:" + id;
        owner.setURL(url); owner.setUser("sa"); owner.setPassword("fixture-only");
        readers.setURL(url + ";IFEXISTS=TRUE"); readers.setUser("sa"); readers.setPassword("fixture-only");
        writer = owner.getConnection();
        try {
            if (schema) try (var resource = H2SqlFixture.class.getResourceAsStream("/sql/reservations.sql");
                            var statement = writer.createStatement()) {
                if (resource == null) throw new IllegalStateException("SQL schema fixture missing");
                statement.execute(new String(resource.readAllBytes(), StandardCharsets.UTF_8));
            }
            writer.setAutoCommit(false);
        } catch (Exception | Error original) {
            try { writer.close(); } catch (SQLException cleanup) { original.addSuppressed(cleanup); }
            throw original;
        }
    }
    public String id() { return id; }
    public Connection writer() { return writer; }
    public DataSource source() { return readers; }
    public JdbcOrderEffectsReader reader(EventuateEvidence evidence) {
        return new JdbcOrderEffectsReader(readers, id, SqlEffectsSnapshot.Provenance.SQL_EMBEDDED, evidence::recordSql);
    }
    public void reserve(long reservationId, long orderId, String amount, String state) throws SQLException {
        try (var statement = writer.prepareStatement("INSERT INTO atf_order_reservations VALUES (?, ?, ?, ?)")) {
            statement.setLong(1, reservationId); statement.setLong(2, orderId);
            statement.setBigDecimal(3, new BigDecimal(amount)); statement.setString(4, state); statement.executeUpdate();
        }
    }
    public void commit() throws SQLException { writer.commit(); }
    public void rollback() throws SQLException { writer.rollback(); }
    @Override public void close() throws SQLException {
        if (closed) return;
        try (writer) { writer.rollback(); } finally { closed = true; }
    }
}
