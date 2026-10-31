package atf.eventuate.validation;
import atf.eventuate.api.OrderObservation;
/** Independent expected values; comparison uses full values before report display. */
public record OrderContract(long orderId, String state, String rejectionReason) {
    public boolean matches(OrderObservation actual) {
        return actual.orderId() == orderId && state.equals(actual.state())
                && java.util.Objects.equals(rejectionReason, actual.rejectionReason());
    }
}
