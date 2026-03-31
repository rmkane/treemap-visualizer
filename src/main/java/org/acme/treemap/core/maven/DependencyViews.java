package org.acme.treemap.core.maven;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Helpers to create alternate visualization views from dependency trees. */
public final class DependencyViews {

    private DependencyViews() {
    }

    /**
     * Returns a flattened tree where top-level children are two buckets:
     * {@code direct} and {@code transitive}. Artifacts are deduplicated by key.
     */
    public static DependencyNode toFlatByDepth(DependencyNode root) {
        DependencyNode viewRoot = cloneNode(root.key(), root.selfSizeBytes());
        DependencyNode direct = bucket("direct");
        DependencyNode transitive = bucket("transitive");

        Map<ArtifactKey, Integer> minDepth = new HashMap<>();
        Map<ArtifactKey, Long> sizeByKey = new HashMap<>();

        ArrayDeque<Frame> stack = new ArrayDeque<>();
        stack.push(new Frame(root, 0));
        while (!stack.isEmpty()) {
            Frame f = stack.pop();
            DependencyNode node = f.node();
            int depth = f.depth();

            minDepth.merge(node.key(), depth, Math::min);
            sizeByKey.merge(node.key(), node.selfSizeBytes(), Math::max);
            for (DependencyNode c : node.children()) {
                stack.push(new Frame(c, depth + 1));
            }
        }

        for (Map.Entry<ArtifactKey, Integer> e : minDepth.entrySet()) {
            ArtifactKey key = e.getKey();
            int depth = e.getValue();
            if (depth == 0) {
                continue;
            }
            long selfSize = sizeByKey.getOrDefault(key, 0L);
            DependencyNode copy = cloneNode(key, selfSize);
            if (depth == 1) {
                direct.children().add(copy);
            } else {
                transitive.children().add(copy);
            }
        }
        if (!direct.children().isEmpty()) {
            viewRoot.children().add(direct);
        }
        if (!transitive.children().isEmpty()) {
            viewRoot.children().add(transitive);
        }
        return viewRoot;
    }

    private static DependencyNode bucket(String name) {
        ArtifactKey k = new ArtifactKey(
                "view",
                name,
                "pom",
                "1",
                "compile",
                Optional.empty());
        return new DependencyNode(k);
    }

    private static DependencyNode cloneNode(ArtifactKey key, long selfSizeBytes) {
        DependencyNode n = new DependencyNode(key);
        n.setSelfSizeBytes(selfSizeBytes);
        return n;
    }

    private record Frame(DependencyNode node, int depth) {
    }
}
