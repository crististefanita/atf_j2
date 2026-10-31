package atf.j2.governance;
import atf.core.files.AtfJavaSourceReferences;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
@AtfComponentTest
public class J2OwnershipGovernanceTest {
    @Test void keepsJunitOutOfConsumerMainAndBusinessOutOfPlatform() throws Exception {
        Path root = J2Project.root();
        for (String module : J2Project.MODULES) {
            Path source = root.resolve(module + "/src/main");
            if (!Files.isDirectory(source)) continue;
            try (var files = Files.walk(source)) {
                for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                    String text = Files.readString(file);
                    for (String foreign : List.of("atf.shop", "atf.autoex")) assertFalse(AtfJavaSourceReferences.containsNamespace(text, foreign), file.toString());
                    if (module.equals("atf-test-eventuate")) for (String forbidden : List.of("org.junit", "atf.testkit"))
                        assertFalse(AtfJavaSourceReferences.containsNamespace(text, forbidden), file.toString());
                    else assertFalse(AtfJavaSourceReferences.containsNamespace(text, "atf.eventuate"), file.toString());
                }
            }
        }
        assertFalse(Files.isDirectory(root.resolve("atf-tests/src/main")), "Governance owns tests only");
    }
    @Test void detectsRealQualifiedReferencesAndIgnoresExamples() {
        assertTrue(AtfJavaSourceReferences.containsNamespace("class Bad { org.junit.jupiter.api.Test test; }", "org.junit"));
        assertFalse(AtfJavaSourceReferences.containsNamespace("// org.junit example\nclass Good { String name = \"org.junit\"; }", "org.junit"));
    }
    @Test void headlessSourcesCannotReferenceBrowserModules() throws Exception {
        for (String module : J2Project.MODULES) {
            Path source = J2Project.root().resolve(module + "/src");
            if (!Files.isDirectory(source)) continue;
            try (var files = Files.walk(source)) {
                for (Path file : files.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".java")).toList()) {
                    String code = Files.readString(file);
                    for (String namespace : List.of("atf.ui", "atf.debugui", "com.microsoft.playwright"))
                        assertFalse(AtfJavaSourceReferences.containsNamespace(code, namespace), file + " references " + namespace);
                }
            }
        }
    }
    @Test void keepsNewPackagesCohesiveAndNonempty() throws Exception {
        Path root = J2Project.root();
        for (String source : List.of("atf-test-eventuate/src/main/java", "atf-test-eventuate/src/test/java", "atf-tests/src/test/java/atf/j2/governance")) {
            var packages = atf.core.files.AtfJavaSourceInventory.packages(root.resolve(source));
            assertFalse(packages.isEmpty());
            packages.forEach((name, files) -> assertTrue(files.size() <= 8, name + " has " + files.size() + " classes"));
        }
    }
}
