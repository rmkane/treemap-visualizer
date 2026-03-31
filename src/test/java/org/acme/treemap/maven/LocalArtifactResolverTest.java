package org.acme.treemap.maven;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalArtifactResolverTest {

    @Test
    void resolvesJarSizeInStandardRepoLayout(@TempDir Path repo) throws Exception {
        Path jar = repo.resolve("com/example/demo-lib/2.0/demo-lib-2.0.jar");
        Files.createDirectories(jar.getParent());
        Files.writeString(jar, "0123456789");

        ArtifactKey key = new ArtifactKey("com.example", "demo-lib", "jar", "2.0", "compile", Optional.empty());
        LocalArtifactResolver resolver = new LocalArtifactResolver(repo);

        assertEquals(10L, resolver.resolveSizeBytes(key));
        assertTrue(resolver.findPath(key).isPresent());
    }

    @Test
    void missingArtifactReturnsZero(@TempDir Path repo) {
        ArtifactKey key = new ArtifactKey("missing", "artifact", "jar", "1", "compile", Optional.empty());
        assertEquals(0L, new LocalArtifactResolver(repo).resolveSizeBytes(key));
    }

    @Test
    void classifierFileStem(@TempDir Path repo) throws Exception {
        ArtifactKey key = new ArtifactKey("g", "a", "jar", "1", "compile", Optional.of("sources"));
        Path jar = repo.resolve("g/a/1/a-1-sources.jar");
        Files.createDirectories(jar.getParent());
        Files.writeString(jar, "x");

        assertEquals(1L, new LocalArtifactResolver(repo).resolveSizeBytes(key));
    }
}
