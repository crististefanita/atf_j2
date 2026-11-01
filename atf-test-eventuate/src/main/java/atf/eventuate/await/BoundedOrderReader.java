package atf.eventuate.await;

import atf.eventuate.api.OrderObservation;
import java.time.Duration;

/** A read receives the remaining convergence budget; it must not renew it. */
@FunctionalInterface
public interface BoundedOrderReader {
    OrderObservation read(Duration remaining);
}
