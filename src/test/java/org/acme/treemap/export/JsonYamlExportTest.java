package org.acme.treemap.export;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import org.acme.treemap.maven.ArtifactKey;
import org.acme.treemap.maven.DependencyNode;
import org.acme.treemap.render.JsonExporter;
import org.acme.treemap.render.YamlExporter;

class JsonYamlExportTest {

    @Test
    void jsonRoundTrip(@TempDir Path dir) throws Exception {
        DependencyNode root = sampleTree();
        Path out = dir.resolve("t.json");
        new JsonExporter()
                .generate(root, o -> o
                        .output(out)
                        .width(100)
                        .height(80)
                        .title("Title"));

        TreemapSnapshot read = new ObjectMapper().readValue(out.toFile(), TreemapSnapshot.class);
        String text = Files.readString(out);
        assertEquals("Title", read.title());
        assertNull(read.width());
        assertNull(read.height());
        assertTrue(!text.contains("\"width\""));
        assertTrue(!text.contains("\"height\""));
        assertEquals("root-arti", read.root().artifactId());
        assertEquals(2, read.root().children().size());
        assertEquals("leaf", read.root().children().getFirst().artifactId());
        assertNull(read.root().children().getFirst().classifier());
    }

    @Test
    void yamlRoundTrip(@TempDir Path dir) throws Exception {
        DependencyNode root = sampleTree();
        Path out = dir.resolve("t.yaml");
        new YamlExporter()
                .generate(root, o -> o
                        .output(out)
                        .width(100)
                        .height(80)
                        .title("Title"));

        String text = Files.readString(out);
        assertTrue(text.contains("title: Title"));
        assertTrue(text.contains("root-arti"));
        assertTrue(!text.contains("\nwidth:"));
        assertTrue(!text.contains("\nheight:"));

        TreemapSnapshot read = new ObjectMapper(new YAMLFactory()).readValue(out.toFile(), TreemapSnapshot.class);
        assertEquals("root-arti", read.root().artifactId());
    }

    private static DependencyNode sampleTree() {
        DependencyNode root = new DependencyNode(
                new ArtifactKey("g.root", "root-arti", "jar", "1", "compile", Optional.empty()));
        root.setSelfSizeBytes(10);
        DependencyNode child = new DependencyNode(
                new ArtifactKey("g", "leaf", "jar", "2", "compile", Optional.empty()));
        child.setSelfSizeBytes(20);
        root.children().add(child);
        root.children().add(new DependencyNode(
                new ArtifactKey(
                        "g",
                        "classified",
                        "jar",
                        "3",
                        "compile",
                        Optional.of("sources"))));
        root.children().get(1).setSelfSizeBytes(5);
        root.children().get(1).children().add(new DependencyNode(
                new ArtifactKey("g", "grand", "jar", "1", "compile", Optional.empty())));
        root.children().get(1).children().getFirst().setSelfSizeBytes(1);
        return root;
    }
}
