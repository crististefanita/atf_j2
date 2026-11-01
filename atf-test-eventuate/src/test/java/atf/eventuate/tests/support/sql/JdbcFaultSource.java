package atf.eventuate.tests.support.sql;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.logging.Logger;

/** Faults at real JDBC boundaries; delegated resources are always closed. */
public final class JdbcFaultSource implements DataSource {
    public enum Point { NONE, QUERY, PARTIAL, CLOSE }
    private final DataSource delegate;
    private final Point point;
    private final SQLException original, cleanup;
    private int opens, queries, closes;
    public JdbcFaultSource(DataSource delegate, Point point, SQLException original, SQLException cleanup) {
        this.delegate = delegate; this.point = point; this.original = original; this.cleanup = cleanup;
    }
    public int opens() { return opens; }
    public int queries() { return queries; }
    public int closes() { return closes; }
    @Override public Connection getConnection() throws SQLException { opens++; return connection(delegate.getConnection()); }
    private Connection connection(Connection connection) {
        return (Connection) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Connection.class}, (proxy, method, args) -> {
            if (method.getName().equals("close")) {
                closes++; connection.close();
                if (point == Point.CLOSE) throw original;
                if (cleanup != null) throw cleanup;
                return null;
            }
            Object value = invoke(connection, method, args);
            return value instanceof PreparedStatement statement ? statement(statement) : value;
        });
    }
    private PreparedStatement statement(PreparedStatement statement) {
        return (PreparedStatement) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{PreparedStatement.class}, (proxy, method, args) -> {
            if (method.getName().equals("executeQuery")) {
                queries++;
                if (point == Point.QUERY) throw original;
                var result = (ResultSet) invoke(statement, method, args);
                if (point != Point.PARTIAL) return result;
                int[] next = {0};
                return Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{ResultSet.class}, (p, m, a) -> {
                    if (m.getName().equals("next") && ++next[0] == 2) throw original;
                    return invoke(result, m, a);
                });
            }
            return invoke(statement, method, args);
        });
    }
    private static Object invoke(Object target, Method method, Object[] args) throws Throwable {
        try { return method.invoke(target, args); }
        catch (InvocationTargetException wrapper) { throw wrapper.getCause(); }
    }
    @Override public Connection getConnection(String user, String password) throws SQLException { throw new SQLFeatureNotSupportedException("Explicit source only"); }
    @Override public PrintWriter getLogWriter() throws SQLException { return delegate.getLogWriter(); }
    @Override public void setLogWriter(PrintWriter writer) throws SQLException { delegate.setLogWriter(writer); }
    @Override public void setLoginTimeout(int seconds) throws SQLException { delegate.setLoginTimeout(seconds); }
    @Override public int getLoginTimeout() throws SQLException { return delegate.getLoginTimeout(); }
    @Override public Logger getParentLogger() throws SQLFeatureNotSupportedException { return delegate.getParentLogger(); }
    @Override public <T> T unwrap(Class<T> type) throws SQLException { return delegate.unwrap(type); }
    @Override public boolean isWrapperFor(Class<?> type) throws SQLException { return delegate.isWrapperFor(type); }
}
