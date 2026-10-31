package atf.eventuate.tests.support.fixtures;
import java.time.Duration;
/** Virtual convergence clock; HTTP execution timing remains measured by Core. */
public final class ManualClock {
    private long nanos;
    public long nanoTime() { return nanos; }
    public void advance(Duration duration) { nanos = Math.addExact(nanos, duration.toNanos()); }
}
