package atf.sql.query;
import atf.sql.value.SqlValues;

import java.sql.JDBCType;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

/** Ordered, typed bind value; mutable JDBC objects are excluded from captured evidence. */
public record SqlParameter(JDBCType type, Object value, boolean sensitive) {
    public SqlParameter {
        Objects.requireNonNull(type, "type");
        value = SqlValues.parameter(value);
    }
    public SqlParameter(JDBCType type, Object value) { this(type, value, false); }
    @Override public Object value() { return value instanceof byte[] bytes ? bytes.clone() : value; }
    public void bind(PreparedStatement statement, int index) throws SQLException {
        if (value == null) statement.setNull(index, type.getVendorTypeNumber());
        else if (value instanceof byte[] bytes) statement.setObject(index, bytes.clone(), type.getVendorTypeNumber());
        else statement.setObject(index, value instanceof BigInteger number ? new BigDecimal(number) : value, type.getVendorTypeNumber());
    }
    public long captureUnits() { return SqlValues.units(value); }
    @Override public String toString() { return "SqlParameter[type=" + type + ", sensitive=" + sensitive + ", value=captured]"; }
    @Override public boolean equals(Object other) {
        return other instanceof SqlParameter parameter && type == parameter.type && sensitive == parameter.sensitive
                && java.util.Objects.deepEquals(value, parameter.value);
    }
    @Override public int hashCode() { return java.util.Objects.hash(type, sensitive, value instanceof byte[] bytes ? java.util.Arrays.hashCode(bytes) : value); }
}
