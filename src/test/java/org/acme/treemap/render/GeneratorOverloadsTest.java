package org.acme.treemap.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.acme.treemap.maven.ArtifactKey;
import org.acme.treemap.maven.DependencyNode;

class GeneratorOverloadsTest {

    @Test
    void generateWithLambdaBuilder(@TempDir Path dir) throws Exception {
        DependencyNode root = sampleRoot();
        Path out = dir.resolve("out.json");

        Generator g = new JsonExporter();
        g.generate(root, b -> b.output(out).title("from-lambda"));

        String text = Files.readString(out);
        assertTrue(text.contains("\"title\" : \"from-lambda\""));
    }

    @Test
    void generateWithBaseAndOverride(@TempDir Path dir) throws Exception {
        DependencyNode root = sampleRoot();
        Path out = dir.resolve("out.yaml");
        OutputOptions base = OutputOptions.builder()
                .output(out)
                .title("base")
                .build();

        Generator g = new YamlExporter();
        g.generate(root, base, b -> b.title("override"));

        String text = Files.readString(out);
        assertTrue(text.contains("title: override"));
    }

    @Test
    void baseOptionsCarryDimensions() {
        OutputOptions base = OutputOptions.builder().output(Path.of("x")).width(123).height(456).title("t").build();
        assertEquals(123, base.width());
        assertEquals(456, base.height());
    }

    private static DependencyNode sampleRoot() {
        DependencyNode root = new DependencyNode(
                new ArtifactKey("g", "a", "jar", "1", "compile", Optional.empty()));
        root.setSelfSizeBytes(1);
        return root;
    }
}
