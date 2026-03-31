package org.acme.treemap.core.maven;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class DependencyScopePrunerTest {

    @Test
    void prunesTestAndProvidedScopes() {
        DependencyNode root = node("root", "compile");
        DependencyNode compileChild = node("compile-child", "compile");
        DependencyNode runtimeChild = node("runtime-child", "runtime");
        DependencyNode testChild = node("test-child", "test");
        DependencyNode providedChild = node("provided-child", "provided");
        DependencyNode nestedUnderTest = node("test-nested", "compile");

        testChild.children().add(nestedUnderTest);
        root.children().add(compileChild);
        root.children().add(runtimeChild);
        root.children().add(testChild);
        root.children().add(providedChild);

        int removed = DependencyScopePruner.pruneNonPackagedScopes(root);

        assertEquals(3, removed);
        assertEquals(2, root.children().size());
        assertEquals("compile-child", root.children().get(0).key().artifactId());
        assertEquals("runtime-child", root.children().get(1).key().artifactId());
    }

    @Test
    void scopeMatchIsCaseInsensitive() {
        DependencyNode root = node("root", "compile");
        root.children().add(node("x", "TEST"));

        int removed = DependencyScopePruner.pruneNonPackagedScopes(root);

        assertEquals(1, removed);
        assertEquals(0, root.children().size());
    }

    private static DependencyNode node(String artifactId, String scope) {
        return new DependencyNode(new ArtifactKey("g", artifactId, "jar", "1", scope, Optional.empty()));
    }
}
