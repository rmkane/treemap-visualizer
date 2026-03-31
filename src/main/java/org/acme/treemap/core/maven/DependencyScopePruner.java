package org.acme.treemap.core.maven;

import java.util.Iterator;
import java.util.Set;

/**
 * Removes dependency scopes that do not normally end up in packaged runtime
 * artifacts.
 */
public final class DependencyScopePruner {

    private static final Set<String> NON_PACKAGED_SCOPES = Set.of("test", "provided");

    private DependencyScopePruner() {
    }

    /**
     * Prunes nodes in-place and returns number of removed nodes (including child
     * subtrees under removed parents).
     */
    public static int pruneNonPackagedScopes(DependencyNode root) {
        return pruneChildren(root);
    }

    private static int pruneChildren(DependencyNode node) {
        int removed = 0;
        Iterator<DependencyNode> it = node.children().iterator();
        while (it.hasNext()) {
            DependencyNode child = it.next();
            if (isNonPackagedScope(child.key().scope())) {
                removed += countSubtree(child);
                it.remove();
                continue;
            }
            removed += pruneChildren(child);
        }
        return removed;
    }

    private static int countSubtree(DependencyNode node) {
        int count = 1;
        for (DependencyNode child : node.children()) {
            count += countSubtree(child);
        }
        return count;
    }

    private static boolean isNonPackagedScope(String scope) {
        return scope != null && NON_PACKAGED_SCOPES.contains(scope.toLowerCase());
    }
}
