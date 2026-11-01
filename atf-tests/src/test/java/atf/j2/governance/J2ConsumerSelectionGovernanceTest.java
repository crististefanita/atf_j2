package atf.j2.governance;
import atf.core.files.AtfJavaSourceReferences;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.List;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;
@AtfComponentTest
public class J2ConsumerSelectionGovernanceTest {
    private static final List<String> CASES = List.of("EventuateApiContractComponentTest", "EventuateConvergenceComponentTest", "EventuateMessageEvidenceComponentTest");
    private static final List<String> SQL_CASES = List.of("EventuateSqlTransactionComponentTest", "EventuateSqlIdentityComponentTest", "EventuateSqlFailureComponentTest");
    @Test void selectsExactlyThreeComponentCasesInTheCanonicalSuite() throws Exception {
        Path tests = J2Project.root().resolve("atf-test-eventuate/src/test/java/atf/eventuate/tests");
        String suite = Files.readString(tests.resolve("suites/EventuateComponentSuite.java"));
        assertEquals(CASES, selected(suite));
        try (var files = Files.list(tests.resolve("component"))) {
            assertEquals(CASES.stream().sorted().toList(), files.filter(path -> path.toString().endsWith(".java")).map(path -> path.getFileName().toString().replace(".java", "")).sorted().toList());
        }
        for (String name : CASES) {
            String code = AtfJavaSourceReferences.codeOnly(Files.readString(tests.resolve("component/" + name + ".java")));
            assertTrue(code.contains("@EventuateComponentTest"));
            assertEquals(1, Pattern.compile("@Test\\b").matcher(code).results().count());
        }
        String owner = Files.readString(tests.resolve("support/EventuateComponentTest.java"));
        assertTrue(AtfJavaSourceReferences.codeOnly(owner).contains("@AtfComponentTest"));
        assertTrue(AtfJavaSourceReferences.codeOnly(owner).contains("@AtfReportPath"));
    }
    @Test void rejectsMissingAndRepeatedSuiteSelections() {
        assertNotEquals(CASES, selected("@SelectClasses({EventuateApiContractComponentTest.class}) class Suite {}"));
        assertNotEquals(CASES, selected("@SelectClasses({EventuateApiContractComponentTest.class, EventuateApiContractComponentTest.class}) class Suite {}"));
        assertNotEquals(SQL_CASES, selected("@SelectClasses({EventuateSqlTransactionComponentTest.class}) class Suite {}"));
        assertNotEquals(SQL_CASES, selected("@SelectClasses({EventuateSqlTransactionComponentTest.class, EventuateSqlTransactionComponentTest.class}) class Suite {}"));
    }
    @Test void selectsExactlyThreeSqlComponentsAndKeepsTheDriverTestScoped() throws Exception {
        Path module = J2Project.root().resolve("atf-test-eventuate");
        Path tests = module.resolve("src/test/java/atf/eventuate/tests");
        assertEquals(SQL_CASES, selected(Files.readString(tests.resolve("suites/EventuateSqlComponentSuite.java"))));
        var expectedPaths = java.util.stream.Stream.concat(CASES.stream().map(name -> name + ".java"),
                SQL_CASES.stream().map(name -> "sql/" + name + ".java")).sorted().toList();
        try (var files = Files.walk(tests.resolve("component"))) {
            assertEquals(expectedPaths, files.filter(path -> path.toString().endsWith(".java"))
                    .map(path -> tests.resolve("component").relativize(path).toString().replace('\\','/')).sorted().toList());
        }
        try (var files = Files.list(tests.resolve("component/sql"))) {
            assertEquals(SQL_CASES.stream().sorted().toList(), files.filter(path -> path.toString().endsWith(".java"))
                    .map(path -> path.getFileName().toString().replace(".java", "")).sorted().toList());
        }
        for (String name : SQL_CASES) {
            String source = AtfJavaSourceReferences.codeOnly(Files.readString(tests.resolve("component/sql/" + name + ".java")));
            assertTrue(source.contains("@EventuateComponentTest"));
            assertEquals(1, Pattern.compile("@Test\\b").matcher(source).results().count());
            assertFalse(CASES.contains(name));
        }
        var dependencies = J2Project.pom(module).getElementsByTagName("dependency");
        int h2 = 0;
        for (int i = 0; i < dependencies.getLength(); i++) {
            var dependency = (org.w3c.dom.Element) dependencies.item(i);
            if (dependency.getElementsByTagName("artifactId").item(0).getTextContent().equals("h2")) {
                h2++;
                assertEquals("com.h2database", dependency.getElementsByTagName("groupId").item(0).getTextContent());
                assertEquals("test", dependency.getElementsByTagName("scope").item(0).getTextContent());
                assertTrue(dependency.getElementsByTagName("version").item(0).getTextContent().matches("\\d+\\.\\d+\\.\\d+"));
            }
        }
        assertEquals(1, h2);
        try (var files = Files.walk(module.resolve("src/main/java"))) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList())
                assertFalse(AtfJavaSourceReferences.codeOnly(Files.readString(file)).contains("org.h2."), "H2 implementation leaked into main: " + file);
        }
    }
    private static List<String> selected(String source) {
        String arguments = AtfJavaSourceReferences.annotationArguments(source, "SelectClasses").orElseThrow();
        return Pattern.compile("(\\w+)\\.class").matcher(arguments).results().map(match -> match.group(1)).toList();
    }
}
