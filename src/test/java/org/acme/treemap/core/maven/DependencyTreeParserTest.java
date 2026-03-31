package org.acme.treemap.core.maven;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class DependencyTreeParserTest {

    @Test
    void parsesMavenStyleTreeWithPipeAndSpaceIndent() {
        List<String> lines = List.of(
                "[INFO] Scanning for projects...",
                "[INFO] --- dependency:3.7.0:tree (default-cli) @ demo ---",
                "[INFO] com.demo:app:jar:1.0.0-SNAPSHOT",
                "[INFO] +- org.slf4j:slf4j-api:jar:2.0.9:compile",
                "[INFO] \\- ch.qos.logback:logback-classic:jar:1.5.3:compile",
                "[INFO]    +- ch.qos.logback:logback-core:jar:1.5.3:compile",
                "[INFO]    \\- org.slf4j:slf4j-api:jar:2.0.9:compile",
                "[INFO] BUILD SUCCESS");

        DependencyNode root = DependencyTreeParser.parse(lines);
        assertEquals("app", root.key().artifactId());
        assertEquals(2, root.children().size());
        DependencyNode logback = root.children().get(1);
        assertEquals("logback-classic", logback.key().artifactId());
        assertEquals(2, logback.children().size());
    }

    @Test
    void parseCoordinatesFourPartDefaultsScopeToCompile() {
        ArtifactKey k = DependencyTreeParser.parseCoordinates("g:a:jar:1.0");
        assertEquals("g", k.groupId());
        assertEquals("a", k.artifactId());
        assertEquals("compile", k.scope());
        assertEquals(Optional.empty(), k.classifier());
    }

    @Test
    void parseCoordinatesSixPartIncludesClassifier() {
        ArtifactKey k = DependencyTreeParser.parseCoordinates("g:a:jar:sources:1.0:compile");
        assertEquals("1.0", k.version());
        assertEquals(Optional.of("sources"), k.classifier());
        assertEquals("a-1.0-sources", k.fileStem());
    }

    @Test
    void parseCoordinatesInvalidReturnsNull() {
        assertTrue(DependencyTreeParser.parseCoordinates("not-a-coord") == null);
        assertTrue(DependencyTreeParser.parseCoordinates("a:b:c") == null);
    }

    @Test
    void parseEmptyOutputThrows() {
        assertThrows(IllegalArgumentException.class, () -> DependencyTreeParser.parse(List.of("[INFO] BUILD SUCCESS")));
    }

    @Test
    void parseRejectsChildLineAsSecondRoot() {
        List<String> lines = List.of(
                "[INFO] root:proj:jar:1.0:compile",
                "[INFO] +- other:lib:jar:2:compile");
        DependencyNode root = DependencyTreeParser.parse(lines);
        assertEquals(1, root.children().size());
        assertEquals("lib", root.children().getFirst().key().artifactId());
    }
}
