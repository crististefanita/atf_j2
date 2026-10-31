package atf.eventuate.api;
import java.util.Objects;
/** HTTP facts; the caller provides the expected business state independently. */
public record OrderObservation(long orderId, String state, String rejectionReason) {
    public OrderObservation { Objects.requireNonNull(state); }
}
