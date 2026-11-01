package atf.eventuate.tests.e2e.support;

import atf.eventuate.evidence.EventuateEvidence;
import atf.eventuate.live.LiveOrderScenario;
import atf.testkit.assertions.RecordedAssertions;
import java.math.BigDecimal;

/** Independent expected values shared by the three LIVE tests; no execution or mutation callback. */
public final class LiveOrderChecks {
    private LiveOrderChecks() { }
    public static void verify(RecordedAssertions checks, EventuateEvidence evidence, LiveOrderScenario.Result result,
                              String state, String reason, int reservations, int customers) {
        checks.equals("HTTP source is real", "LIVE", evidence.source());
        checks.equals("One order POST; no mutation replay", 1, result.orderPosts());
        checks.equals("Customer POST count", customers, result.customerPosts());
        checks.equals("Terminal HTTP identity", result.orderId(), result.terminal().orderId());
        checks.equals("Terminal HTTP state", state, result.terminal().state());
        checks.equals("Terminal HTTP reason", reason, result.terminal().rejectionReason());
        checks.isTrue("Observer ready before the first mutation", result.kafka().readyBeforeMutation());
        checks.isTrue("Observer group is independently owned", result.kafka().groupId().startsWith("atf-j2-observer-"));
        checks.isTrue("Kafka partition baselines captured", result.kafka().baseline().size() >= 2);
        var relevant = result.kafka().events().stream().filter(event -> event.aggregateId().equals(Long.toString(result.orderId()))
                && event.topic().endsWith(".Order")).toList();
        checks.isTrue("Real OrderCreated event observed", relevant.stream().anyMatch(event -> event.eventType().endsWith(".OrderCreatedEvent")));
        checks.isTrue("Real terminal order event observed", relevant.stream().anyMatch(event -> event.eventType().endsWith(
                state.equals("APPROVED") ? ".OrderApprovedEvent" : ".OrderRejectedEvent")));
        checks.equals("Distinct broker message identities", (long) relevant.size(), relevant.stream().map(event -> event.messageId()).distinct().count());
        checks.isTrue("Broker offsets follow the captured baseline", relevant.stream().allMatch(event -> result.kafka().baseline().stream()
                .anyMatch(position -> position.topic().equals(event.topic()) && position.partition() == event.partition() && event.offset() >= position.offset())));
        checks.isTrue("Independent SQL capture is complete", result.sql().complete());
        checks.isTrue("Independent SQL connection is read-only", result.sql().readOnly());
        checks.equals("Exactly one persisted order", 1, result.sql().orders().size());
        var order = result.sql().orders().getFirst();
        checks.equals("SQL order identity", result.orderId(), order.id());
        checks.equals("SQL customer identity", result.customerId(), order.customerId());
        checks.equals("SQL state independently agrees", state, order.state());
        checks.equals("SQL amount is exact", 0, new BigDecimal("12.34").compareTo(order.amount()));
        checks.equals("SQL rejection code", reason == null ? null : reason.equals("INSUFFICIENT_CREDIT") ? "0" : "1", order.rejectionReason());
        checks.equals("Independent credit reservation count", reservations, result.sql().reservations().size());
        if (reservations == 1) {
            var row = result.sql().reservations().getFirst();
            checks.equals("Reservation belongs to this customer", result.customerId(), row.customerId());
            checks.equals("Reservation belongs to this order", result.orderId(), row.orderId());
            checks.equals("Reserved amount is exact", 0, new BigDecimal("12.34").compareTo(row.amount()));
        }
        checks.equals("Two captured SELECT templates", 2, result.sql().queries().size());
    }
}
