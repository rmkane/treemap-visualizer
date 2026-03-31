package org.acme.treemap.core.render;

import java.nio.file.Path;
import java.util.Objects;

import lombok.Builder;

/** Shared render configuration passed to all renderer strategies. */
@Builder(builderClassName = "Builder")
public record OutputOptions(Path output, int width, int height, String title) {

    public OutputOptions {
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(title, "title");
        if (width <= 0) {
            throw new IllegalArgumentException("width must be > 0");
        }
        if (height <= 0) {
            throw new IllegalArgumentException("height must be > 0");
        }
    }

    /**
     * Partial builder declared to supply default values; Lombok generates the
     * fluent setter methods ({@code output()}, {@code width()}, {@code height()},
     * {@code title()}), the {@code build()} method, and the static
     * {@code builder()} factory on the record.
     */
    public static final class Builder {
        private int width = 1200;
        private int height = 800;
        private String title = "Dependency Treemap";
    }
}
