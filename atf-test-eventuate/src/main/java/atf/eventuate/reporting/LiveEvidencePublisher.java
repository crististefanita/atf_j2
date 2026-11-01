package atf.eventuate.reporting;

import atf.eventuate.evidence.EventuateEvidence;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.model.ReportField;
import atf.reporting.model.ReportObservation;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

/** Projects captured LIVE facts through the existing JAR observation contract, without JDBC/Kafka callbacks. */
final class LiveEvidencePublisher {
    private static final ObjectMapper JSON = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS);
    private LiveEvidencePublisher() { }
    static void publish(AtfReportPorts.Phase phase, EventuateEvidence evidence, String name) {
        for (var kafka : evidence.kafka(name)) observation(phase, "Kafka observer · LIVE", List.of(
                new ReportField("Observer group", kafka.groupId()),
                new ReportField("Ready before mutation", Boolean.toString(kafka.readyBeforeMutation())),
                new ReportField("Offset commit", "DISABLED; manual assignment")), kafka);
        for (var sql : evidence.liveSql(name)) observation(phase, "SQL witness · LIVE · " + (sql.complete() ? "COMPLETE" : "FAILED"), List.of(
                new ReportField("SQL provenance", "SQL_LIVE"), new ReportField("Independent read-only connection", Boolean.toString(sql.readOnly())),
                new ReportField("Requested order ID", Long.toString(sql.orderId())), new ReportField("SQL elapsed", sql.elapsed().toString())), sql);
    }
    private static void observation(AtfReportPorts.Phase phase, String title, List<ReportField> fields, Object facts) {
        if (!phase.supportsObservations()) throw new IllegalStateException("LIVE observation capability required");
        try { phase.observation(new ReportObservation(title, fields, List.of(new ReportObservation.Body(
                "Captured LIVE facts", JSON.writeValueAsString(facts), ReportObservation.Format.JSON)), List.of())); }
        catch (java.io.IOException failure) { throw new IllegalStateException("Cannot project captured LIVE facts", failure); }
    }
}
