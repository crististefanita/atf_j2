package atf.eventuate.tests.support.acceptance.live;

import atf.eventuate.live.LiveSettings;
import atf.eventuate.live.LiveDeadline;
import atf.eventuate.live.kafka.KafkaObserver;
import atf.eventuate.live.kafka.KafkaEvidence;
import atf.eventuate.live.kafka.KafkaClientSettings;
import atf.sql.execution.SqlExecutor;
import atf.sql.execution.SqlExecutionFailure;
import atf.sql.query.SqlQuery;
import atf.sql.query.SqlParameter;
import atf.sql.result.SqlSnapshot;
import atf.testkit.assertions.RecordedAssertions;
import atf.testkit.assertions.AssertionValueDisplay;
import atf.reporting.audit.AssertionAuditReport;
import atf.reporting.configuration.AtfReportConfigurationLoader;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.html.HtmlReportSession;
import atf.reporting.model.ReportObservation;
import atf.reporting.model.ReportField;
import atf.reporting.model.diagnostic.ReportPhaseDiagnostic;
import atf.reporting.security.SensitiveDataRedactor;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.nio.file.Path;
import java.nio.file.Files;
import java.sql.DriverManager;
import java.sql.JDBCType;
import java.sql.SQLTimeoutException;
import java.time.Duration;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** One low-memory REAL Kafka/MySQL probe; business event is synthetic and Eventuate application is NOT exercised. */
public final class InfrastructureAcceptance {
    private static final ObjectMapper JSON = new ObjectMapper().registerModule(new JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS);
    private InfrastructureAcceptance() { }
    public static void main(String[] args) throws Exception {
        Path output=Path.of(args[0]).toAbsolutePath();
        if(Files.exists(output))throw new IllegalArgumentException("Fresh infrastructure evidence directory required");
        Files.createDirectories(output);
        var config=AtfReportConfigurationLoader.load(Path.of(args[1])).reportingSettings(); var redactor=new SensitiveDataRedactor();
        var checks=new RecordedAssertions(new AssertionValueDisplay(config.assertionMaxDisplayCharacters(),redactor::redactField));
        var sql=new ArrayList<SqlSnapshot>(); var kafka=new ArrayList<KafkaEvidence>();
        Throwable primary=null;
        long executionStarted=System.nanoTime();
        try { execute(LiveSettings.load(),checks,sql,kafka); }
        catch(Exception|Error failure) { primary=failure; }
        Duration executionElapsed=Duration.ofNanos(Math.max(0,System.nanoTime()-executionStarted));
        Throwable executionFailure=primary;
        var artifactAudits=new LinkedHashMap<String,String>();
        for(String renderer:List.of("EXTENT","FALLBACK")) {
            try {
                Path file=output.resolve("infrastructure-"+renderer.toLowerCase(Locale.ROOT)+".html");
                try(AtfReportPorts.Session session=renderer.equals("EXTENT")?new ExtentReportSession(file,config):new HtmlReportSession(file,config)) {
                  try(var scenario=session.startScenario("EventuateInfrastructureProbe",InfrastructureAcceptance.class.getPackageName(),
                            "REAL Kafka and read-only MySQL · synthetic event · application NOT RUN",List.of("component","infrastructure","kafka","sql","synthetic-event"))) {
                   try(var phase=scenario.startPhase("TEST BODY")) {
                    phase.executionTiming(new atf.reporting.model.timing.ReportPhaseTiming(executionElapsed,1));
                    phase.observation(new ReportObservation("Infrastructure provenance",List.of(new ReportField("Kafka target","CONFIGURED_REAL_BROKER"),
                            new ReportField("SQL target","CONFIGURED_REAL_MYSQL_READ_ONLY"),new ReportField("Kafka capture",kafka.isEmpty()?"NOT_OBSERVED":"RECORDED"),
                            new ReportField("SQL capture",sql.isEmpty()?"NOT_EXECUTED":"RECORDED"),new ReportField("Business event","SYNTHETIC_FIXTURE"),
                            new ReportField("Eventuate application","NOT_RUN")),List.of(),List.of()));
                    for(var snapshot:kafka) phase.observation(new ReportObservation("Kafka observer capture",List.of(),
                            List.of(new ReportObservation.Body("Real broker coordinates",JSON.writeValueAsString(snapshot),ReportObservation.Format.JSON)),List.of()));
                    for(var snapshot:sql) phase.observation(new ReportObservation("Independent JDBC capture · "+snapshot.outcome(),List.of(),
                            List.of(new ReportObservation.Body("Query and typed result",JSON.writeValueAsString(snapshot),ReportObservation.Format.JSON)),List.of()));
                    AssertionAuditReport.publishStep(phase,"Component assertions",checks.checks().stream().map(check->
                            new AtfAssertion(check.name(),check.expected(),check.actual(),check.passed())).toList(),AtfReportStatus.INFO);
                    if(executionFailure!=null) phase.diagnostic(ReportPhaseDiagnostic.Kind.EXECUTION_FAILURE,redactor.redactText(executionFailure.toString()));
                    phase.complete(executionFailure==null?AtfReportStatus.PASS:AtfReportStatus.FAIL);
                   }
                    scenario.complete(executionFailure==null?AtfReportStatus.PASS:AtfReportStatus.FAIL);
                  }
                }
                InfrastructureReportOracle.require(Files.readString(file),sql,kafka,executionFailure!=null);
                artifactAudits.put(renderer,"PASS");
            } catch(Exception|Error publication) {
                artifactAudits.put(renderer,"FAIL");
                if(primary==null)primary=publication;else if(publication!=primary)primary.addSuppressed(publication);
            }
        }
        var receipt=new LinkedHashMap<String,Object>(); receipt.put("status",primary==null?"PASS":"FAIL");
        receipt.put("source","REAL_INFRASTRUCTURE_SYNTHETIC_BUSINESS_EVENT"); receipt.put("applicationAcceptance","NOT_RUN");
        receipt.put("checks",checks.checks().size());receipt.put("sqlExecutions",sql.size());receipt.put("kafkaCaptures",kafka.size());
        receipt.put("executionElapsed",executionElapsed.toString());
        receipt.put("assertionMaxDisplayCharacters",config.assertionMaxDisplayCharacters());
        receipt.put("bodyMaxDisplayCharacters",config.bodyMaxDisplayCharacters());
        receipt.put("artifactAudits",artifactAudits);
        if(primary!=null) receipt.put("failure",Map.of("type",primary.getClass().getName(),"message",redactor.redactText(java.util.Objects.toString(primary.getMessage(),"")),
                "cause",redactor.redactText(java.util.Objects.toString(primary.getCause(),"none"))));
        receipt.put("coreCodeSource",atf.core.http.request.ApiRequest.class.getProtectionDomain().getCodeSource().getLocation().toString());
        receipt.put("reportingCodeSource",ExtentReportSession.class.getProtectionDomain().getCodeSource().getLocation().toString());
        JSON.writerWithDefaultPrettyPrinter().writeValue(output.resolve("receipt.json").toFile(),receipt);
        if(primary instanceof Exception exception)throw exception;if(primary instanceof Error error)throw error;
        System.out.println("ATF_INFRASTRUCTURE_PROBE=PASS; APP_NOT_RUN; checks="+checks.checks().size());
    }
    private static void execute(LiveSettings settings,RecordedAssertions checks,List<SqlSnapshot> sql,List<KafkaEvidence> kafka) throws Exception {
        var deadline=new LiveDeadline(Duration.ofSeconds(20));
        String topic="atf-j2-live-probe-"+UUID.randomUUID().toString().replace("-","");
        String messageId="synthetic-"+UUID.randomUUID(), aggregate="probe-"+UUID.randomUUID();
        var properties=KafkaClientSettings.admin(settings.bootstrap());
        try(var admin=AdminClient.create(properties)) {
            boolean creationAttempted=false;Throwable primary=null;
            try {
                creationAttempted=true;
                admin.createTopics(List.of(new NewTopic(topic,1,(short)1))).all().get(5,TimeUnit.SECONDS);
                try(var observer=new KafkaObserver(settings.bootstrap())) {
                    try {
                        observer.prepare(List.of(topic),deadline);
                        checks.isTrue("Observer readiness precedes synthetic producer",observer.snapshot().readyBeforeMutation());
                        var producerProperties=KafkaClientSettings.probeProducer(settings.bootstrap());
                        String wire=JSON.writeValueAsString(Map.of("headers",Map.of("ID",messageId,"event-aggregate-id",aggregate,
                                "event-type","atf.probe.SyntheticEvent"),"payload","{}"));
                        try(var producer=new KafkaProducer<String,String>(producerProperties)) {
                            var ack=producer.send(new ProducerRecord<>(topic,aggregate,wire)).get(5,TimeUnit.SECONDS);
                            observer.await(events->events.stream().anyMatch(event->event.messageId().equals(messageId)),deadline);
                            var event=observer.snapshot().events().stream().filter(value->value.messageId().equals(messageId)).findFirst().orElseThrow();
                            checks.equals("Synthetic event delivered once",1L,observer.snapshot().events().stream().filter(value->value.messageId().equals(messageId)).count());
                            checks.equals("Broker acknowledgement matches observed offset",ack.offset(),event.offset());
                            checks.equals("Broker partition matches acknowledgement",ack.partition(),event.partition());
                            checks.equals("Synthetic aggregate correlation",aggregate,event.aggregateId());
                        }
                    } finally {kafka.add(observer.snapshot());}
                }
                try(var executor=new SqlExecutor(remaining->{
                    var jdbc=new Properties();jdbc.put("user",settings.jdbcUser());jdbc.put("password",settings.jdbcPassword());
                    jdbc.put("connectTimeout",Long.toString(Math.max(1,remaining.toMillis())));jdbc.put("socketTimeout",Long.toString(Math.max(1,remaining.toMillis())));
                    return DriverManager.getConnection(settings.jdbcUrl(),jdbc);
                },sql::add)) {
                    var result=executor.query(new SqlQuery("SELECT CAST(? AS DECIMAL(30,2)) AS amount, CAST(? AS CHAR) AS literal_value, CAST(NULL AS CHAR) AS missing",
                            List.of(new SqlParameter(JDBCType.DECIMAL,new BigDecimal("9007199254740993.01")),
                                    new SqlParameter(JDBCType.VARCHAR,"x' OR 1=1 --"))),deadline.cap(Duration.ofSeconds(5))).requireComplete();
                    checks.isTrue("MySQL acknowledged independent read-only connection",result.readOnlyAcknowledged());
                    checks.isTrue("SQL resources closed before publication",result.resourcesClosed());
                    checks.equals("Exact MySQL decimal",0,new BigDecimal("9007199254740993.01").compareTo((BigDecimal)result.rows().getFirst().getFirst().value()));
                    checks.equals("Bind remains a literal value","x' OR 1=1 --",result.rows().getFirst().get(1).value());
                    checks.equals("SQL NULL remains distinct",null,result.rows().getFirst().get(2).value());
                    java.sql.SQLException timeout=null;
                    try {executor.query(new SqlQuery("SELECT SLEEP(2)"),Duration.ofMillis(100));}catch(java.sql.SQLException failure){timeout=failure;}
                    checks.isTrue("Real MySQL operation is bounded by its deadline",timeout!=null);
                    checks.isTrue("Deadline failure has a timeout classification",timeout instanceof SQLTimeoutException || socketTimeout(timeout));
                    checks.equals("Terminal timeout capture is FAILED",SqlSnapshot.Outcome.FAILED,SqlExecutionFailure.find(timeout).orElseThrow().outcome());
                    checks.equals("No automatic SQL replay",2,sql.size());
                }
            } catch(Exception|Error failure){primary=failure;throw failure;}
            finally {if(creationAttempted)try{admin.deleteTopics(List.of(topic)).all().get(5,TimeUnit.SECONDS);}catch(Exception cleanup){if(primary==null)throw cleanup;primary.addSuppressed(cleanup);}}
        }
    }
    private static boolean socketTimeout(Throwable failure) {
        var seen=Collections.newSetFromMap(new IdentityHashMap<Throwable,Boolean>());
        for(var value=failure;value!=null && seen.size()<32 && seen.add(value);value=value.getCause())
            if(value instanceof java.net.SocketTimeoutException)return true;
        return false;
    }
}
