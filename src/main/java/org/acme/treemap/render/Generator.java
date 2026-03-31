package org.acme.treemap.render;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Function;

import org.acme.treemap.maven.DependencyNode;

/**
 * Strategy for generating an output artifact from a dependency tree (PNG, HTML,
 * JSON, YAML, etc.).
 * <p>
 * Add new formats by implementing this interface and registering the
 * implementation in
 * {@link Generators#forOutputFormat(org.acme.treemap.OutputFormat)} (and extend
 * {@link org.acme.treemap.OutputFormat} if the CLI should select it).
 */
@FunctionalInterface
public interface Generator {

    /**
     * @param root    dependency tree with {@link DependencyNode#selfSizeBytes()}
     *                populated
     * @param options output options (destination, title, and optional canvas
     *                sizing)
     */
    void generate(DependencyNode root, OutputOptions options) throws IOException;

    /**
     * Convenience overload that configures {@link OutputOptions.Builder} with a
     * function returning the (possibly same) builder, enabling fluent expression
     * lambdas like {@code g.generate(root, b -> b.output(path).title("foo"))}.
     */
    default void generate(
            DependencyNode root, Function<OutputOptions.Builder, OutputOptions.Builder> configureFn)
            throws IOException {
        Objects.requireNonNull(configureFn, "configureFn");
        OutputOptions.Builder configured = configureFn.apply(OutputOptions.builder());
        generate(root, configured.build());
    }

    /**
     * Function-style overload that starts from existing options and returns an
     * updated builder.
     */
    default void generate(
            DependencyNode root,
            OutputOptions base,
            Function<OutputOptions.Builder, OutputOptions.Builder> configureFn)
            throws IOException {
        Objects.requireNonNull(base, "base");
        Objects.requireNonNull(configureFn, "configureFn");
        OutputOptions.Builder start = OutputOptions.builder()
                .output(base.output())
                .width(base.width())
                .height(base.height())
                .title(base.title());
        OutputOptions.Builder configured = configureFn.apply(start);
        generate(root, configured.build());
    }
}
