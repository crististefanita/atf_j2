package atf.sql.execution;

/** JDBC capture limits; these are independent from Reporting HTML display budgets. */
public record SqlCaptureLimits(int rows, int columns, int textCharacters, int binaryBytes, long payloadUnits) {
    public SqlCaptureLimits {
        if (rows < 1 || rows > 100000 || columns < 1 || columns > 1024 || textCharacters < 1
                || textCharacters > 1048576 || binaryBytes < 1 || binaryBytes > 1048576 || payloadUnits < 1 || payloadUnits > 16777216)
            throw new IllegalArgumentException("Positive, bounded SQL capture limits required");
    }
    public SqlCaptureLimits(int rows, int columns, int textCharacters, int binaryBytes) { this(rows, columns, textCharacters, binaryBytes, 4194304); }
    public static SqlCaptureLimits defaults() { return new SqlCaptureLimits(1000, 128, 16384, 16384, 4194304); }
}
