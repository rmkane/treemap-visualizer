package org.acme.treemap.core.maven;

import java.util.Objects;
import java.util.Optional;

/** Maven coordinates from {@code dependency:tree} lines. */
public record ArtifactKey(
        String groupId,
        String artifactId,
        String packaging,
        String version,
        String scope,
        Optional<String> classifier) {

    public ArtifactKey {
        Objects.requireNonNull(groupId);
        Objects.requireNonNull(artifactId);
        Objects.requireNonNull(packaging);
        Objects.requireNonNull(version);
        scope = scope == null || scope.isEmpty() ? "compile" : scope;
        classifier = classifier == null ? Optional.empty() : classifier;
    }

    public String shortLabel() {
        return groupId + ":" + artifactId;
    }

    /** Base file name without extension (handles optional classifier). */
    public String fileStem() {
        return classifier.filter(s -> !s.isEmpty())
                .map(c -> artifactId + "-" + version + "-" + c)
                .orElse(artifactId + "-" + version);
    }
}
