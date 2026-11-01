package atf.eventuate.tests.support.sql;

import atf.testkit.assertions.RecordedAssertions;
import java.sql.SQLException;

/** Records expected JDBC failures through the same audited assertion port. */
public final class SqlTestChecks {
    private SqlTestChecks() { }
    @FunctionalInterface public interface Operation { void run() throws SQLException; }
    public static SQLException failure(RecordedAssertions checks, String label, Operation operation) {
        SQLException caught = null;
        try { operation.run(); } catch (SQLException original) { caught = original; }
        checks.isTrue(label, caught != null);
        return caught;
    }
}
