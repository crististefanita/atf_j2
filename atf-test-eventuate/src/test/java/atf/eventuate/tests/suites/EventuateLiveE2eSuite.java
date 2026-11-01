package atf.eventuate.tests.suites;

import atf.testkit.AtfIntegrationTest;
import atf.eventuate.tests.e2e.EventuateApprovedOrderE2eTest;
import atf.eventuate.tests.e2e.EventuateInsufficientCreditE2eTest;
import atf.eventuate.tests.e2e.EventuateUnknownCustomerE2eTest;
import org.junit.platform.suite.api.*;

@AtfIntegrationTest
@Suite
@SelectClasses({EventuateApprovedOrderE2eTest.class, EventuateInsufficientCreditE2eTest.class, EventuateUnknownCustomerE2eTest.class})
@ConfigurationParameter(key = "atf.test.run.scope", value = "suite")
@ConfigurationParameter(key = "junit.jupiter.execution.parallel.enabled", value = "false")
public class EventuateLiveE2eSuite { }
