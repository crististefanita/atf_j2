package atf.j2.governance;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;
@AtfComponentTest
public class J2DocumentationGovernanceTest {
    @Test void keepsMarkdownTargetsRealAndDiagramsExplained() throws Exception {
        Path root = J2Project.root();
        try (var files = Files.walk(root)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".md") && !path.toString().contains("target") && !path.toString().contains(".git")).toList()) {
                String text = Files.readString(file);
                assertFalse(text.contains("\uFFFD"), file.toString());
                String prose = text.replaceAll("(?s)```.*?```", "");
                var links = Pattern.compile("!?\\[[^\\]]*]\\(([^)]+)\\)").matcher(prose);
                while (links.find()) {
                    String target = links.group(1).split("#", 2)[0];
                    if (target.isEmpty() || target.contains("://") || target.startsWith("mailto:")) continue;
                    assertTrue(Files.exists(file.getParent().resolve(java.net.URLDecoder.decode(target, java.nio.charset.StandardCharsets.UTF_8)).normalize()), file + " -> " + target);
                }
                var diagrams = Pattern.compile("(?s)```mermaid\\R(.*?)```\\s*(.*)").matcher(text);
                while (diagrams.find()) {
                    assertTrue(diagrams.group(1).contains("%%{init:"), file.toString());
                    assertTrue(diagrams.group(2).startsWith("<details>"), "Diagram needs its adjacent legend: " + file);
                }
            }
        }
    }
    @Test void documentsTheActualDeploymentAndLibraryOwner() throws Exception {
        Path root = J2Project.root();
        String architecture = Files.readString(root.resolve("docs/ATF_Architecture.md"));
        assertTrue(architecture.contains("exact cele 7 module"));
        String atlas = Files.readString(root.resolve("docs/ATF_Context_Atlas.md"));
        assertTrue(atlas.contains("demo2") && atlas.contains("atf_j0") && atlas.contains("MOCK"));
    }
}
