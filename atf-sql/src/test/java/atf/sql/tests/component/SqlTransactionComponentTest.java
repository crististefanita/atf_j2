package atf.sql.tests.component;

import atf.sql.tests.support.SqlComponentTest;
import atf.sql.tests.support.SqlTestEvidence;
import atf.sql.tests.support.H2SqlDatabase;
import atf.sql.execution.SqlCaptureLimits;
import atf.sql.query.SqlQuery;
import atf.testkit.assertions.RecordedAssertions;
import java.time.Duration;
import org.junit.jupiter.api.Test;

@SqlComponentTest
public class SqlTransactionComponentTest {
    @Test void witnessesCommitAndRollbackFromIndependentConnections(RecordedAssertions checks, SqlTestEvidence evidence) throws Exception {
        try (var database = new H2SqlDatabase(); var writer = database.writer(); var reader = database.reader(evidence, SqlCaptureLimits.defaults())) {
            database.execute("CREATE TABLE effects(id BIGINT PRIMARY KEY)"); writer.setAutoCommit(false);
            try (var statement = writer.createStatement()) { statement.executeUpdate("INSERT INTO effects VALUES(42)"); }
            var query = new SqlQuery("SELECT id FROM effects ORDER BY id");
            checks.equals("Uncommitted writer is invisible", 0, reader.query(query, Duration.ofSeconds(2)).requireComplete().rows().size());
            writer.commit();
            checks.equals("Commit is independently visible", 1, reader.query(query, Duration.ofSeconds(2)).requireComplete().rows().size());
            try (var statement = writer.createStatement()) { statement.executeUpdate("INSERT INTO effects VALUES(99)"); }
            writer.rollback();
            var actual = reader.query(query, Duration.ofSeconds(2)).requireComplete();
            checks.equals("Rollback does not leak another effect", 1, actual.rows().size());
            checks.equals("Original identity retained", 42L, actual.rows().getFirst().getFirst().value());
            checks.equals("Three read executions", 3, evidence.snapshots().size());
        }
    }
}
