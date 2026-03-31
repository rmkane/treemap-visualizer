package org.acme.treemap.cli;

/** Backward-compatible alias; prefer {@link Entrypoint}. */
public final class TreemapCli {

    private TreemapCli() {
    }

    public static void main(String[] args) {
        Entrypoint.main(args);
    }
}
