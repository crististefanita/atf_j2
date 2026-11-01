package atf.sql.tests.component;

import atf.sql.tests.support.SqlComponentTest;
import atf.sql.tests.support.SqlTestEvidence;
import atf.sql.tests.support.H2SqlDatabase;
import atf.sql.execution.SqlCaptureLimits;
import atf.sql.execution.SqlExecutionFailure;
import atf.sql.query.SqlQuery;
import atf.sql.result.SqlSnapshot;
import atf.testkit.assertions.RecordedAssertions;
import java.time.Duration;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;

@SqlComponentTest
public class SqlCaptureFailureComponentTest {
    @Test void rejectsAggregateCaptureOverflowWithoutReturningACompleteResult(RecordedAssertions checks, SqlTestEvidence evidence) throws Exception {
        try (var database = new H2SqlDatabase(); var reader = database.reader(evidence, new SqlCaptureLimits(10,8,100,100,80))) {
            SQLException failure = null;
            try { reader.query(new SqlQuery("SELECT 'abcdefghijklmnopqrstuvwxyz0123456789' AS value_text"),Duration.ofSeconds(2)); }
            catch(SQLException original) { failure=original; }
            checks.isTrue("Aggregate budget exhaustion is a JDBC data failure",failure instanceof java.sql.SQLDataException);
            checks.isTrue("Failure identifies the aggregate capture budget",failure.getMessage().contains("Aggregate SQL capture budget"));
            var snapshot=SqlExecutionFailure.find(failure).orElseThrow();
            checks.equals("Overflow cannot become COMPLETE",SqlSnapshot.Outcome.FAILED,snapshot.outcome());
            checks.equals("Exactly one terminal capture",1,evidence.snapshots().size());
        }
    }
    @Test void capturesBinaryAndTemporalValuesWithoutLeakingMutableJdbcObjects(RecordedAssertions checks, SqlTestEvidence evidence) throws Exception {
        try (var database = new H2SqlDatabase(); var reader = database.reader(evidence,new SqlCaptureLimits(10,8,2,2))) {
            var snapshot=reader.query(new SqlQuery("SELECT X'010203' AS raw_bytes, DATE '2026-11-01' AS day_value"),Duration.ofSeconds(2));
            checks.equals("Binary truncation is incomplete",SqlSnapshot.Outcome.INCOMPLETE,snapshot.outcome());
            byte[] bytes=(byte[])snapshot.rows().getFirst().getFirst().value();
            checks.isTrue("Bounded binary prefix retained",java.util.Arrays.equals(new byte[]{1,2},bytes)); bytes[0]=9;
            checks.isTrue("Captured binary is immutable",java.util.Arrays.equals(new byte[]{1,2},(byte[])snapshot.rows().getFirst().getFirst().value()));
            checks.equals("JDBC date becomes immutable LocalDate",java.time.LocalDate.of(2026,11,1),snapshot.rows().getFirst().get(1).value());
        }
    }
    @Test void marksPartialCaptureAndPreservesSqlFailure(RecordedAssertions checks, SqlTestEvidence evidence) throws Exception {
        try (var database = new H2SqlDatabase(); var reader = database.reader(evidence, new SqlCaptureLimits(1, 8, 4, 4))) {
            var partial = reader.query(new SqlQuery("SELECT 'abcdefgh' AS content FROM (VALUES(1),(2))"), Duration.ofSeconds(2));
            checks.equals("Partial capture cannot claim COMPLETE", SqlSnapshot.Outcome.INCOMPLETE, partial.outcome());
            checks.isTrue("Additional rows are declared", partial.moreRows());
            checks.equals("Configured text prefix retained", "abcd", partial.rows().getFirst().getFirst().value());
            checks.isFalse("Cell truncation is explicit", partial.rows().getFirst().getFirst().complete());
            Throwable rejected = null;
            try { partial.requireComplete(); } catch (IllegalStateException failure) { rejected = failure; }
            checks.isTrue("Full-result assertion refuses partial data", rejected instanceof IllegalStateException);
            SQLException original = null;
            try { reader.query(new SqlQuery("SELECT * FROM absent_table"), Duration.ofSeconds(2)); }
            catch (SQLException failure) { original = failure; }
            checks.isTrue("Real JDBC failure is propagated", original != null);
            checks.isTrue("Missing table is a genuine JDBC syntax error", original instanceof java.sql.SQLSyntaxErrorException);
            checks.isTrue("Failure identifies the requested absent table",original.getMessage().toUpperCase(java.util.Locale.ROOT).contains("ABSENT_TABLE"));
            var terminal = SqlExecutionFailure.find(original).orElseThrow();
            checks.equals("Terminal SQL evidence is FAILED", SqlSnapshot.Outcome.FAILED, terminal.outcome());
            checks.equals("Original SQLState retained", original.getSQLState(), terminal.failure().sqlState());
            checks.equals("One capture per execution; no replay", 2, evidence.snapshots().size());
            if (Boolean.getBoolean("atf.sql.probe.unhandled")) throw original;
        }
    }
}
