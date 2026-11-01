package atf.eventuate.reporting;

import atf.eventuate.sql.SqlEffectsSnapshot;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.model.ReportField;
import atf.reporting.model.ReportObservation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** SQL facts use the existing neutral observation API, separate from assertion results. */
final class SqlEvidencePublisher {
    private static final ObjectMapper JSON = new ObjectMapper();
    private SqlEvidencePublisher() { }
    static void publish(AtfReportPorts.Phase phase, List<SqlEffectsSnapshot> snapshots) {
        if (snapshots.isEmpty()) return;
        if (!phase.supportsObservations()) throw new IllegalStateException("Reporting observation capability required");
        for (var snapshot : snapshots) phase.observation(project(snapshot));
    }
    private static ReportObservation project(SqlEffectsSnapshot snapshot) {
        var fields = new ArrayList<ReportField>();
        fields.add(new ReportField("SQL provenance", snapshot.provenance().name()));
        fields.add(new ReportField("Witness ID", snapshot.witnessId()));
        fields.add(new ReportField("Requested order ID", Long.toString(snapshot.requestedOrderId())));
        fields.add(new ReportField("SQL outcome", snapshot.complete() ? "COMPLETE" : "FAILED"));
        fields.add(new ReportField("SQL row count", Integer.toString(snapshot.reservations().size())));
        fields.add(new ReportField("SQL elapsed", snapshot.elapsed().toString()));
        snapshot.failure().ifPresent(failure -> {
            fields.add(new ReportField("SQL failure type", failure.type()));
            fields.add(new ReportField("SQL failure message", failure.message()));
            fields.add(new ReportField("SQL state", failure.sqlState()));
            fields.add(new ReportField("SQL vendor code", Integer.toString(failure.vendorCode())));
            fields.add(new ReportField("Original SQL cause", failure.originalCause()));
            fields.add(new ReportField("SQL cleanup failures", failure.suppressed().toString()));
        });
        try {
            var rows = new ArrayList<Map<String, Object>>();
            for (var row : snapshot.reservations()) {
                var value = new LinkedHashMap<String, Object>();
                value.put("reservationId", row.reservationId()); value.put("orderId", row.orderId());
                value.put("amount", row.amount().toPlainString()); value.put("state", row.state()); rows.add(value);
            }
            return new ReportObservation("SQL witness · " + (snapshot.complete() ? "COMPLETE" : "FAILED"), fields,
                    List.of(new ReportObservation.Body("SQL reservation rows", JSON.writeValueAsString(rows), ReportObservation.Format.JSON)), List.of());
        } catch (JsonProcessingException failure) { throw new IllegalStateException("Cannot project captured SQL rows", failure); }
    }
}
