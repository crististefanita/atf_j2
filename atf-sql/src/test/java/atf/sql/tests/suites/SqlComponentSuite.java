package atf.sql.tests.suites;

import atf.sql.tests.component.SqlResultComponentTest;
import atf.sql.tests.component.SqlTransactionComponentTest;
import atf.sql.tests.component.SqlCaptureFailureComponentTest;
import atf.testkit.AtfIntegrationTest;
import org.junit.platform.suite.api.*;

@AtfIntegrationTest
@Suite
@SelectClasses({SqlResultComponentTest.class, SqlTransactionComponentTest.class, SqlCaptureFailureComponentTest.class})
@ConfigurationParameter(key = "atf.test.run.scope", value = "suite")
@ConfigurationParameter(key = "junit.jupiter.execution.parallel.enabled", value = "false")
public class SqlComponentSuite { }
