package org.acme.treemap.core.maven;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class DependencyNodeTest {

    @Test
    void subtreeUniqueBytesCountsEachArtifactOnce() {
        ArtifactKey slf4j = new ArtifactKey("org.slf4j", "slf4j-api", "jar", "2.0.9", "compile", Optional.empty());
        ArtifactKey logback = new ArtifactKey("ch.qos.logback", "logback-classic", "jar", "1.5", "compile",
                Optional.empty());
        ArtifactKey core = new ArtifactKey("ch.qos.logback", "logback-core", "jar", "1.5", "compile", Optional.empty());

        DependencyNode root = new DependencyNode(
                new ArtifactKey("demo", "app", "jar", "1", "compile", Optional.empty()));
        root.setSelfSizeBytes(100);

        DependencyNode lb = new DependencyNode(logback);
        lb.setSelfSizeBytes(200);
        DependencyNode coreNode = new DependencyNode(core);
        coreNode.setSelfSizeBytes(300);
        DependencyNode slf4jUnderLb = new DependencyNode(slf4j);
        slf4jUnderLb.setSelfSizeBytes(400);

        lb.children().add(coreNode);
        lb.children().add(slf4jUnderLb);

        DependencyNode slf4jDirect = new DependencyNode(slf4j);
        slf4jDirect.setSelfSizeBytes(400);

        root.children().add(lb);
        root.children().add(slf4jDirect);

        assertEquals(100 + 200 + 300 + 400, root.subtreeUniqueBytes());
    }
}
