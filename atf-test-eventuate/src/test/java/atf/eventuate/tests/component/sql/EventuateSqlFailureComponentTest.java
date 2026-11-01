package atf.eventuate.tests.component.sql;

import atf.eventuate.evidence.EventuateEvidence;
import atf.eventuate.sql.*;
import atf.eventuate.tests.support.EventuateComponentSupport;
import atf.eventuate.tests.support.EventuateComponentTest;
import atf.eventuate.tests.support.sql.*;
import atf.testkit.assertions.RecordedAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@EventuateComponentTest
public class EventuateSqlFailureComponentTest extends EventuateComponentSupport {
    @Test @DisplayName("SQL embedded · original failure, partial rows and cleanup")
    void preservesSqlFailureAndNeverPromotesPartialEvidenceToPass(RecordedAssertions checks, EventuateEvidence evidence) throws Exception {
        try (var absent = new H2SqlFixture(false)) {
            var original = SqlTestChecks.failure(checks, "Missing schema raises a real JDBC failure", () -> absent.reader(evidence).read(42));
            checks.isTrue("Vendor SQL state remains available", original.getSQLState() != null);
            checks.isFalse("Failed query is not an empty successful result", evidence.sql().getLast().complete());
        }
        try (var db = new H2SqlFixture()) {
            db.reserve(1, 42, "70.00", "APPROVED"); db.reserve(2, 42, "70.00", "APPROVED"); db.commit();
            var expected = new ReservationEffectContract(42, new BigDecimal("70.00"), "APPROVED");
            for (var point : List.of(JdbcFaultSource.Point.QUERY, JdbcFaultSource.Point.PARTIAL, JdbcFaultSource.Point.CLOSE)) {
                var cause = new java.io.IOException("controlled JDBC cause; password=SQL_WITNESS_SECRET");
                var original = new SQLException("controlled JDBC " + point, "45000", 901, cause);
                var cleanup = new SQLException("controlled JDBC cleanup", "45001", 902);
                var source = new JdbcFaultSource(db.source(), point, original, point == JdbcFaultSource.Point.CLOSE ? null : cleanup);
                var callbacks = new AtomicInteger(); var sink = new IllegalStateException("controlled SQL sink");
                var reader = new JdbcOrderEffectsReader(source, db.id(), SqlEffectsSnapshot.Provenance.SQL_EMBEDDED, captured -> {
                    callbacks.incrementAndGet(); evidence.recordSql(captured);
                    if (point == JdbcFaultSource.Point.QUERY) throw sink;
                });
                var caught = SqlTestChecks.failure(checks, point + " raises original SQLException", () -> reader.read(42));
                var captured = evidence.sql().getLast();
                checks.isTrue(point + " preserves exception identity", caught == original);
                checks.isTrue(point + " preserves cause identity", caught.getCause() == cause);
                checks.equals(point + " opens exactly one witness connection", 1, source.opens());
                checks.equals(point + " executes no replay", 1, source.queries());
                checks.equals(point + " closes the actual connection", 1, source.closes());
                checks.equals(point + " publishes one snapshot", 1, callbacks.get());
                checks.equals(point + " retains partial rows", point == JdbcFaultSource.Point.QUERY ? 0 : point == JdbcFaultSource.Point.PARTIAL ? 1 : 2, captured.reservations().size());
                checks.isFalse(point + " failed read cannot pass the effect contract", expected.evaluate(captured).passed());
                checks.equals(point + " vendor code preserved", 901, captured.failure().orElseThrow().vendorCode());
                if (point != JdbcFaultSource.Point.CLOSE) checks.isTrue(point + " cleanup stays suppressed", List.of(caught.getSuppressed()).contains(cleanup));
                if (point == JdbcFaultSource.Point.QUERY) checks.isTrue("Sink error stays secondary", List.of(caught.getSuppressed()).contains(sink));
            }
            var snapshots = new AtomicInteger(); var sink = new IllegalStateException("post-success sink");
            var successful = new JdbcFaultSource(db.source(), JdbcFaultSource.Point.NONE, null, null);
            var reader = new JdbcOrderEffectsReader(successful, db.id(), SqlEffectsSnapshot.Provenance.SQL_EMBEDDED, captured -> {
                snapshots.incrementAndGet(); evidence.recordSql(captured); throw sink;
            });
            RuntimeException caught = null;
            try { reader.read(42); } catch (RuntimeException original) { caught = original; }
            checks.isTrue("Successful read preserves the failing sink", caught == sink);
            checks.equals("Successful read does not replay publication", 1, snapshots.get());
            checks.equals("Successful read executes one SELECT", 1, successful.queries());
            checks.equals("Successful read closes its connection", 1, successful.closes());
            checks.isTrue("Sink error does not reclassify completed SQL", evidence.sql().getLast().complete());
            if (System.getProperty("atf.eventuate.sql.probe", "none").equals("sql-error")) {
                var failure = new SQLException("controlled unhandled JDBC failure", "45000", 903,
                        new java.io.IOException("original unhandled SQL cause; password=SQL_WITNESS_SECRET"));
                SqlReportProbe.capture(failure);
                new JdbcOrderEffectsReader(new JdbcFaultSource(db.source(), JdbcFaultSource.Point.QUERY, failure, null),
                        db.id(), SqlEffectsSnapshot.Provenance.SQL_EMBEDDED, evidence::recordSql).read(42);
            }
        }
    }
}
