package atf.eventuate.await;
import atf.eventuate.api.OrderObservation;
import atf.eventuate.validation.OrderContract;
import java.time.Duration;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import java.util.function.Consumer;
import java.util.Optional;
import static atf.eventuate.await.OrderConvergenceSnapshot.Termination.*;

/** One convergence budget and one terminal capture; publication never replays a read. */
public final class OrderAwaiter {
    public record Observation(int number, Duration elapsed, OrderObservation order) { }
    public record Result(boolean converged, List<Observation> observations, Duration elapsed) {
        public Result { observations = List.copyOf(observations); }
        public OrderObservation last() { return observations.getLast().order(); }
        public Optional<OrderObservation> lastObservedOrder() {
            return observations.isEmpty() ? Optional.empty() : Optional.of(last());
        }
    }
    private final LongSupplier ticks;
    private final Consumer<Duration> pause;
    private final Consumer<OrderConvergenceSnapshot> evidence;
    public OrderAwaiter(LongSupplier ticks, Consumer<Duration> pause) { this(ticks, pause, snapshot -> { }); }
    public OrderAwaiter(LongSupplier ticks, Consumer<Duration> pause, Consumer<OrderConvergenceSnapshot> evidence) {
        this.ticks = Objects.requireNonNull(ticks); this.pause = Objects.requireNonNull(pause);
        this.evidence = Objects.requireNonNull(evidence);
    }
    public Result await(Supplier<OrderObservation> read, OrderContract expected, Duration budget, Duration interval) {
        Objects.requireNonNull(read, "read");
        Objects.requireNonNull(expected, "expected");
        Objects.requireNonNull(budget, "budget"); Objects.requireNonNull(interval, "interval");
        if (budget.isNegative() || budget.isZero() || interval.isNegative() || interval.isZero())
            throw new IllegalArgumentException("Positive convergence budget and interval required");
        var run = new Run(expected, budget, interval);
        final Result result;
        try { result = run.execute(read); }
        catch (RuntimeException | Error original) {
            var snapshot = run.failed(original);
            OrderConvergenceFailure.attach(original, snapshot);
            try { evidence.accept(snapshot); }
            catch (RuntimeException | Error secondary) { suppress(original, secondary); }
            throw original;
        }
        var snapshot = run.snapshot(Optional.of(result.elapsed()), Optional.empty());
        try { evidence.accept(snapshot); }
        catch (RuntimeException | Error publication) {
            OrderConvergenceFailure.attach(publication, snapshot);
            throw publication;
        }
        return result;
    }
    private static void suppress(Throwable original, Throwable secondary) {
        if (original != secondary) original.addSuppressed(secondary);
    }
    private final class Run {
        private final OrderContract expected;
        private final Duration budget, interval;
        private final long budgetNanos, intervalNanos;
        private final List<OrderConvergenceSnapshot.Observation> trace = new ArrayList<>();
        private OrderConvergenceSnapshot.Termination stage = CLOCK_FAILURE;
        private long start;
        private Run(OrderContract expected, Duration budget, Duration interval) {
            this.expected = expected; this.budget = budget; this.interval = interval;
            budgetNanos = budget.toNanos(); intervalNanos = interval.toNanos();
        }
        private long elapsed() { return Math.max(0, ticks.getAsLong() - start); }
        private Result execute(Supplier<OrderObservation> read) {
            start = ticks.getAsLong();
            while (true) {
                stage = CLOCK_FAILURE;
                long elapsed = elapsed();
                if (elapsed >= budgetNanos) return finish(false, elapsed);
                stage = READ_FAILURE;
                var actual = Objects.requireNonNull(read.get(), "Order observation");
                int number = trace.size() + 1;
                trace.add(new OrderConvergenceSnapshot.Observation(number, Optional.empty(), actual));
                stage = CLOCK_FAILURE;
                elapsed = elapsed();
                trace.set(trace.size() - 1, new OrderConvergenceSnapshot.Observation(number, Optional.of(Duration.ofNanos(elapsed)), actual));
                if (elapsed < budgetNanos && expected.matches(actual)) return finish(true, elapsed);
                if (elapsed >= budgetNanos) return finish(false, elapsed);
                stage = PAUSE_FAILURE;
                pause.accept(Duration.ofNanos(Math.min(intervalNanos, budgetNanos - elapsed)));
            }
        }
        private Result finish(boolean converged, long elapsed) {
            stage = converged ? CONVERGED : DEADLINE_REACHED;
            return new Result(converged, trace.stream().map(value -> new Observation(value.number(),
                    value.elapsed().orElseThrow(), value.order())).toList(), Duration.ofNanos(elapsed));
        }
        private OrderConvergenceSnapshot failed(Throwable original) {
            Optional<Duration> elapsed = Optional.empty();
            if (stage != CLOCK_FAILURE) {
                try { elapsed = Optional.of(Duration.ofNanos(elapsed())); }
                catch (RuntimeException | Error clockFailure) { suppress(original, clockFailure); }
            }
            return snapshot(elapsed, Optional.of(OrderConvergenceSnapshot.Failure.from(original)));
        }
        private OrderConvergenceSnapshot snapshot(Optional<Duration> elapsed,
                Optional<OrderConvergenceSnapshot.Failure> failure) {
            return new OrderConvergenceSnapshot(expected, budget, interval, trace, elapsed, stage, failure);
        }
    }
}
