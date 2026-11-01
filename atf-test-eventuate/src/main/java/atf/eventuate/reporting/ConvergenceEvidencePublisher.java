package atf.eventuate.reporting;

import atf.eventuate.await.OrderConvergenceSnapshot;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.model.ReportField;
import atf.reporting.model.ReportObservation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Captured facts only: no assertion verdict, transport call or convergence reevaluation. */
final class ConvergenceEvidencePublisher {
    private static final ObjectMapper JSON = new ObjectMapper();
    private ConvergenceEvidencePublisher() { }
    static void publish(AtfReportPorts.Phase phase, List<OrderConvergenceSnapshot> snapshots) {
        if (snapshots.isEmpty()) return;
        if (!phase.supportsObservations()) throw new IllegalStateException("Reporting observation capability required");
        for (var snapshot : snapshots) phase.observation(project(snapshot));
    }
    private static ReportObservation project(OrderConvergenceSnapshot snapshot) {
        var fields = new ArrayList<ReportField>();
        fields.add(new ReportField("Convergence outcome", snapshot.termination().name()));
        fields.add(new ReportField("Expected order ID", Long.toString(snapshot.expected().orderId())));
        fields.add(new ReportField("Expected order state", snapshot.expected().state()));
        fields.add(new ReportField("Expected rejection reason", String.valueOf(snapshot.expected().rejectionReason())));
        fields.add(new ReportField("Convergence budget", snapshot.budget().toString()));
        fields.add(new ReportField("Poll interval", snapshot.interval().toString()));
        fields.add(new ReportField("Convergence elapsed", snapshot.elapsed().map(Object::toString).orElse("unknown")));
        fields.add(new ReportField("Observation count", Integer.toString(snapshot.observations().size())));
        snapshot.failure().ifPresent(failure -> {
            fields.add(new ReportField("Failure type", failure.type()));
            fields.add(new ReportField("Failure message", failure.message()));
            fields.add(new ReportField("Original cause", failure.originalCause()));
        });
        try {
            var history = new ArrayList<Map<String, Object>>();
            for (var observed : snapshot.observations()) {
                var row = new LinkedHashMap<String, Object>();
                row.put("number", observed.number()); row.put("elapsedNanos", observed.elapsed().map(java.time.Duration::toNanos).orElse(null));
                row.put("orderId", observed.order().orderId()); row.put("orderState", observed.order().state());
                row.put("rejectionReason", observed.order().rejectionReason()); history.add(row);
            }
            return new ReportObservation("Order convergence · " + snapshot.termination(), fields,
                    List.of(new ReportObservation.Body("Convergence observations", JSON.writeValueAsString(history), ReportObservation.Format.JSON)), List.of());
        } catch (JsonProcessingException failure) { throw new IllegalStateException("Cannot project captured convergence history", failure); }
    }
}
