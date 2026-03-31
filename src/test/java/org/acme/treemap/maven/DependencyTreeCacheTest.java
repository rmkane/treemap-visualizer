package org.acme.treemap.maven;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DependencyTreeCacheTest {

    @Test
    void loadMissWhenEmpty(@TempDir Path dir) throws Exception {
        DependencyTreeCache cache = new DependencyTreeCache(dir);
        assertTrue(cache.load("abc").isEmpty());
    }

    @Test
    void saveAndLoadRoundTrip(@TempDir Path dir) throws Exception {
        DependencyTreeCache cache = new DependencyTreeCache(dir);
        List<String> lines = List.of("[INFO] a:b:jar:1:compile", "[INFO] BUILD SUCCESS");
        cache.save("deadbeef", lines);
        Optional<List<String>> loaded = cache.load("deadbeef");
        assertTrue(loaded.isPresent());
        assertEquals(lines, loaded.get());
    }

    @Test
    void pomChecksumStable(@TempDir Path dir) throws Exception {
        Path pom = dir.resolve("pom.xml");
        Files.writeString(pom, "<project></project>\n", StandardCharsets.UTF_8);
        String a = PomChecksum.sha256Hex(pom);
        String b = PomChecksum.sha256Hex(pom);
        assertEquals(a, b);
        Files.writeString(pom, "<project/>", StandardCharsets.UTF_8);
        String c = PomChecksum.sha256Hex(pom);
        assertTrue(!a.equals(c));
    }
}
