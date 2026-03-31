package org.acme.treemap.cli;

import org.acme.treemap.core.TreemapCommand;

import picocli.CommandLine;

/** Canonical CLI entrypoint for treemap generation. */
public final class Entrypoint {

    private Entrypoint() {
    }

    public static void main(String[] args) {
        int exit = new CommandLine(new TreemapCommand()).execute(args);
        System.exit(exit);
    }
}
