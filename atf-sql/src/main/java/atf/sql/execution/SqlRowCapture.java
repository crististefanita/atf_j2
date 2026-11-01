package atf.sql.execution;

import atf.sql.result.SqlValue;
import atf.sql.value.SqlValues;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Bounded LOB/text reads; driver-private objects never escape the JDBC resource scope. */
final class SqlRowCapture {
    @FunctionalInterface interface Budget { void account(long units) throws SQLException; }
    private SqlRowCapture() { }
    static List<SqlValue> read(ResultSet result, int columns, SqlCaptureLimits limits, Budget budget) throws SQLException {
        var row = new ArrayList<SqlValue>();
        for (int index = 1; index <= columns; index++) {
            int type = result.getMetaData().getColumnType(index);
            if (List.of(Types.CHAR, Types.VARCHAR, Types.LONGVARCHAR, Types.NCHAR, Types.NVARCHAR, Types.LONGNVARCHAR, Types.CLOB, Types.NCLOB).contains(type)) {
                try (var reader = result.getCharacterStream(index)) {
                    if (reader == null) { budget.account(SqlValues.units(null)); row.add(new SqlValue(null, true)); continue; }
                    var value = new StringBuilder();
                    int next;
                    while (value.length() <= limits.textCharacters() && (next = reader.read()) != -1) value.append((char) next);
                    boolean complete = value.length() <= limits.textCharacters();
                    if (!complete) value.setLength(limits.textCharacters());
                    if (!value.isEmpty() && Character.isHighSurrogate(value.charAt(value.length() - 1))) { value.setLength(value.length() - 1); complete = false; }
                    budget.account(SqlValues.units(value.toString())); row.add(new SqlValue(value.toString(), complete));
                } catch (IOException failure) { throw new java.sql.SQLDataException("Cannot capture SQL text", failure); }
            } else if (List.of(Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY, Types.BLOB).contains(type)) {
                try (var stream = result.getBinaryStream(index)) {
                    if (stream == null) { budget.account(SqlValues.units(null)); row.add(new SqlValue(null, true)); continue; }
                    byte[] bytes = stream.readNBytes(limits.binaryBytes() + 1);
                    boolean complete = bytes.length <= limits.binaryBytes();
                    budget.account(64L + Math.min(bytes.length, limits.binaryBytes()));
                    row.add(new SqlValue(complete ? bytes : java.util.Arrays.copyOf(bytes, limits.binaryBytes()), complete));
                } catch (IOException failure) { throw new java.sql.SQLDataException("Cannot capture SQL binary", failure); }
            } else {
                Object value = result.getObject(index);
                if (value instanceof java.sql.Date date) value = date.toLocalDate();
                else if (value instanceof java.sql.Time time) value = time.toLocalTime();
                else if (value instanceof java.sql.Timestamp timestamp) value = timestamp.toLocalDateTime();
                budget.account(SqlValues.units(value));
                try { row.add(new SqlValue(value, true)); }
                catch (IllegalArgumentException unsupported) { throw new java.sql.SQLFeatureNotSupportedException("Unsupported SQL value at column " + index, unsupported); }
            }
        }
        return List.copyOf(row);
    }
}
