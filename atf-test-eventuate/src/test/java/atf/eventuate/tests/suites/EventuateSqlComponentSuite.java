package atf.eventuate.tests.suites;

import atf.testkit.AtfIntegrationTest;
import atf.eventuate.tests.component.sql.*;
import org.junit.platform.suite.api.*;

@AtfIntegrationTest
@Suite
@SelectClasses({EventuateSqlTransactionComponentTest.class, EventuateSqlIdentityComponentTest.class, EventuateSqlFailureComponentTest.class})
@ConfigurationParameter(key = "atf.test.run.scope", value = "suite")
@ConfigurationParameter(key = "junit.jupiter.execution.parallel.enabled", value = "false")
public class EventuateSqlComponentSuite { }
