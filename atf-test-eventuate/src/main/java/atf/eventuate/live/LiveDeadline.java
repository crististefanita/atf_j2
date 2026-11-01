package atf.eventuate.live;

import java.time.Duration;
import java.util.Objects;
import java.util.function.LongSupplier;

/** Monotonic, invocation-owned budget shared by readiness, HTTP, Kafka and SQL. */
public final class LiveDeadline {
    private final LongSupplier ticks;
    private final long start, nanos;
    public LiveDeadline(Duration budget) { this(budget, System::nanoTime); }
    public LiveDeadline(Duration budget, LongSupplier ticks) {
        this.ticks = Objects.requireNonNull(ticks);
        nanos = Objects.requireNonNull(budget).toNanos();
        if (nanos <= 0) throw new IllegalArgumentException("Positive LIVE budget required");
        start = ticks.getAsLong();
    }
    public Duration remaining() {
        long remaining = nanos - Math.max(0, ticks.getAsLong() - start);
        if (remaining <= 0) throw new Expired("LIVE deadline exhausted; no further operation dispatched");
        return Duration.ofNanos(remaining);
    }
    public Duration cap(Duration maximum) {
        if (maximum.isNegative() || maximum.isZero()) throw new IllegalArgumentException("Positive operation cap required");
        Duration remaining = remaining();
        return remaining.compareTo(maximum) < 0 ? remaining : maximum;
    }
    public static final class Expired extends RuntimeException {
        public Expired(String message) { super(message); }
    }
}
