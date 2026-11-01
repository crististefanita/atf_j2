package atf.sql.tests.support;

import atf.sql.result.SqlSnapshot;
import java.util.ArrayList;
import java.util.List;

/** Invocation-owned test collector; no JDBC or renderer callback. */
public final class SqlTestEvidence {
    private final List<SqlSnapshot> captured = new ArrayList<>();
    public void record(SqlSnapshot snapshot) { captured.add(snapshot); }
    public List<SqlSnapshot> snapshots() { return List.copyOf(captured); }
}
