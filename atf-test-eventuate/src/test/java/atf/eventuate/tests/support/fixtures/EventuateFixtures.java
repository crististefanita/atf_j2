package atf.eventuate.tests.support.fixtures;
import atf.mock.MockRoute;
import atf.eventuate.messaging.*;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
/** Observed fixture values may be faulted; the tests' expected values remain fixed. */
public final class EventuateFixtures {
    public static final String DESCRIPTION = "Order evidence with a deliberately long audit description: " + "auditable-value-".repeat(20);
    private EventuateFixtures() { }
    public static MockRoute route(String method, String path, int status, String body) {
        return new MockRoute(method, path, status, body, Map.of("Content-Type", "application/json"), Duration.ZERO);
    }
    public static MockRoute creation() {
        return new MockRoute("POST", "/orders", 200, "{\"orderId\":42}",
                Map.of("Content-Type", "application/json"), Duration.ZERO, MockRoute.ANY_AUTH_PROFILE,
                Map.of(), Map.of("Content-Type", "application/json"), "json",
                "{\"customerId\":7,\"orderTotal\":{\"amount\":70.00}}");
    }
    public static String order(String state) {
        String description = DESCRIPTION + (probe().equals("late-mismatch") ? "DIFFERENT" : "");
        long identity = probe().equals("wrong-identity") ? 99 : 42;
        return "{\"orderId\":" + identity + ",\"orderState\":\"" + state + "\",\"rejectionReason\":null,\"auditDescription\":\"" + description + "\"}";
    }
    public static List<MessageDelivery> deliveries() {
        return List.of(new MessageDelivery("evt-42", 42, "OrderCreated", 1), new MessageDelivery("evt-42", 42, "OrderCreated", 2));
    }
    public static OrderEffects effects() {
        return new OrderEffects(42, probe().equals("duplicate-effect") ? 2 : 1, new BigDecimal("70.00"), "APPROVED", "APPROVED");
    }
    private static String probe() {
        String value = System.getProperty("atf.eventuate.probe", "none");
        if (!List.of("none", "late-mismatch", "duplicate-effect", "wrong-identity").contains(value)) throw new IllegalArgumentException("Unknown controlled probe: " + value);
        return value;
    }
}
