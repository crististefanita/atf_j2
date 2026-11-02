package atf.sql.tests.support.acceptance;

import atf.reporting.artifact.ReportArtifactProfile;
import atf.reporting.artifact.ReportArtifactProfile.*;
import java.util.ArrayList;
import java.util.List;
import static atf.reporting.contract.AtfReportStatus.*;

/** SQL intent authored from the fixture contract; precise typed values remain the independent SQL oracle's responsibility. */
final class SqlExpectedProfiles {
    private SqlExpectedProfiles() { }
    static ReportArtifactProfile expected(String renderer, String selection, boolean negative) {
        var scenarios = new ArrayList<Scenario>();
        if (selection.equals("suite") || selection.equals("results"))
            scenarios.add(scenario("SqlResultComponentTest", "capturesTypedValuesDuplicateLabelsAndRows",
                    List.of(
                "Complete JDBC result",
                "Four ordered columns",
                "Duplicate labels retained",
                "Exact decimal beyond Double precision",
                "Literal null text retained",
                "SQL NULL retained",
                "Empty text retained",
                "Resources closed before publication",
                "H2 read-only acknowledgement is not invented",
                "Duplicate rows retained",
                "Two distinct execution identities"), List.of("COMPLETE","COMPLETE"), false));
        if (selection.equals("suite") || selection.equals("transactions"))
            scenarios.add(scenario("SqlTransactionComponentTest", "witnessesCommitAndRollbackFromIndependentConnections",
                    List.of(
                "Uncommitted writer is invisible",
                "Commit is independently visible",
                "Rollback does not leak another effect",
                "Original identity retained",
                "Three read executions"), List.of("COMPLETE","COMPLETE","COMPLETE"), false));
        if (selection.equals("suite") || selection.equals("failures"))
            scenarios.add(scenario("SqlCaptureFailureComponentTest", "rejectsAggregateCaptureOverflowWithoutReturningACompleteResult",
                    List.of(
                "Aggregate budget exhaustion is a JDBC data failure",
                "Failure identifies the aggregate capture budget",
                "Overflow cannot become COMPLETE",
                "Exactly one terminal capture"), List.of("FAILED"), false));
        if (selection.equals("suite") || selection.equals("failures"))
            scenarios.add(scenario("SqlCaptureFailureComponentTest", "capturesBinaryAndTemporalValuesWithoutLeakingMutableJdbcObjects",
                    List.of(
                "Binary truncation is incomplete",
                "Bounded binary prefix retained",
                "Captured binary is immutable",
                "JDBC date becomes immutable LocalDate"), List.of("INCOMPLETE"), false));
        if (selection.equals("suite") || selection.equals("failures"))
            scenarios.add(scenario("SqlCaptureFailureComponentTest", "marksPartialCaptureAndPreservesSqlFailure",
                    List.of(
                "Partial capture cannot claim COMPLETE",
                "Additional rows are declared",
                "Configured text prefix retained",
                "Cell truncation is explicit",
                "Full-result assertion refuses partial data",
                "Real JDBC failure is propagated",
                "Missing table is a genuine JDBC syntax error",
                "Failure identifies the requested absent table",
                "Terminal SQL evidence is FAILED",
                "Original SQLState retained",
                "One capture per execution; no replay"), List.of("INCOMPLETE","FAILED"), negative));
        return new ReportArtifactProfile(Renderer.valueOf(renderer), scenarios);
    }
    private static Scenario scenario(String type, String method, List<String> names, List<String> outcomes, boolean failed) {
        var evidence = new ArrayList<Evidence>();
        for (String outcome : outcomes) evidence.add(new Observation("SQL capture · " + outcome,
                List.of("SQL execution ID", "SQL provenance"), List.of("Query, typed binds and captured rows")));
        evidence.add(new Assertions("Component assertions", names.stream().map(name -> new Comparison(name, true)).toList()));
        var status = failed ? FAIL : PASS;
        return new Scenario(type, method + "(RecordedAssertions, SqlTestEvidence)", status,
                List.of(new Phase("TEST BODY", status, evidence)));
    }
}
