package atf.j2.governance;

import atf.testkit.AtfComponentTest;
import atf.core.files.AtfJavaSourceReferences;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/** SQL is an explicit J2 module; its runtime must remain driver/renderer/JUnit independent. */
@AtfComponentTest
public class J2SqlModuleGovernanceTest {
    @Test void keepsNativeSqlRuntimeIndependentAndH2TestScoped() throws Exception {
        Path root=J2Project.root(), module=root.resolve("atf-sql");
        J2Project.requireDeployment(root);
        try(var files=Files.walk(module.resolve("src/main/java"))) {
            for(Path file:files.filter(path->path.toString().endsWith(".java")).toList()) {
                String source=Files.readString(file);
                for(String forbidden:List.of("org.junit","atf.testkit","atf.reporting","org.h2","com.mysql","atf.eventuate"))
                    assertFalse(AtfJavaSourceReferences.containsNamespace(source,forbidden),()->file+" contains "+forbidden);
            }
        }
        var dependencies=J2Project.pom(module).getElementsByTagName("dependency");
        for(int index=0;index<dependencies.getLength();index++) {
            var dependency=(org.w3c.dom.Element)dependencies.item(index);
            assertEquals("test",dependency.getElementsByTagName("scope").item(0).getTextContent(),"SQL runtime must depend only on the JDK");
        }
    }
    @Test void preservesFiniteNativeSuiteAndExplicitLiveOptIn() throws Exception {
        Path root=J2Project.root();
        String suite=Files.readString(root.resolve("atf-sql/src/test/java/atf/sql/tests/suites/SqlComponentSuite.java"));
        String arguments=AtfJavaSourceReferences.annotationArguments(suite,"SelectClasses").orElseThrow();
        assertEquals(List.of("SqlResultComponentTest","SqlTransactionComponentTest","SqlCaptureFailureComponentTest"),
                java.util.regex.Pattern.compile("(\\w+)\\.class").matcher(arguments).results().map(match->match.group(1)).toList());
        String live=AtfJavaSourceReferences.codeOnly(Files.readString(root.resolve("atf-test-eventuate/src/test/java/atf/eventuate/tests/support/EventuateLiveTest.java")));
        assertTrue(live.contains("@AtfE2eTest")); assertTrue(live.contains("@EnabledIfSystemProperty"));
        String setup=Files.readString(root.resolve("atf-setup/Start-EventuateLive.ps1"));
        assertFalse(setup.contains("'stop'")); assertFalse(setup.contains("'rm'"));
    }
}
