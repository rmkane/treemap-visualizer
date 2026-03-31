package org.acme.treemap;

import org.acme.treemap.cli.Entrypoint;
import org.acme.treemap.core.TreemapCommand;

/**
 * Legacy compatibility alias for tests and older invocations; prefer
 * {@link Entrypoint}.
 */
public final class Main extends TreemapCommand {

    public static void main(String[] args) {
        Entrypoint.main(args);
    }
}
