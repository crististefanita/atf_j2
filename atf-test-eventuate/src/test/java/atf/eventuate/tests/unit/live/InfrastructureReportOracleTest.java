package atf.eventuate.tests.unit.live;

import atf.eventuate.tests.support.acceptance.live.InfrastructureReportOracle;
import atf.reporting.configuration.AtfReportConfigurationLoader;
import atf.sql.query.SqlQuery;
import atf.sql.result.SqlSnapshot;
import atf.sql.result.SqlValue;
import atf.eventuate.live.kafka.KafkaEvidence;
import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
class InfrastructureReportOracleTest {
    private static final SqlSnapshot SQL=new SqlSnapshot("execution-one",new SqlQuery("SELECT amount"),
            List.of(new SqlSnapshot.Column(1,"amount",3,"DECIMAL")),List.of(List.of(new SqlValue(new BigDecimal("9007199254740993.01"),true))),
            SqlSnapshot.Outcome.COMPLETE,false,true,true,Duration.ofMillis(1),null);
    private static final String BODY="""
            {"executionId":"execution-one","query":{"template":"SELECT amount","parameters":[]},
             "columns":[{"ordinal":1,"label":"amount","jdbcType":3,"typeName":"DECIMAL"}],
             "rows":[[{"value":9007199254740993.01,"complete":true}]],"outcome":"COMPLETE","moreRows":false,
             "readOnlyAcknowledged":true,"resourcesClosed":true,"elapsed":"PT0.001S","failure":null}
            """;
    private static String report(String body,String status) {return "<li class=\"test-item\" status=\""+status+"\"></li><pre>"+body+"</pre>";}
    @Test void comparesKafkaZeroAndLargeOffsetsWithoutJavaNodeTypeOrDoubleCoercion() {
        var facts=new KafkaEvidence("group-one",true,List.of(new KafkaEvidence.Position("topic",0,0L)),
                List.of(new KafkaEvidence.Event("topic",0,9007199254740993L,"key","message-one","aggregate-one","Approved","{}")));
        String body="""
                {"groupId":"group-one","readyBeforeMutation":true,"baseline":[{"topic":"topic","partition":0,"offset":0}],
                 "events":[{"topic":"topic","partition":0,"offset":9007199254740993,"key":"key","messageId":"message-one",
                 "aggregateId":"aggregate-one","eventType":"Approved","payload":"{}"}]}
                """;
        assertDoesNotThrow(()->InfrastructureReportOracle.require(report(body,"pass"),List.of(),List.of(facts),false));
        assertThrows(AssertionError.class,()->InfrastructureReportOracle.require(report(body.replace("9007199254740993","9007199254740992"),"pass"),List.of(),List.of(facts),false));
        assertThrows(AssertionError.class,()->InfrastructureReportOracle.require(report(body.replace("message-one","foreign-message"),"pass"),List.of(),List.of(facts),false));
    }
    @Test void rejectsRoundedOrRelocatedSqlEvenWhenTheRootRemainsPass() {
        assertDoesNotThrow(()->InfrastructureReportOracle.require(report(BODY,"pass"),List.of(SQL),List.of(),false));
        assertThrows(AssertionError.class,()->InfrastructureReportOracle.require(report(BODY.replace("9007199254740993.01","9007199254740992.00"),"pass"),List.of(SQL),List.of(),false));
        assertThrows(AssertionError.class,()->InfrastructureReportOracle.require(report(BODY.replace("execution-one","foreign-id"),"pass"),List.of(SQL),List.of(),false));
    }
    @Test void rejectsTruncatedBodiesAndFalsePassForFailedExecution() {
        assertThrows(AssertionError.class,()->InfrastructureReportOracle.require(report(BODY.substring(0,100)+"… [truncated]","pass"),List.of(SQL),List.of(),false));
        assertThrows(AssertionError.class,()->InfrastructureReportOracle.require(report(BODY,"pass"),List.of(SQL),List.of(),true));
    }
    @Test void usesExplicitBoundedLaboratoryConfigInsteadOfProcessDefaults() {
        Path root=Path.of("").toAbsolutePath();
        while(root!=null && !java.nio.file.Files.isRegularFile(root.resolve("atf-setup/config/eventuate-report.yaml")))root=root.getParent();
        assertNotNull(root,"Laboratory configuration is part of the deployment");
        var settings=AtfReportConfigurationLoader.load(root.resolve("atf-setup/config/eventuate-report.yaml")).reportingSettings();
        assertEquals(240,settings.assertionMaxDisplayCharacters());assertEquals(12000,settings.bodyMaxDisplayCharacters());
        assertThrows(IllegalStateException.class,()->AtfReportConfigurationLoader.load(Path.of("missing-explicit-eventuate-report.yaml")));
    }
}
