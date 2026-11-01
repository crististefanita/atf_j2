package atf.sql.tests.component;

import atf.sql.tests.support.SqlComponentTest;
import atf.sql.tests.support.SqlTestEvidence;
import atf.sql.tests.support.H2SqlDatabase;
import atf.sql.execution.SqlCaptureLimits;
import atf.sql.query.SqlQuery;
import atf.sql.query.SqlParameter;
import atf.testkit.assertions.RecordedAssertions;
import java.sql.JDBCType;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

@SqlComponentTest
public class SqlResultComponentTest {
    @Test void capturesTypedValuesDuplicateLabelsAndRows(RecordedAssertions checks, SqlTestEvidence evidence) throws Exception {
        try (var database = new H2SqlDatabase(); var reader = database.reader(evidence, SqlCaptureLimits.defaults())) {
            var snapshot = reader.query(new SqlQuery("SELECT CAST(? AS DECIMAL(30,2)) AS same, CAST(? AS VARCHAR) AS same, CAST(NULL AS VARCHAR) AS missing, '' AS empty",
                    List.of(new SqlParameter(JDBCType.DECIMAL, new BigDecimal("9007199254740993.01")), new SqlParameter(JDBCType.VARCHAR, "null"))), Duration.ofSeconds(2));
            checks.isTrue("Complete JDBC result", snapshot.complete());
            checks.equals("Four ordered columns", 4, snapshot.columns().size());
            checks.equals("Duplicate labels retained", snapshot.columns().get(0).label(), snapshot.columns().get(1).label());
            checks.equals("Exact decimal beyond Double precision", new BigDecimal("9007199254740993.01"), snapshot.rows().getFirst().get(0).value());
            checks.equals("Literal null text retained", "null", snapshot.rows().getFirst().get(1).value());
            checks.equals("SQL NULL retained", null, snapshot.rows().getFirst().get(2).value());
            checks.equals("Empty text retained", "", snapshot.rows().getFirst().get(3).value());
            checks.isTrue("Resources closed before publication", snapshot.resourcesClosed());
            checks.isFalse("H2 read-only acknowledgement is not invented", snapshot.readOnlyAcknowledged());
            var duplicate = reader.query(new SqlQuery("SELECT * FROM (VALUES (1), (1))"), Duration.ofSeconds(2));
            checks.equals("Duplicate rows retained", 2, duplicate.rows().size());
            checks.equals("Two distinct execution identities", 2L, evidence.snapshots().stream().map(value -> value.executionId()).distinct().count());
        }
    }
}
