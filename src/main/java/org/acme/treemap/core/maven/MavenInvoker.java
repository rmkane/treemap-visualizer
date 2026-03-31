package org.acme.treemap.core.maven;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Runs {@code mvn} (or project {@code mvnw}) in a directory and captures
 * stdout.
 */
public final class MavenInvoker {

    private static final int TIMEOUT_MINUTES = 15;

    private final Path projectDir;
    private final List<String> mvnCommand;

    public MavenInvoker(Path projectDir) {
        this.projectDir = projectDir;
        this.mvnCommand = detectMvnCommand(projectDir);
    }

    private static List<String> detectMvnCommand(Path projectDir) {
        boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
        Path unix = projectDir.resolve("mvnw");
        Path win = projectDir.resolve("mvnw.cmd");
        if (!windows && Files.isRegularFile(unix) && Files.isExecutable(unix)) {
            return List.of(unix.toAbsolutePath().toString());
        }
        if (windows && Files.isRegularFile(win)) {
            return List.of(win.toAbsolutePath().toString());
        }
        return List.of("mvn");
    }

    public List<String> dependencyTree() throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>(mvnCommand);
        cmd.add("-f");
        cmd.add(projectDir.resolve("pom.xml").toAbsolutePath().toString());
        cmd.add("--batch-mode");
        cmd.add("dependency:tree");
        cmd.add("-DoutputType=text");

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(projectDir.toFile());
        pb.redirectErrorStream(true);
        Process p = pb.start();

        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }

        boolean finished = p.waitFor(TIMEOUT_MINUTES, TimeUnit.MINUTES);
        if (!finished) {
            p.destroyForcibly();
            throw new IOException("mvn dependency:tree timed out after " + TIMEOUT_MINUTES + " minutes");
        }
        if (p.exitValue() != 0) {
            throw new IOException("mvn dependency:tree failed with exit code "
                    + p.exitValue()
                    + ". Output:\n"
                    + String.join("\n", lines));
        }
        return lines;
    }

    /**
     * Resolves {@code settings.localRepository} by running Maven in
     * {@code projectDir}.
     */
    public Path resolveLocalRepository() throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>(mvnCommand);
        cmd.add("-f");
        cmd.add(projectDir.resolve("pom.xml").toAbsolutePath().toString());
        cmd.add("--batch-mode");
        cmd.add("-q");
        cmd.add("help:evaluate");
        cmd.add("-Dexpression=settings.localRepository");
        cmd.add("-DforceStdout");

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(projectDir.toFile());
        pb.redirectErrorStream(true);
        Process p = pb.start();

        StringBuilder out = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line).append('\n');
            }
        }

        boolean finished = p.waitFor(2, TimeUnit.MINUTES);
        if (!finished) {
            p.destroyForcibly();
            throw new IOException("mvn help:evaluate timed out");
        }
        if (p.exitValue() != 0) {
            throw new IOException("mvn help:evaluate failed: " + out);
        }
        String path = out.toString().strip();
        if (path.isEmpty()) {
            return LocalArtifactResolver.defaultLocalRepository();
        }
        return Path.of(path);
    }
}
