package atf.j2.governance;
import atf.testkit.AtfComponentTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.zip.ZipFile;
import static org.junit.jupiter.api.Assertions.*;
@AtfComponentTest
public class J2DeploymentGovernanceTest {
    @TempDir Path fixture;
    @Test void requiresDeclaredModulesAndSourceFreeReporting() throws Exception { J2Project.requireDeployment(J2Project.root()); }
    @Test void rejectsMissingDuplicateAndForeignModules() throws Exception {
        for (String value : java.util.List.of("missing", "duplicate", "foreign")) {
            seed();
            Path pom = fixture.resolve("pom.xml");
            String xml = Files.readString(pom);
            xml = switch (value) {
                case "missing" -> xml.replace("<module>atf-mock</module>", "");
                case "duplicate" -> xml.replace("<module>atf-mock</module>", "<module>atf-mock</module><module>atf-mock</module>");
                default -> xml.replace("<module>atf-mock</module>", "<module>unknown</module>");
            };
            Files.writeString(pom, xml);
            assertThrows(IllegalStateException.class, () -> J2Project.requireDeployment(fixture), value);
        }
    }
    @Test void rejectsUndeclaredReportingSourceAndMissingPom() throws Exception {
        seed(); Files.createDirectories(fixture.resolve("atf-reporting"));
        assertThrows(IllegalStateException.class, () -> J2Project.requireDeployment(fixture));
        Files.delete(fixture.resolve("atf-reporting")); Files.delete(fixture.resolve("atf-mock/pom.xml"));
        assertThrows(IllegalStateException.class, () -> J2Project.requireDeployment(fixture));
    }
    @Test void rejectsUiSourceDirectoriesEvenWithoutAPom() throws Exception {
        seed();
        for (String module : java.util.List.of("atf-ui", "atf-debug-ui")) {
            Files.createDirectory(fixture.resolve(module));
            assertThrows(IllegalStateException.class, () -> J2Project.requireDeployment(fixture));
            Files.delete(fixture.resolve(module));
        }
    }
    @Test void rejectsUiDependenciesOutsideTheDeclaredReactor() throws Exception {
        seed();
        for (String artifact : java.util.List.of("atf-ui", "atf-debug-ui", "playwright")) {
            Files.writeString(fixture.resolve("atf-mock/pom.xml"), "<project><dependencies><dependency><artifactId>"
                    + artifact + "</artifactId></dependency></dependencies></project>");
            assertThrows(IllegalStateException.class, () -> J2Project.requireDeployment(fixture));
        }
    }
    @Test void bindsReportingCoordinatePomAndLoadedBinary() throws Exception {
        Path root = J2Project.root();
        var provenance = new ObjectMapper().readTree(root.resolve("library-provenance.json").toFile());
        String version = J2Project.pom(root).getElementsByTagName("atf.reporting.version").item(0).getTextContent();
        assertEquals("atf.api:atf-reporting:" + version, provenance.path("coordinate").asText());
        Path jar = root.resolve("lib/atf-reporting-" + version + ".jar");
        Path pom = root.resolve("lib/atf-reporting-" + version + ".pom");
        assertEquals(provenance.path("binarySha256").asText(), J2Project.sha256(jar));
        assertEquals(provenance.path("publishedPomSha256").asText(), J2Project.sha256(pom));
        Path loaded = Path.of(atf.reporting.configuration.ReportingSettings.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        assertTrue(Files.isRegularFile(loaded)); assertEquals(J2Project.sha256(jar), J2Project.sha256(loaded));
        try (var archive = new ZipFile(jar.toFile())) {
            assertArrayEquals(Files.readAllBytes(pom), archive.getInputStream(archive.getEntry("META-INF/maven/atf.api/atf-reporting/pom.xml")).readAllBytes());
            assertTrue(archive.stream().noneMatch(entry -> entry.getName().endsWith(".java")));
        }
    }
    @Test void preservesImportedPlatformSourceBytes() throws Exception {
        Path root = J2Project.root();
        var files = new ObjectMapper().readTree(root.resolve("platform-provenance.json").toFile()).path("files");
        assertTrue(files.size() > 100);
        for (var item : files.properties()) assertEquals(item.getValue().asText().toUpperCase(), J2Project.sha256(root.resolve(item.getKey())), item.getKey());
    }
    private void seed() throws Exception {
        Files.writeString(fixture.resolve("pom.xml"), "<project><modules>" + J2Project.MODULES.stream().map(name -> "<module>" + name + "</module>").collect(java.util.stream.Collectors.joining()) + "</modules></project>");
        for (String module : J2Project.MODULES) { Path p = fixture.resolve(module); Files.createDirectories(p); Files.writeString(p.resolve("pom.xml"), "<project/>"); }
    }
}
