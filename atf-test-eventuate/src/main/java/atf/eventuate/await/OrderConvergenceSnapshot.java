package atf.eventuate.await;

import atf.eventuate.validation.OrderContract;
import atf.eventuate.api.OrderObservation;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Invocation-local facts, independent of HTTP evidence and report verdicts. */
public record OrderConvergenceSnapshot(OrderContract expected, Duration budget, Duration interval,
        List<Observation> observations, Optional<Duration> elapsed,
        Termination termination, Optional<Failure> failure) {
    public enum Termination { CONVERGED, DEADLINE_REACHED, READ_FAILURE, PAUSE_FAILURE, CLOCK_FAILURE }
    public record Observation(int number, Optional<Duration> elapsed, OrderObservation order) {
        public Observation { Objects.requireNonNull(elapsed); Objects.requireNonNull(order); }
    }
    public record Failure(String type, String message, String originalCause) {
        static Failure from(Throwable cause) {
            return new Failure(cause.getClass().getName(), Objects.toString(cause.getMessage(), ""),
                    Objects.toString(cause.getCause(), "none"));
        }
    }
    public OrderConvergenceSnapshot {
        Objects.requireNonNull(expected, "expected");
        Objects.requireNonNull(budget, "budget"); Objects.requireNonNull(interval, "interval");
        observations = List.copyOf(observations);
        Objects.requireNonNull(elapsed, "elapsed"); Objects.requireNonNull(termination, "termination");
        Objects.requireNonNull(failure, "failure");
        if (elapsed.filter(Duration::isNegative).isPresent()) throw new IllegalArgumentException("Negative elapsed time");
    }
    public Optional<Observation> lastObservation() {
        return observations.isEmpty() ? Optional.empty() : Optional.of(observations.getLast());
    }
}
