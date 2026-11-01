package atf.j2.governance;
import java.nio.file.*;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
/** Explicit J2 deployment; absent modules never shrink the expected perimeter. */
final class J2Project {
    static final List<String> MODULES = List.of("atf-test-kit", "atf-mock", "atf-sql", "atf-validation", "atf-console", "atf-setup", "atf-test-eventuate", "atf-tests");
    static Path root() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null && !Files.isRegularFile(current.resolve("platform-provenance.json"))) current = current.getParent();
        if (current == null) throw new IllegalStateException("J2 root not found");
        return current;
    }
    static org.w3c.dom.Document pom(Path root) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, ""); factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory.newDocumentBuilder().parse(root.resolve("pom.xml").toFile());
    }
    static void requireDeployment(Path root) throws Exception {
        var pom = pom(root);
        var declared = new java.util.ArrayList<String>();
        var nodes = pom.getElementsByTagName("module");
        for (int i = 0; i < nodes.getLength(); i++) declared.add(nodes.item(i).getTextContent().strip());
        if (!declared.equals(MODULES)) throw new IllegalStateException("J2 requires its exact eight-module perimeter: " + declared);
        for (String module : MODULES) if (!Files.isRegularFile(root.resolve(module).resolve("pom.xml"))) throw new IllegalStateException("Missing module POM: " + module);
        try (var children = Files.list(root)) {
            for (Path child : children.filter(Files::isDirectory).toList()) {
                if (Files.isRegularFile(child.resolve("pom.xml")) && !MODULES.contains(child.getFileName().toString()))
                    throw new IllegalStateException("Undeclared module: " + child.getFileName());
            }
        }
        for (String module : MODULES) {
            var dependencies = pom(root.resolve(module)).getElementsByTagName("dependency");
            for (int i = 0; i < dependencies.getLength(); i++) {
                var dependency = (org.w3c.dom.Element) dependencies.item(i);
                var ids = dependency.getElementsByTagName("artifactId");
                if (ids.getLength() > 0 && List.of("atf-ui", "atf-debug-ui", "playwright").contains(ids.item(0).getTextContent().strip()))
                    throw new IllegalStateException("UI dependency is outside J2: " + module);
            }
        }
        for (String forbidden : List.of("atf-core", "atf-reporting", "atf-reporting-tests", "atf-test-practice-shop", "atf-test-auto-ex", "atf-ui", "atf-debug-ui"))
            if (Files.exists(root.resolve(forbidden))) throw new IllegalStateException("Foreign source module in J2: " + forbidden);
    }
    static String sha256(Path file) throws Exception {
        return java.util.HexFormat.of().withUpperCase().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));
    }
}
