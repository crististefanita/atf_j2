package atf.eventuate.reporting;
import atf.core.http.response.ApiExecutionResult;
import atf.reporting.api.ReportModelAdapters;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.contract.AtfReportStatus;
import java.util.List;
/** Uses the library's published adapter; HTTP evidence keeps its MOCK provenance. */
final class ApiEvidencePublisher {
    private ApiEvidencePublisher() { }
    static void publish(AtfReportPorts.Phase phase, List<ApiExecutionResult> exchanges) {
        for (var exchange : exchanges) {
            var input = exchange.request();
            try (var request = phase.startRequest("Observed mock exchange", input.method(), input.path())) {
                request.execution(ReportModelAdapters.execution(exchange));
                request.json("Request body", input.bodyText());
                request.json("Response body", exchange.response().body());
                request.detail("Evidence source", exchange.provenance().name());
                request.complete(AtfReportStatus.INFO);
            }
        }
    }
}
