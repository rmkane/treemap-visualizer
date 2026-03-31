package org.acme.treemap.maven;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** One row in the Maven dependency tree. */
public final class DependencyNode {

    private final ArtifactKey key;
    private final List<DependencyNode> children = new ArrayList<>();
    private long selfSizeBytes;

    public DependencyNode(ArtifactKey key) {
        this.key = key;
    }

    public ArtifactKey key() {
        return key;
    }

    public List<DependencyNode> children() {
        return children;
    }

    public long selfSizeBytes() {
        return selfSizeBytes;
    }

    public void setSelfSizeBytes(long selfSizeBytes) {
        this.selfSizeBytes = selfSizeBytes;
    }

    /**
     * Sum of {@link #selfSizeBytes()} for every distinct {@link ArtifactKey} in
     * this subtree (avoids double-counting when the same GAV appears on multiple
     * tree paths).
     */
    public long subtreeUniqueBytes() {
        Set<ArtifactKey> seen = new HashSet<>();
        return accumulateUnique(this, seen);
    }

    private static long accumulateUnique(DependencyNode node, Set<ArtifactKey> seen) {
        long sum = 0;
        if (seen.add(node.key)) {
            sum += node.selfSizeBytes;
        }
        for (DependencyNode c : node.children) {
            sum += accumulateUnique(c, seen);
        }
        return sum;
    }
}
