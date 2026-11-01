package atf.sql.tests.unit;

import atf.sql.query.SqlParameter;
import atf.sql.query.SqlQuery;
import atf.sql.result.SqlValue;
import atf.sql.result.SqlSnapshot;
import atf.sql.execution.SqlCaptureLimits;
import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;
import java.sql.JDBCType;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
class SqlQueryContractTest {
    @Test void parametersAreTypedOrderedAndDefensivelyCopied() {
        byte[] bytes = {1, 2}; var parameter = new SqlParameter(JDBCType.VARBINARY, bytes); bytes[0] = 9;
        assertArrayEquals(new byte[]{1,2}, (byte[]) parameter.value());
        assertEquals(parameter,new SqlParameter(JDBCType.VARBINARY,new byte[]{1,2}));
        assertEquals(new SqlValue(new byte[]{1,2},true),new SqlValue(new byte[]{1,2},true));
        ((byte[]) parameter.value())[0] = 7; assertArrayEquals(new byte[]{1,2}, (byte[]) parameter.value());
        var parameters = new ArrayList<>(List.of(parameter)); var query = new SqlQuery("SELECT ?", parameters); parameters.clear();
        assertEquals(1, query.parameters().size());
        assertThrows(UnsupportedOperationException.class, () -> query.parameters().clear());
        assertFalse(new SqlParameter(JDBCType.VARCHAR, "private-token", true).toString().contains("private-token"));
        assertThrows(IllegalArgumentException.class, () -> new SqlParameter(JDBCType.TIMESTAMP, new java.util.Date()));
        assertThrows(IllegalArgumentException.class, () -> new SqlParameter(JDBCType.DOUBLE, Double.NaN));
    }
    @Test void invalidConfigurationFailsBeforeDispatch() {
        assertThrows(IllegalArgumentException.class, () -> new SqlQuery(" "));
        assertThrows(IllegalArgumentException.class, () -> new SqlCaptureLimits(0,1,1,1));
        assertThrows(IllegalArgumentException.class, () -> new SqlCaptureLimits(1,1,1,1,Long.MAX_VALUE));
        assertThrows(NullPointerException.class, () -> new SqlParameter(null, "value"));
    }
    @Test void forgedCompleteSnapshotsAreRejected() {
        var columns = List.of(new SqlSnapshot.Column(1,"value",JDBCType.VARCHAR.getVendorTypeNumber(),"VARCHAR"));
        var rows = List.of(List.of(new SqlValue("truncated",false)));
        assertThrows(IllegalArgumentException.class, () -> new SqlSnapshot("id",new SqlQuery("SELECT 1"),columns,rows,
                SqlSnapshot.Outcome.COMPLETE,false,true,true,Duration.ZERO,null));
        assertThrows(IllegalArgumentException.class, () -> new SqlSnapshot("id",new SqlQuery("SELECT 1"),columns,List.of(),
                SqlSnapshot.Outcome.COMPLETE,true,true,true,Duration.ZERO,null));
        assertThrows(IllegalArgumentException.class, () -> new SqlSnapshot("id",new SqlQuery("SELECT 1"),columns,List.of(),
                SqlSnapshot.Outcome.COMPLETE,false,true,false,Duration.ZERO,null));
    }
}
