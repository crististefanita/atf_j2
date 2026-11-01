package atf.sql.tests.support;

import atf.sql.execution.SqlCaptureLimits;
import atf.sql.execution.SqlConnections;
import atf.sql.execution.SqlExecutor;
import atf.sql.execution.SqlReadOnlyPolicy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;

/** Writer fixture is independent of the executor; final close removes the uniquely owned database. */
public final class H2SqlDatabase implements AutoCloseable {
    private final String url = "jdbc:h2:mem:sql_" + UUID.randomUUID().toString().replace("-", "");
    private final Connection owner;
    public H2SqlDatabase() throws SQLException { owner = DriverManager.getConnection(url); }
    public Connection writer() throws SQLException { return DriverManager.getConnection(url + ";IFEXISTS=TRUE"); }
    public SqlExecutor reader(SqlTestEvidence evidence, SqlCaptureLimits limits) {
        // H2 does not acknowledge Connection.setReadOnly; this is explicit fixture policy, never the LIVE default.
        return new SqlExecutor(remaining -> writer(), limits, SqlReadOnlyPolicy.REQUESTED, evidence::record);
    }
    public void execute(String ddl) throws SQLException { try (var statement = owner.createStatement()) { statement.execute(ddl); } }
    @Override public void close() throws SQLException { owner.close(); }
}
