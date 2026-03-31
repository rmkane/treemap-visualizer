package org.acme.treemap.core.render;

import java.nio.file.Path;
import java.util.Objects;

/** Shared render configuration passed to all renderer strategies. */
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

    /** Fluent builder entrypoint (Elastic-style). */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Path output;
        private int width = 1200;
        private int height = 800;
        private String title = "Dependency Treemap";

        public Builder output(Path output) {
            this.output = output;
            return this;
        }

        public Builder width(int width) {
            this.width = width;
            return this;
        }

        public Builder height(int height) {
            this.height = height;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public OutputOptions build() {
            return new OutputOptions(output, width, height, title);
        }
    }
}
