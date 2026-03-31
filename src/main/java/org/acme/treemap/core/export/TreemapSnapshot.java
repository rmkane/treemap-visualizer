package org.acme.treemap.core.export;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Serializable dependency tree + chart metadata for JSON / YAML export. */
public record TreemapSnapshot(
        String title,
        @JsonInclude(JsonInclude.Include.NON_NULL) Integer width,
        @JsonInclude(JsonInclude.Include.NON_NULL) Integer height,
        Node root) {

    public record Node(
            String groupId,
            String artifactId,
            String packaging,
            String version,
            String scope,
            @JsonInclude(JsonInclude.Include.NON_NULL) String classifier,
            long selfSizeBytes,
            long subtreeUniqueBytes,
            List<Node> children) {
    }
}
