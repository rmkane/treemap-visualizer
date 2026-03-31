package org.acme.treemap.cli;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import org.acme.treemap.core.TreemapCommand;

import picocli.CommandLine;

/** Canonical CLI entrypoint for treemap generation. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Entrypoint {

    public static void main(String[] args) {
        int exit = new CommandLine(new TreemapCommand()).execute(args);
        System.exit(exit);
    }
}
