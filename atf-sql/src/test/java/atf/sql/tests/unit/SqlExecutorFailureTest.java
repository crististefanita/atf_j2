package atf.sql.tests.unit;

import atf.sql.execution.*;
import atf.sql.query.SqlQuery;
import atf.sql.result.SqlSnapshot;
import atf.sql.tests.support.JdbcFaults;
import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
class SqlExecutorFailureTest {
    @Test void aggregateBudgetAccountsForNullCellsAndRejectsOversizedInputBeforeDispatch() throws Exception {
        var jdbc = new JdbcFaults(); jdbc.nullColumns=3;
        try(var executor = new SqlExecutor(jdbc.connections(),new SqlCaptureLimits(10,8,64,64,256),SqlReadOnlyPolicy.REQUIRED,snapshot->{})) {
            assertThrows(java.sql.SQLDataException.class,()->executor.query(new SqlQuery("SELECT nulls"),Duration.ofSeconds(2)));
            assertEquals(1,jdbc.queries.get());
            assertThrows(IllegalArgumentException.class,()->executor.query(new SqlQuery("SELECT "+"x".repeat(300)),Duration.ofSeconds(2)));
            assertEquals(1,jdbc.opens.get());
        }
    }
    @Test void preservesOriginalExecutionAndBothCleanupFailures() throws Exception {
        var jdbc = new JdbcFaults(); jdbc.queryFailure = new SQLException("primary", "42000", 77);
        jdbc.statementCloseFailure = new SQLException("statement cleanup"); jdbc.connectionCloseFailure = new SQLException("connection cleanup");
        var captured = new ArrayList<SqlSnapshot>();
        try(var executor = new SqlExecutor(jdbc.connections(),captured::add)) {
            var original = assertThrows(SQLException.class,()->executor.query(new SqlQuery("SELECT id"),Duration.ofSeconds(2)));
            assertSame(jdbc.queryFailure,original); assertEquals(1,jdbc.queries.get()); assertEquals(1,jdbc.opens.get());
            assertSame(jdbc.statementCloseFailure,original.getSuppressed()[0]); assertSame(jdbc.connectionCloseFailure,original.getSuppressed()[1]);
            assertEquals(1,captured.size()); assertSame(captured.getFirst(),SqlExecutionFailure.find(original).orElseThrow());
            assertEquals("42000",captured.getFirst().failure().sqlState()); assertEquals(77,captured.getFirst().failure().vendorCode());
        }
    }
    @Test void publicationFailureDoesNotReclassifyOrReplaySuccessfulSql() throws Exception {
        var jdbc = new JdbcFaults(); var publication = new IllegalStateException("sink failed");
        var captured = new ArrayList<SqlSnapshot>();
        try(var executor = new SqlExecutor(jdbc.connections(),snapshot->{captured.add(snapshot);throw publication;})) {
            assertSame(publication,assertThrows(IllegalStateException.class,()->executor.query(new SqlQuery("SELECT id"),Duration.ofSeconds(2))));
            assertEquals(1,jdbc.queries.get()); assertEquals(SqlSnapshot.Outcome.COMPLETE,captured.getFirst().outcome());
            assertSame(captured.getFirst(),SqlExecutionFailure.find(publication).orElseThrow());
            assertEquals(SqlExecutionFailure.Origin.PUBLICATION,SqlExecutionFailure.origin(publication).orElseThrow());
        }
    }
    @Test void returnsAndPublishesTheSameImmutableExecutionSnapshot() throws Exception {
        var jdbc = new JdbcFaults(); var captured = new ArrayList<SqlSnapshot>();
        try(var executor = new SqlExecutor(jdbc.connections(),captured::add)) {
            var result = executor.query(new SqlQuery("SELECT id"),Duration.ofSeconds(2));
            assertSame(result,captured.getFirst()); assertEquals(1,jdbc.queries.get());
            assertThrows(UnsupportedOperationException.class,()->result.rows().clear());
        }
    }
    @Test void requiresReadOnlyAcknowledgementBeforePreparingAQuery() throws Exception {
        var jdbc = new JdbcFaults(); jdbc.readOnly=false;
        try(var executor = new SqlExecutor(jdbc.connections(),snapshot->{})) {
            assertThrows(SQLException.class,()->executor.query(new SqlQuery("SELECT id"),Duration.ofSeconds(2)));
            assertEquals(0,jdbc.queries.get()); assertEquals(1,jdbc.closes.get());
            assertThrows(IllegalArgumentException.class,()->executor.query(new SqlQuery("SELECT id"),Duration.ZERO));
            assertEquals(1,jdbc.opens.get());
        }
    }
    @Test void boundsBlockedJdbcAndPoisonsTheExecutorWithoutReplay() throws Exception {
        var jdbc = new JdbcFaults(); jdbc.block=true;
        try(var executor = new SqlExecutor(jdbc.connections(),snapshot->{})) {
            var timeout = assertThrows(SQLTimeoutException.class,()->executor.query(new SqlQuery("SELECT blocked"),Duration.ofMillis(100)));
            assertEquals(SqlSnapshot.Outcome.FAILED,SqlExecutionFailure.find(timeout).orElseThrow().outcome());
            assertTrue(jdbc.opens.get()<=1); assertTrue(jdbc.queries.get()<=1);
            if(jdbc.queries.get()==1) assertTrue(jdbc.interrupted.await(1,TimeUnit.SECONDS));
            assertThrows(IllegalStateException.class,()->executor.query(new SqlQuery("SELECT retry"),Duration.ofSeconds(2)));
        }
    }
}
