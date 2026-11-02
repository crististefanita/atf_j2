package atf.sql.tests.support.acceptance;

import atf.sql.tests.component.SqlResultComponentTest;
import atf.sql.tests.component.SqlTransactionComponentTest;
import atf.sql.tests.component.SqlCaptureFailureComponentTest;
import atf.sql.tests.suites.SqlComponentSuite;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import atf.reporting.artifact.ReportArtifactInspector;
import atf.reporting.artifact.ReportArtifactProfile;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.*;
import java.util.*;

/** Native SQL consumer matrix; controlled JDBC failure remains an actual JUnit failure. */
public final class SqlReportAcceptance {
    private SqlReportAcceptance() { }
    public static void main(String[] args) throws Exception {
        Path output=Path.of(args[0]).toAbsolutePath();
        if(Files.exists(output))throw new IllegalArgumentException("Fresh SQL matrix required");
        Files.createDirectories(output);var receipts=new ArrayList<Map<String,Object>>();
        var previous=new LinkedHashMap<String,String>();
        for(String key:List.of("atf.sql.report.directory","atf.sql.report.renderer","atf.sql.probe.unhandled"))previous.put(key,System.getProperty(key));
        try {
            for(String renderer:List.of("EXTENT","FALLBACK")) {
                for(String selection:List.of("results","transactions","failures","suite"))receipts.add(run(output,renderer,selection,false));
                receipts.add(run(output,renderer,"failures",true));receipts.add(run(output,renderer,"suite",true));
            }
        } finally {previous.forEach((key,value)->{if(value==null)System.clearProperty(key);else System.setProperty(key,value);});}
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(output.resolve("acceptance.json").toFile(),receipts);
        System.out.println("SQL_NATIVE_ACCEPTANCE reports="+receipts.size()+"; independent facts and mutants=PASS");
    }
    private static Map<String,Object> run(Path output,String renderer,String selected,boolean negative) throws Exception {
        Path directory=output.resolve(renderer.toLowerCase(Locale.ROOT)).resolve(selected+(negative?"-unhandled":"-nominal"));Files.createDirectories(directory);
        System.setProperty("atf.sql.report.directory",directory.toString());System.setProperty("atf.sql.report.renderer",renderer);
        System.setProperty("atf.sql.probe.unhandled",Boolean.toString(negative));
        Class<?> type=switch(selected){case "results"->SqlResultComponentTest.class;case "transactions"->SqlTransactionComponentTest.class;
            case "failures"->SqlCaptureFailureComponentTest.class;case "suite"->SqlComponentSuite.class;default->throw new IllegalArgumentException(selected);};
        var summary=new SummaryGeneratingListener();
        var request=LauncherDiscoveryRequestBuilder.request().selectors(DiscoverySelectors.selectClass(type))
                .configurationParameter("junit.jupiter.execution.parallel.enabled","false").build();
        LauncherFactory.create().execute(request,summary);var result=summary.getSummary();
        int tests=selected.equals("suite")?5:selected.equals("failures")?3:1;
        if(result.getTestsFoundCount()!=tests || result.getTestsStartedCount()!=tests || result.getTestsSkippedCount()!=0
                || result.getTestsAbortedCount()!=0 || result.getTestsSucceededCount()!=tests-(negative?1:0)
                || result.getTestsFailedCount()!=(negative?1:0))throw new AssertionError("Native SQL JUnit verdict differs: "+result.getFailures());
        if(negative && !(result.getFailures().getFirst().getException() instanceof java.sql.SQLSyntaxErrorException))
            throw new AssertionError("Unhandled SQL cause was replaced");
        List<Path> reports;try(var files=Files.list(directory)){reports=files.filter(path->path.toString().endsWith(".html")).toList();}
        if(reports.size()!=1)throw new AssertionError("One class/suite HTML required");
        String html=Files.readString(reports.getFirst());SqlReportOracle.require(html,selected,negative);SqlReportOracle.rejectMutants(html,selected,negative);
        var inspector=requireProfile(reports.getFirst(),SqlExpectedProfiles.expected(renderer,selected,negative));
        var receipt=new LinkedHashMap<String,Object>();receipt.put("renderer",renderer);receipt.put("selection",selected);receipt.put("controlledFailure",negative);
        receipt.put("tests",tests);receipt.put("failures",negative?1:0);receipt.put("report",output.relativize(reports.getFirst()).toString().replace('\\','/'));
        receipt.put("sqlFactsAndMutants","PASS");receipt.put("explicitProfileCompliant",inspector.isCompliant());
        receipt.put("explicitProfileChecks",inspector.artifact().checks());receipt.put("profileMutationRejected",true);
        receipt.put("legacyInspector",ReportArtifactInspector.inspect(reports.getFirst()));return receipt;
    }
    private static ReportArtifactInspector.ProfileInspection requireProfile(Path report, ReportArtifactProfile profile) throws Exception {
        var inspection = ReportArtifactInspector.inspect(report, profile);
        if (!inspection.isCompliant()) throw new AssertionError("Declared artifact profile failed: " + inspection.artifact().checks());
        String html = Files.readString(report);
        var document = org.jsoup.Jsoup.parse(html);
        String selector = profile.renderer() == ReportArtifactProfile.Renderer.EXTENT
                ? "#test-view .test-list-item .test-contents table.helm-validation-table th"
                : "#tests article.scenario table.assertions th";
        var target = document.select(selector).stream().filter(cell -> cell.text().equals("ACTUAL")).findFirst().orElseThrow();
        target.text("REMOVED");
        String mutant = document.outerHtml();
        Path negative = Files.createTempFile(report.getParent(), "profile-mutant-", ".html");
        try {
            Files.writeString(negative, mutant);
            if (ReportArtifactInspector.inspect(negative, profile).isCompliant())
                throw new AssertionError("Degraded evidence escaped the declared profile");
        } finally { Files.delete(negative); }
        return inspection;
    }
}
