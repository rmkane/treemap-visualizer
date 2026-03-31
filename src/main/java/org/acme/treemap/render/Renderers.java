package org.acme.treemap.render;

import org.acme.treemap.OutputFormat;

import java.util.Objects;

/** Resolves an {@link OutputFormat} to a {@link Renderer} implementation. */
public final class Renderers {

    private Renderers() {}

    /**
     * Returns the renderer used for CLI / {@code --format} selection.
     * When adding a new {@link OutputFormat} value, add a {@code case} here and a corresponding
     * {@link Renderer} class.
     */
    public static Renderer forOutputFormat(OutputFormat format) {
        Objects.requireNonNull(format, "format");
        return switch (format) {
            case PNG -> new PngRenderer();
            case HTML -> new HtmlRenderer();
            case JSON -> new JsonRenderer();
            case YAML -> new YamlRenderer();
        };
    }
}
