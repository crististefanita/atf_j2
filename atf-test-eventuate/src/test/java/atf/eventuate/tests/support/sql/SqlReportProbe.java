package atf.eventuate.tests.support.sql;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicReference;

/** Serial acceptance-only failure capture; never a business singleton. */
public final class SqlReportProbe {
    private static final AtomicReference<SQLException> FAILURE = new AtomicReference<>();
    private SqlReportProbe() { }
    public static void capture(SQLException original) { FAILURE.set(original); }
    public static SQLException consume() { return FAILURE.getAndSet(null); }
}
