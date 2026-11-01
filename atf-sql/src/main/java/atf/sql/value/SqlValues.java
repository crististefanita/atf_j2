package atf.sql.value;

import java.util.Set;
import java.math.BigDecimal;
import java.math.BigInteger;

/** Shared immutable-value and capture-cost policy; no JDBC objects or renderer formatting. */
public final class SqlValues {
    private static final Set<Class<?>> IMMUTABLE=Set.of(String.class,Boolean.class,Byte.class,Short.class,Integer.class,Long.class,
            Float.class,Double.class,BigInteger.class,BigDecimal.class,java.util.UUID.class,java.time.LocalDate.class,
            java.time.LocalTime.class,java.time.LocalDateTime.class,java.time.OffsetTime.class,java.time.OffsetDateTime.class,java.time.Instant.class);
    private SqlValues() { }
    public static Object parameter(Object value) {
        if(value instanceof Double number && !Double.isFinite(number) || value instanceof Float floating && !Float.isFinite(floating))
            throw new IllegalArgumentException("Finite SQL parameter required");
        return snapshot(value);
    }
    public static Object snapshot(Object value) {
        if(value==null || IMMUTABLE.contains(value.getClass()))return value;
        if(value instanceof byte[] bytes)return bytes.clone();
        throw new IllegalArgumentException("Unsupported or mutable SQL value: "+value.getClass().getName());
    }
    /** UTF-16 text units/binary bytes plus conservative per-cell storage units, including SQL NULL. */
    public static long units(Object value) {
        if(value instanceof String text)return 64L+text.length();
        if(value instanceof byte[] bytes)return 64L+bytes.length;
        if(value instanceof BigDecimal number)return 66L+Math.max(number.precision(),(long)number.precision()-number.scale())+Math.max(0L,(long)number.scale());
        if(value instanceof BigInteger number)return 65L+((long)number.bitLength()*30103+99999)/100000;
        return 64;
    }
}
