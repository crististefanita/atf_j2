package atf.eventuate.tests.support.acceptance.profile;

import atf.eventuate.tests.component.*;
import atf.eventuate.tests.suites.EventuateComponentSuite;
import atf.reporting.artifact.ReportArtifactProfile;
import atf.reporting.artifact.ReportArtifactProfile.*;
import java.util.ArrayList;
import java.util.List;
import static atf.reporting.contract.AtfReportStatus.*;

/** Declared consumer intent; neither HTML nor publisher output can select or weaken this inventory. */
public final class EventuateExpectedProfiles {
    private EventuateExpectedProfiles() { }
    public static ReportArtifactProfile expected(String renderer, Class<?> selected, String probe) {
        var scenarios = new ArrayList<Scenario>();
        if (selected == EventuateApiContractComponentTest.class || selected == EventuateComponentSuite.class)
            scenarios.add(api(probe));
        if (selected == EventuateConvergenceComponentTest.class || selected == EventuateComponentSuite.class)
            scenarios.add(convergence());
        if (selected == EventuateMessageEvidenceComponentTest.class || selected == EventuateComponentSuite.class)
            scenarios.add(message(probe));
        return new ReportArtifactProfile(Renderer.valueOf(renderer), scenarios);
    }
    private static Scenario api(String probe) {
        var names = List.of(
                "Execution provenance",
                "Order identity",
                "Order state",
                "Full synthetic audit value before display preview",
                "Independent complete order contract",
                "Negative control rejects a different order",
                "All expected exchanges consumed");
        int count = switch (probe) { case "wrong-identity" -> 2; case "late-mismatch" -> 4; default -> names.size(); };
        return scenario("EventuateApiContractComponentTest", "API contract · approved order",
                names.subList(0, count), !probe.equals("none"), List.of(http("GET /orders/42")));
    }
    private static Scenario convergence() {
        var evidence = new ArrayList<Evidence>();
        evidence.add(http("POST /orders"));
        for (int index = 0; index < 3; index++) evidence.add(http("GET /orders/42"));
        for (String outcome : List.of("CONVERGED", "DEADLINE_REACHED", "DEADLINE_REACHED"))
            evidence.add(new Observation("Order convergence · " + outcome,
                    List.of("Convergence outcome", "Expected order ID", "Expected order state", "Expected rejection reason",
                            "Convergence budget", "Poll interval", "Convergence elapsed", "Observation count"),
                    List.of("Convergence observations")));
        return scenario("EventuateConvergenceComponentTest", "Convergence · bounded read retry",
                List.of(
                "Order converges within one budget",
                "Convergence observations",
                "Measured virtual wait",
                "HTTP attempts in first read",
                "Creation dispatches",
                "Total dispatched exchanges",
                "All expected exchanges consumed",
                "Negative control refuses nonconvergence",
                "Exhausted budget is not renewed",
                "No read at the expired deadline",
                "Foreign approved order cannot converge",
                "Foreign order observations",
                "Observed foreign order identity",
                "Foreign identity consumes only the original budget"), false, evidence);
    }
    private static Scenario message(String probe) {
        var names = List.of(
                "Evidence provenance",
                "Observed fixture deliveries",
                "Same message and order identity",
                "Independent reservation count",
                "Independent reserved credit",
                "Single business effect witness",
                "Business and projection states converge",
                "Negative control rejects duplicate effects",
                "Negative control rejects missing delivery",
                "Negative control rejects a stale projection");
        boolean failed = probe.equals("duplicate-effect");
        return scenario("EventuateMessageEvidenceComponentTest", "Message evidence · duplicate witness",
                failed ? names.subList(0, 4) : names, failed, List.of());
    }
    private static HttpExchange http(String request) {
        return new HttpExchange(request + " — Observed mock exchange", true, true,
                List.of("Request body", "Response body"), List.of(), INFO);
    }
    private static Scenario scenario(String type, String title, List<String> names, boolean failed, List<Evidence> evidence) {
        var rows = new ArrayList<Comparison>();
        for (int index = 0; index < names.size(); index++)
            rows.add(new Comparison(names.get(index), !failed || index < names.size() - 1));
        var body = new ArrayList<Evidence>(evidence);
        body.add(new Assertions("Component assertions", rows));
        var status = failed ? FAIL : PASS;
        return new Scenario(type, title, status, List.of(
                phase("SETUP", "Reporting loaded from a binary artifact"),
                new Phase("TEST BODY", status, body),
                phase("TEARDOWN", "No captured HTTP exchange executed a network endpoint")));
    }
    private static Phase phase(String name, String comparison) {
        return new Phase(name, PASS, List.of(new Assertions("Component assertions", List.of(new Comparison(comparison, true)))));
    }
}
