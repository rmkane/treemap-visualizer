package org.acme.treemap.export;

import java.util.List;

import org.acme.treemap.maven.DependencyNode;

public final class TreemapSnapshots {

    private TreemapSnapshots() {
    }

    public static TreemapSnapshot from(DependencyNode root, String title) {
        return from(root, title, null, null);
    }

    public static TreemapSnapshot from(
            DependencyNode root, String title, Integer width, Integer height) {
        return new TreemapSnapshot(title, width, height, toNode(root));
    }

    private static TreemapSnapshot.Node toNode(DependencyNode n) {
        String classifier = n.key().classifier().filter(s -> !s.isEmpty()).orElse(null);
        List<TreemapSnapshot.Node> children = n.children().stream().map(TreemapSnapshots::toNode).toList();
        return new TreemapSnapshot.Node(
                n.key().groupId(),
                n.key().artifactId(),
                n.key().packaging(),
                n.key().version(),
                n.key().scope(),
                classifier,
                n.selfSizeBytes(),
                n.subtreeUniqueBytes(),
                children);
    }
}
