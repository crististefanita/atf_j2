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
    }
    private static List<String> selected(String source) {
        String arguments = AtfJavaSourceReferences.annotationArguments(source, "SelectClasses").orElseThrow();
        return Pattern.compile("(\\w+)\\.class").matcher(arguments).results().map(match -> match.group(1)).toList();
    }
}
