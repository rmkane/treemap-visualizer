package org.acme.treemap.core.maven;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PackagedJarAnalyzerTest {

    @Test
    void attributesPackagedEntriesToOwningDependencies(@TempDir Path tmp) throws Exception {
        Path repo = tmp.resolve("repo");
        ArtifactKey depA = key("g", "a", "1");
        ArtifactKey depB = key("g", "b", "1");
        writeJar(
                artifactJarPath(repo, depA),
                "com/example/A.class",
                "a-bytes",
                "META-INF/services/x",
                "a-service");
        writeJar(artifactJarPath(repo, depB), "com/example/B.class", "b-bytes");

        DependencyNode root = node(key("g", "root", "1"));
        DependencyNode a = node(depA);
        DependencyNode b = node(depB);
        root.children().add(a);
        root.children().add(b);

        Path out = tmp.resolve("out.jar");
        writeJar(
                out,
                "com/example/A.class",
                "a-bytes",
                "com/example/B.class",
                "b-bytes",
                "app/Main.class",
                "main-bytes");

        PackagedJarAnalyzer.Result result = new PackagedJarAnalyzer(new LocalArtifactResolver(repo))
                .applyPackagedSizes(root, out);

        assertEquals("a-bytes".getBytes(StandardCharsets.UTF_8).length, a.selfSizeBytes());
        assertEquals("b-bytes".getBytes(StandardCharsets.UTF_8).length, b.selfSizeBytes());
        assertEquals("main-bytes".getBytes(StandardCharsets.UTF_8).length, result.unknownBytes());
        assertEquals(0L, result.overlapEntryCount());
    }

    private static ArtifactKey key(String group, String artifact, String version) {
        return new ArtifactKey(group, artifact, "jar", version, "compile", Optional.empty());
    }

    private static DependencyNode node(ArtifactKey key) {
        return new DependencyNode(key);
    }

    private static Path artifactJarPath(Path repo, ArtifactKey key) {
        return repo.resolve(key.groupId().replace('.', '/'))
                .resolve(key.artifactId())
                .resolve(key.version())
                .resolve(key.fileStem() + ".jar");
    }

    private static void writeJar(Path path, String... entriesAndContent) throws IOException {
        Files.createDirectories(path.getParent());
        try (OutputStream os = Files.newOutputStream(path); JarOutputStream jos = new JarOutputStream(os)) {
            for (int i = 0; i < entriesAndContent.length; i += 2) {
                String name = entriesAndContent[i];
                String content = entriesAndContent[i + 1];
                JarEntry e = new JarEntry(name);
                jos.putNextEntry(e);
                jos.write(content.getBytes(StandardCharsets.UTF_8));
                jos.closeEntry();
            }
        }
    }
}
