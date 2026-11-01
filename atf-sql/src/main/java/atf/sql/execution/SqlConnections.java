package atf.sql.execution;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import javax.sql.DataSource;

/** Each execution obtains an independent connection; implementations configure driver connect/socket budgets. */
@FunctionalInterface
public interface SqlConnections {
    Connection open(Duration remaining) throws SQLException;
    static SqlConnections from(DataSource source) {
        java.util.Objects.requireNonNull(source);
        return remaining -> source.getConnection();
    }
}
