package org.acme.treemap.core.maven;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Attributes bytes from the built output JAR to dependency artifacts by
 * matching entry names against resolved dependency JAR contents.
 */
public final class PackagedJarAnalyzer {

    public record Result(long attributedBytes, long unknownBytes, long overlapEntryCount) {
    }

    private final LocalArtifactResolver resolver;

    public PackagedJarAnalyzer(LocalArtifactResolver resolver) {
        this.resolver = resolver;
    }

    public Result applyPackagedSizes(DependencyNode root, Path packagedJar) throws IOException {
        if (!Files.isRegularFile(packagedJar)) {
            throw new IOException("Packaged JAR not found: " + packagedJar);
        }
        List<DependencyNode> nodes = flatten(root);
        Map<ArtifactKey, DependencyNode> byKey = new HashMap<>();
        Map<String, List<ArtifactKey>> ownersByJarName = new HashMap<>();
        for (DependencyNode node : nodes) {
            node.setSelfSizeBytes(0L);
            byKey.put(node.key(), node);
            Optional<Path> p = resolver.findPath(node.key());
            if (p.isPresent()) {
                String fileName = p.get().getFileName().toString();
                ownersByJarName.computeIfAbsent(fileName, k -> new ArrayList<>()).add(node.key());
            }
        }

        Map<String, List<ArtifactKey>> ownersByEntry = buildOwnersIndex(nodes);

        long attributed = 0L;
        long unknown = 0L;
        long overlapEntries = 0L;
        try (JarFile out = new JarFile(packagedJar.toFile())) {
            for (JarEntry e : java.util.Collections.list(out.entries())) {
                if (e.isDirectory()) {
                    continue;
                }
                long size = entrySize(out, e);
                if (size <= 0) {
                    continue;
                }
                String entryName = e.getName();
                if (isNoisyMetadataEntry(entryName)) {
                    continue;
                }
                List<ArtifactKey> owners = ownersByEntry.get(entryName);
                if (owners == null || owners.isEmpty()) {
                    owners = ownersByJarName.get(nestedJarName(entryName));
                }
                if (owners == null || owners.isEmpty()) {
                    unknown += size;
                    continue;
                }
                if (owners.size() > 1) {
                    overlapEntries++;
                }
                long perOwner = size / owners.size();
                long remainder = size % owners.size();
                for (int i = 0; i < owners.size(); i++) {
                    ArtifactKey k = owners.get(i);
                    DependencyNode node = byKey.get(k);
                    if (node != null) {
                        long share = perOwner + (i < remainder ? 1L : 0L);
                        node.setSelfSizeBytes(node.selfSizeBytes() + share);
                    }
                }
                attributed += size;
            }
        }
        return new Result(attributed, unknown, overlapEntries);
    }

    public static Optional<Path> detectDefaultPackagedJar(Path projectDir) throws IOException {
        Path target = projectDir.resolve("target");
        if (!Files.isDirectory(target)) {
            return Optional.empty();
        }
        try (var stream = Files.list(target)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".jar"))
                    .filter(p -> !p.getFileName().toString().startsWith("original-"))
                    .filter(p -> !p.getFileName().toString().endsWith("-sources.jar"))
                    .filter(p -> !p.getFileName().toString().endsWith("-javadoc.jar"))
                    .max(Comparator.comparingLong(PackagedJarAnalyzer::safeMtime));
        }
    }

    private Map<String, List<ArtifactKey>> buildOwnersIndex(List<DependencyNode> nodes) throws IOException {
        Map<String, List<ArtifactKey>> ownersByEntry = new HashMap<>();
        for (DependencyNode node : nodes) {
            Optional<Path> jar = resolver.findPath(node.key());
            if (jar.isEmpty()) {
                continue;
            }
            try (JarFile jf = new JarFile(jar.get().toFile())) {
                for (JarEntry e : java.util.Collections.list(jf.entries())) {
                    if (e.isDirectory()) {
                        continue;
                    }
                    if (isNoisyMetadataEntry(e.getName())) {
                        continue;
                    }
                    ownersByEntry.computeIfAbsent(e.getName(), k -> new ArrayList<>()).add(node.key());
                }
            } catch (IOException ex) {
                // Skip unreadable artifacts and continue analysis.
            }
        }
        return ownersByEntry;
    }

    private static List<DependencyNode> flatten(DependencyNode root) {
        List<DependencyNode> out = new ArrayList<>();
        Deque<DependencyNode> stack = new ArrayDeque<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            DependencyNode n = stack.pop();
            out.add(n);
            for (DependencyNode c : n.children()) {
                stack.push(c);
            }
        }
        return out;
    }

    private static long entrySize(JarFile jar, JarEntry e) throws IOException {
        long s = e.getSize();
        if (s >= 0) {
            return s;
        }
        long compressed = e.getCompressedSize();
        if (compressed >= 0) {
            return compressed;
        }
        // Fallback for jars where central directory size is unavailable.
        try (InputStream in = jar.getInputStream(e)) {
            long total = 0;
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) >= 0) {
                total += n;
            }
            return total;
        }
    }

    private static String nestedJarName(String entryName) {
        if (!entryName.endsWith(".jar")) {
            return "";
        }
        int idx = entryName.lastIndexOf('/');
        return idx >= 0 ? entryName.substring(idx + 1) : entryName;
    }

    private static boolean isNoisyMetadataEntry(String entryName) {
        if (entryName == null) {
            return true;
        }
        String n = entryName;
        if ("META-INF/MANIFEST.MF".equals(n)) {
            return true;
        }
        if (n.startsWith("META-INF/LICENSE") || n.startsWith("META-INF/NOTICE")) {
            return true;
        }
        if ("module-info.class".equals(n) || n.endsWith("/module-info.class")) {
            return true;
        }
        return false;
    }

    private static long safeMtime(Path p) {
        try {
            return Files.getLastModifiedTime(p).toMillis();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
