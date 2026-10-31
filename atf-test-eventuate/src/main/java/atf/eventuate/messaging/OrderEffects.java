package atf.eventuate.messaging;
import java.math.BigDecimal;
/** Independent fixture witness; no broker, database or application handler is emulated. */
public record OrderEffects(long orderId, int reservations, BigDecimal reservedCredit, String state, String projectionState) { }
