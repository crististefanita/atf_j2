package atf.eventuate.tests.suites;
import atf.testkit.AtfIntegrationTest;
import atf.eventuate.tests.component.*;
import org.junit.platform.suite.api.*;
@AtfIntegrationTest
@Suite
@SelectClasses({EventuateApiContractComponentTest.class, EventuateConvergenceComponentTest.class, EventuateMessageEvidenceComponentTest.class})
@ConfigurationParameter(key = "atf.test.run.scope", value = "suite")
@ConfigurationParameter(key = "junit.jupiter.execution.parallel.enabled", value = "false")
public class EventuateComponentSuite { }
