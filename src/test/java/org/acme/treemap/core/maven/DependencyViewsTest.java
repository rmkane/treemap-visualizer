package org.acme.treemap.core.maven;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class DependencyViewsTest {

    @Test
    void flatViewSplitsDirectAndTransitiveBuckets() {
        DependencyNode root = node("root", "compile", 1);
        DependencyNode directA = node("a", "compile", 10);
        DependencyNode transitiveC = node("c", "compile", 30);
        DependencyNode directB = node("b", "compile", 20);

        directA.children().add(transitiveC);
        root.children().add(directA);
        root.children().add(directB);

        DependencyNode flat = DependencyViews.toFlatByDepth(root);

        assertEquals(2, flat.children().size());
        DependencyNode directBucket = flat.children().get(0);
        DependencyNode transitiveBucket = flat.children().get(1);
        assertEquals("direct", directBucket.key().artifactId());
        assertEquals("transitive", transitiveBucket.key().artifactId());
        assertEquals(2, directBucket.children().size());
        assertEquals(1, transitiveBucket.children().size());
    }

    private static DependencyNode node(String artifactId, String scope, long size) {
        DependencyNode n = new DependencyNode(new ArtifactKey("g", artifactId, "jar", "1", scope, Optional.empty()));
        n.setSelfSizeBytes(size);
        return n;
    }
}
