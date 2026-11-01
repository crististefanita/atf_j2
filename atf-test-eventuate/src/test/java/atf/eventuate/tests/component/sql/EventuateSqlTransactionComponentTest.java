package atf.eventuate.tests.component.sql;

import atf.eventuate.evidence.EventuateEvidence;
import atf.eventuate.sql.ReservationEffectContract;
import atf.eventuate.tests.support.EventuateComponentSupport;
import atf.eventuate.tests.support.EventuateComponentTest;
import atf.eventuate.tests.support.sql.H2SqlFixture;
import atf.eventuate.tests.support.sql.SqlTestChecks;
import atf.eventuate.tests.support.sql.JdbcFaultSource;
import atf.eventuate.sql.JdbcOrderEffectsReader;
import atf.eventuate.sql.SqlEffectsSnapshot;
import atf.testkit.assertions.RecordedAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;

@EventuateComponentTest
public class EventuateSqlTransactionComponentTest extends EventuateComponentSupport {
    @Test @DisplayName("SQL embedded · independent commit and rollback witness")
    void readsCommittedEffectsThroughIndependentConnections(RecordedAssertions checks, EventuateEvidence evidence) throws Exception {
        var db = new H2SqlFixture();
        try (db) {
            var source = new JdbcFaultSource(db.source(), JdbcFaultSource.Point.NONE, null, null);
            var reader = new JdbcOrderEffectsReader(source, db.id(), SqlEffectsSnapshot.Provenance.SQL_EMBEDDED, evidence::recordSql);
            var expected = new ReservationEffectContract(42, new BigDecimal("70.00"), "APPROVED");
            try (var independent = db.source().getConnection()) {
                checks.isFalse("Writer and witness use different JDBC connections", independent == db.writer());
                checks.equals("Witness isolation", java.sql.Connection.TRANSACTION_READ_COMMITTED, independent.getTransactionIsolation());
            }
            checks.equals("New database has no reservation", 0, reader.read(42).reservations().size());
            db.reserve(1, 42, "70.00", "APPROVED");
            checks.equals("Uncommitted reservation remains invisible", 0, reader.read(42).reservations().size());
            db.commit();
            var committed = reader.read(42);
            checks.equals("SQL provenance is explicit", "SQL_EMBEDDED", committed.provenance().name());
            checks.equals("Committed reservation count", 1, committed.reservations().size());
            checks.equals("Committed amount", new BigDecimal("70.00"), committed.reservations().getFirst().amount());
            checks.isTrue("Independent committed effect matches fixed expected", expected.evaluate(committed).passed());
            db.reserve(2, 42, "70.00", "APPROVED");
            checks.equals("Second uncommitted effect remains invisible", 1, reader.read(42).reservations().size());
            db.rollback();
            checks.isTrue("Rollback preserves exactly one committed effect", expected.evaluate(reader.read(42)).passed());
            checks.equals("Each SELECT captured once", 5, evidence.sql().size());
            checks.equals("Each read opens a fresh connection", 5, source.opens());
            checks.equals("All witness connections are closed", 5, source.closes());
            checks.equals("Exactly five SELECT executions", 5, source.queries());
        }
        SqlTestChecks.failure(checks, "Owner close removes the in-memory database", () -> db.source().getConnection().close());
    }
}
