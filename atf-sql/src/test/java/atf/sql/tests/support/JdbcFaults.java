package atf.sql.tests.support;

import atf.sql.execution.SqlConnections;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/** Pure JDBC boundary double; every unconfigured call fails instead of manufacturing success. */
public final class JdbcFaults {
    public final AtomicInteger opens = new AtomicInteger(), queries = new AtomicInteger(), closes = new AtomicInteger();
    public final CountDownLatch entered = new CountDownLatch(1), interrupted = new CountDownLatch(1);
    public SQLException queryFailure, statementCloseFailure, connectionCloseFailure;
    public boolean readOnly = true, block;
    public int nullColumns;
    public SqlConnections connections() { return remaining -> { opens.incrementAndGet(); return connection(); }; }
    private Connection connection() {
        return proxy(Connection.class, (method,args) -> switch(method) {
            case "setReadOnly" -> null;
            case "isReadOnly" -> readOnly;
            case "prepareStatement" -> statement();
            case "abort" -> null;
            case "close" -> { closes.incrementAndGet(); if(connectionCloseFailure!=null)throw connectionCloseFailure; yield null; }
            default -> throw new UnsupportedOperationException(method);
        });
    }
    private PreparedStatement statement() {
        return proxy(PreparedStatement.class, (method,args) -> switch(method) {
            case "setQueryTimeout","setObject","setNull","setBytes","setMaxRows","setFetchSize" -> null;
            case "executeQuery" -> {
                queries.incrementAndGet(); entered.countDown();
                if(block) try { new CountDownLatch(1).await(); }
                catch(InterruptedException failure){interrupted.countDown();Thread.currentThread().interrupt();throw new SQLException("JDBC interrupted",failure);}
                if(queryFailure!=null)throw queryFailure;
                yield rows();
            }
            case "close" -> { if(statementCloseFailure!=null)throw statementCloseFailure; yield null; }
            default -> throw new UnsupportedOperationException(method);
        });
    }
    private ResultSet rows() {
        boolean[] returned = {false};
        return proxy(ResultSet.class, (method,args) -> switch(method) {
            case "next" -> { boolean result = nullColumns > 0 && !returned[0]; returned[0] = true; yield result; }
            case "getCharacterStream" -> null;
            case "getMetaData" -> proxy(ResultSetMetaData.class,(name,ignored) -> switch(name){
                case "getColumnCount" -> nullColumns > 0 ? nullColumns : 1; case "getColumnLabel" -> "v"+ignored[0];
                case "getColumnType" -> nullColumns > 0 ? Types.VARCHAR : Types.BIGINT;
                case "getColumnTypeName" -> nullColumns > 0 ? "VARCHAR" : "BIGINT"; default -> throw new UnsupportedOperationException(name);});
            case "close" -> null;
            default -> throw new UnsupportedOperationException(method);
        });
    }
    @FunctionalInterface private interface Call { Object invoke(String method,Object[] args) throws Throwable; }
    private static <T> T proxy(Class<T> type,Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},(target,method,args)->call.invoke(method.getName(),args)));
    }
}
