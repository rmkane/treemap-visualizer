package org.acme.treemap.core.maven;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Resolves artifact files under the local Maven repository and reports size on
 * disk.
 */
public final class LocalArtifactResolver {

    private final Path localRepository;

    public LocalArtifactResolver(Path localRepository) {
        this.localRepository = localRepository;
    }

    public static Path defaultLocalRepository() {
        String home = System.getProperty("user.home");
        if (home == null || home.isEmpty()) {
            throw new IllegalStateException("user.home is not set");
        }
        return Path.of(home, ".m2", "repository");
    }

    public long resolveSizeBytes(ArtifactKey key) {
        return findPath(key).map(this::safeSize).orElse(0L);
    }

    private long safeSize(Path p) {
        try {
            return Files.isRegularFile(p) ? Files.size(p) : 0L;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static long sizeOrZero(Path p) {
        try {
            return Files.isRegularFile(p) ? Files.size(p) : 0L;
        } catch (IOException e) {
            return 0L;
        }
    }

    public Optional<Path> findPath(ArtifactKey key) {
        Path base = localRepository.resolve(key.groupId().replace('.', '/')).resolve(key.artifactId())
                .resolve(key.version());

        String ext = extensionForPackaging(key.packaging());
        Path exact = base.resolve(key.fileStem() + "." + ext);
        if (Files.isRegularFile(exact)) {
            return Optional.of(exact);
        }

        if (key.version().endsWith("SNAPSHOT")) {
            return findSnapshotArtifact(base, key, ext);
        }

        return Optional.empty();
    }

    private static String extensionForPackaging(String packaging) {
        return switch (packaging) {
        case "jar", "war", "ear", "bundle" -> "jar";
        case "pom" -> "pom";
        default -> packaging;
        };
    }

    private Optional<Path> findSnapshotArtifact(Path versionDir, ArtifactKey key, String ext) {
        if (!Files.isDirectory(versionDir)) {
            return Optional.empty();
        }
        String prefix = key.artifactId() + "-";
        String suffix = "-SNAPSHOT." + ext;
        try (Stream<Path> stream = Files.list(versionDir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> {
                        String name = p.getFileName().toString();
                        return name.startsWith(prefix) && name.endsWith(suffix);
                    })
                    .max(Comparator.comparingLong(p -> sizeOrZero(p)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void applySizes(DependencyNode root) {
        applySizesRecursive(root);
    }

    private void applySizesRecursive(DependencyNode node) {
        node.setSelfSizeBytes(resolveSizeBytes(node.key()));
        for (DependencyNode c : node.children()) {
            applySizesRecursive(c);
        }
    }
}
