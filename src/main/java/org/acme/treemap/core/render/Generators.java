package org.acme.treemap.core.render;

import java.util.Objects;

import org.acme.treemap.core.OutputFormat;

/** Resolves an {@link OutputFormat} to a {@link Generator} implementation. */
public final class Generators {

    private Generators() {
    }

    /**
     * Returns the generator used for CLI / {@code --format} selection. When adding
     * a new {@link OutputFormat} value, add a {@code case} here and a corresponding
     * {@link Generator} class.
     */
    public static Generator forOutputFormat(OutputFormat format) {
        Objects.requireNonNull(format, "format");
        return switch (format) {
        case PNG -> new PngRenderer();
        case HTML -> new HtmlRenderer();
        case JSON -> new JsonExporter();
        case YAML -> new YamlExporter();
        };
    }
}
