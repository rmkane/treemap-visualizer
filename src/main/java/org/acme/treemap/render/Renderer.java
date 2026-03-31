package org.acme.treemap.render;

import org.acme.treemap.maven.DependencyNode;

import java.io.IOException;

/**
 * Strategy for writing a dependency treemap to a file (PNG, HTML, JSON, YAML, etc.).
 * <p>
 * Add new formats by implementing this interface and registering the implementation in
 * {@link Renderers#forOutputFormat(org.acme.treemap.OutputFormat)} (and extend
 * {@link org.acme.treemap.OutputFormat} if the CLI should select it).
 */
@FunctionalInterface
public interface Renderer {

    /**
     * @param root   dependency tree with {@link DependencyNode#selfSizeBytes()} populated
     * @param options format-agnostic render options (destination, title, and optional canvas sizing)
     */
    void render(DependencyNode root, OutputOptions options) throws IOException;
}
