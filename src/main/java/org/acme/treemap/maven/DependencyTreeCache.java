package org.acme.treemap.maven;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;

/**
 * Caches raw {@code mvn dependency:tree} stdout keyed by {@link PomChecksum} of {@code pom.xml}.
 * Default directory follows XDG Base Directory: {@code $XDG_CACHE_HOME/treemap-visualize/dependency-tree}
 * or {@code ~/.cache/treemap-visualize/dependency-tree}.
 */
public final class DependencyTreeCache {

    private final Path directory;

    public DependencyTreeCache(Path directory) {
        this.directory = directory;
    }

    public static Path defaultDirectory() {
        String home = System.getProperty("user.home");
        if (home == null || home.isEmpty()) {
            throw new IllegalStateException("user.home is not set");
        }
        String xdg = System.getenv("XDG_CACHE_HOME");
        Path base =
                (xdg != null && !xdg.isEmpty()) ? Path.of(xdg) : Path.of(home, ".cache");
        return base.resolve("treemap-visualize").resolve("dependency-tree");
    }

    public Optional<List<String>> load(String pomSha256Hex) throws IOException {
        Path file = directory.resolve(pomSha256Hex + ".txt");
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        return Optional.of(Files.readAllLines(file, StandardCharsets.UTF_8));
    }

    public void save(String pomSha256Hex, List<String> lines) throws IOException {
        Files.createDirectories(directory);
        Path target = directory.resolve(pomSha256Hex + ".txt");
        Path tmp = Files.createTempFile(directory, "tree-", ".tmp");
        try {
            Files.write(tmp, lines, StandardCharsets.UTF_8);
            try {
                Files.move(
                        tmp,
                        target,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.copy(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(tmp);
        }
    }
}
