package atf.eventuate.reporting;
import atf.eventuate.evidence.EventuateExchange;
import atf.reporting.api.ReportModelAdapters;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.contract.AtfReportStatus;
import java.util.List;
/** Uses the library's published adapter; HTTP evidence keeps its MOCK provenance. */
final class ApiEvidencePublisher {
    private ApiEvidencePublisher() { }
    static void publish(AtfReportPorts.Phase phase, List<EventuateExchange> exchanges, AtfReportStatus phaseStatus) {
        for (var exchange : exchanges) {
            var input = exchange.request();
            try (var request = phase.startRequest("Observed mock exchange", input.method(), input.path())) {
                switch (exchange) {
                    case EventuateExchange.Completed completed -> request.execution(ReportModelAdapters.execution(completed.result()));
                    case EventuateExchange.Terminal terminal -> {
                        request.execution(ReportModelAdapters.partialExecution(terminal.snapshot()));
                        request.detail("Terminal outcome", terminal.snapshot().termination().name());
                        request.detail("Failure cause", terminal.failure().toString());
                        request.detail("Original cause", java.util.Objects.toString(terminal.failure().getCause(), "none"));
                    }
                }
                request.json("Request body", input.bodyText());
                exchange.response().ifPresent(response -> request.json("Response body", response.body()));
                request.detail("Evidence source", exchange.provenance().name());
                request.complete(exchange instanceof EventuateExchange.Terminal
                        && (phaseStatus == AtfReportStatus.FAIL || phaseStatus == AtfReportStatus.SKIP) ? phaseStatus : AtfReportStatus.INFO);
            }
        }
    }
}
