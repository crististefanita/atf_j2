package atf.j2.governance;
import atf.testkit.AtfIntegrationTest;
import org.junit.platform.suite.api.*;
@AtfIntegrationTest
@Suite
@SelectClasses({J2DeploymentGovernanceTest.class, J2OwnershipGovernanceTest.class,
        J2ConsumerSelectionGovernanceTest.class, J2DocumentationGovernanceTest.class})
public class J2GovernanceSuite { }
