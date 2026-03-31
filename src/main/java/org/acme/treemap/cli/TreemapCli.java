package org.acme.treemap.cli;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Backward-compatible alias; prefer {@link Entrypoint}. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TreemapCli {

    public static void main(String[] args) {
        Entrypoint.main(args);
    }
}
