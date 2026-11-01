package atf.sql.result;
import atf.sql.value.SqlValues;

import java.math.BigDecimal;
import java.math.BigInteger;

/** SQL NULL, text, binary and exact scalar values remain distinct; partial capture is explicit. */
public record SqlValue(Object value, boolean complete) {
    public SqlValue {
        value = SqlValues.snapshot(value);
    }
    @Override public Object value() { return value instanceof byte[] bytes ? bytes.clone() : value; }
    @Override public String toString() { return "SqlValue[type=" + (value == null ? "SQL_NULL" : value.getClass().getSimpleName()) + ", complete=" + complete + "]"; }
    @Override public boolean equals(Object other) { return other instanceof SqlValue cell && complete == cell.complete && java.util.Objects.deepEquals(value, cell.value); }
    @Override public int hashCode() { return java.util.Objects.hash(complete, value instanceof byte[] bytes ? java.util.Arrays.hashCode(bytes) : value); }
}
