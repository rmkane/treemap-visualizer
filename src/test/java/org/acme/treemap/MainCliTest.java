package org.acme.treemap;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import picocli.CommandLine;

class MainCliTest {

    @Test
    void helpExitsZero() {
        PrintStream out = System.out;
        System.setOut(new PrintStream(OutputStream.nullOutputStream()));
        try {
            int exit = new CommandLine(new Main()).execute("--help");
            assertEquals(0, exit);
        } finally {
            System.setOut(out);
        }
    }

    @Test
    void missingPomExitsTwo(@TempDir Path emptyDir) {
        PrintStream err = System.err;
        System.setErr(new PrintStream(OutputStream.nullOutputStream()));
        try {
            int exit = new CommandLine(new Main()).execute(emptyDir.toString());
            assertEquals(2, exit);
        } finally {
            System.setErr(err);
        }
    }
}
