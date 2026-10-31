package atf.eventuate.await;
import atf.eventuate.api.OrderObservation;
import java.time.Duration;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import java.util.function.Consumer;

/** One convergence budget; intermediate observations have no assertion verdict. */
public final class OrderAwaiter {
    public record Observation(int number, Duration elapsed, OrderObservation order) { }
    public record Result(boolean converged, List<Observation> observations, Duration elapsed) {
        public Result { observations = List.copyOf(observations); }
        public OrderObservation last() { return observations.getLast().order(); }
    }
    private final LongSupplier ticks;
    private final Consumer<Duration> pause;
    public OrderAwaiter(LongSupplier ticks, Consumer<Duration> pause) {
        this.ticks = Objects.requireNonNull(ticks); this.pause = Objects.requireNonNull(pause);
    }
    public Result await(Supplier<OrderObservation> read, String expectedState, Duration budget, Duration interval) {
        if (budget.isNegative() || budget.isZero() || interval.isNegative() || interval.isZero())
            throw new IllegalArgumentException("Positive convergence budget and interval required");
        long start = ticks.getAsLong();
        var trace = new ArrayList<Observation>();
        while (true) {
            long elapsed = Math.max(0, ticks.getAsLong() - start);
            if (elapsed >= budget.toNanos()) return new Result(false, trace, Duration.ofNanos(elapsed));
            var actual = Objects.requireNonNull(read.get());
            elapsed = Math.max(0, ticks.getAsLong() - start);
            trace.add(new Observation(trace.size() + 1, Duration.ofNanos(elapsed), actual));
            if (elapsed < budget.toNanos() && expectedState.equals(actual.state()))
                return new Result(true, trace, Duration.ofNanos(elapsed));
            if (elapsed >= budget.toNanos()) return new Result(false, trace, Duration.ofNanos(elapsed));
            pause.accept(Duration.ofNanos(Math.min(interval.toNanos(), budget.toNanos() - elapsed)));
        }
    }
}
