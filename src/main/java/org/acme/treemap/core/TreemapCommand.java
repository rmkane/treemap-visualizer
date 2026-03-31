package org.acme.treemap.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.acme.treemap.core.maven.DependencyNode;
import org.acme.treemap.core.maven.DependencyScopePruner;
import org.acme.treemap.core.maven.DependencyTreeCache;
import org.acme.treemap.core.maven.DependencyTreeParser;
import org.acme.treemap.core.maven.LocalArtifactResolver;
import org.acme.treemap.core.maven.MavenInvoker;
import org.acme.treemap.core.maven.PackagedJarAnalyzer;
import org.acme.treemap.core.maven.PomChecksum;
import org.acme.treemap.core.render.Generator;
import org.acme.treemap.core.render.Generators;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "treemap-visualize", description = "Treemap of Maven dependency sizes: PNG, HTML, JSON, or YAML (local ~/.m2 for sizes).", mixinStandardHelpOptions = true)
public class TreemapCommand implements Callable<Integer> {

    private static final Logger log = LoggerFactory.getLogger(TreemapCommand.class);
    private static final long NANOS_PER_MILLISECOND = 1_000_000L;

    @Parameters(index = "0", arity = "0..1", defaultValue = ".", description = "Maven project directory (must contain pom.xml)")
    private Path projectDir;

    @Option(names = { "-o",
            "--output" }, description = "Output path (optional). Default: target/treemap-<projectName>.<ext>")
    private Path output;

    @Option(names = {
            "--format" }, converter = OutputFormatConverter.class, description = "png | html | json | yaml (default: infer from --output extension)")
    private OutputFormat format;

    @Option(names = "--width", defaultValue = "1200", description = "Canvas width in pixels")
    private int width;

    @Option(names = "--height", defaultValue = "800", description = "Canvas height in pixels")
    private int height;

    @Option(names = "--refresh", description = "Skip dependency tree cache and re-run Maven (updates cache)")
    private boolean refresh;

    @Option(names = "--cache-dir", description = "Dependency tree cache directory (default: XDG_CACHE_HOME/.../treemap-visualize/dependency-tree)")
    private Path cacheDir;

    @Option(names = {
            "--analysis-mode" }, description = "dependency | packaged | auto (default: auto)")
    private AnalysisMode analysisMode = AnalysisMode.AUTO;

    @Option(names = "--packaged-jar", description = "Output JAR to inspect in packaged mode (default: auto-detect in target/)")
    private Path packagedJar;

    public static final class OutputFormatConverter implements CommandLine.ITypeConverter<OutputFormat> {
        @Override
        public OutputFormat convert(String value) {
            return OutputFormat.valueOf(value.toUpperCase(Locale.ROOT));
        }
    }

    public OutputFormat resolvedFormat() {
        if (format != null) {
            return format;
        }
        if (output == null) {
            return OutputFormat.PNG;
        }
        String name = output.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".html") || name.endsWith(".htm")) {
            return OutputFormat.HTML;
        }
        if (name.endsWith(".json")) {
            return OutputFormat.JSON;
        }
        if (name.endsWith(".yaml") || name.endsWith(".yml")) {
            return OutputFormat.YAML;
        }
        return OutputFormat.PNG;
    }

    public Path resolvedOutput(Path project, OutputFormat outFormat) {
        if (output != null) {
            return output.toAbsolutePath().normalize();
        }
        String ext = switch (outFormat) {
        case HTML -> "html";
        case JSON -> "json";
        case PNG -> "png";
        case YAML -> "yaml";
        };
        String stem = "treemap-" + projectNameForOutput(project);
        return project.resolve("target").resolve(stem + "." + ext).toAbsolutePath().normalize();
    }

    @Override
    public Integer call() throws Exception {
        long runStartNs = System.nanoTime();
        Path project = projectDir.toAbsolutePath().normalize();
        Path pom = project.resolve("pom.xml");
        if (!Files.isRegularFile(pom)) {
            System.err.println("No pom.xml in " + project);
            return 2;
        }

        log.info("Project directory: {}", project);
        MavenInvoker maven = new MavenInvoker(project);

        String pomDigest = PomChecksum.sha256Hex(pom);
        Path cacheRoot = cacheDir != null ? cacheDir.toAbsolutePath().normalize()
                : DependencyTreeCache.defaultDirectory();
        DependencyTreeCache treeCache = new DependencyTreeCache(cacheRoot);

        List<String> lines;
        boolean treeCacheHit = false;
        long depTreeStartNs = System.nanoTime();
        if (refresh) {
            log.info("Refreshing dependency tree (--refresh); running mvn dependency:tree...");
            lines = maven.dependencyTree();
            treeCache.save(pomDigest, lines);
        } else {
            Optional<List<String>> cached = treeCache.load(pomDigest);
            if (cached.isPresent()) {
                lines = cached.get();
                treeCacheHit = true;
                log.info("Using cached dependency tree for pom.xml (sha256 {}...)", pomDigest.substring(0, 12));
            } else {
                log.info("Running mvn dependency:tree...");
                lines = maven.dependencyTree();
                treeCache.save(pomDigest, lines);
            }
        }
        long depTreeElapsedMs = (System.nanoTime() - depTreeStartNs) / NANOS_PER_MILLISECOND;
        log.info("Dependency tree time: {} ms (cache: {})", depTreeElapsedMs, treeCacheHit ? "hit" : "miss");

        Path localRepo;
        long localRepoStartNs = System.nanoTime();
        try {
            localRepo = maven.resolveLocalRepository();
        } catch (Exception e) {
            log.warn("Could not resolve settings.localRepository ({}), using ~/.m2/repository", e.toString());
            localRepo = LocalArtifactResolver.defaultLocalRepository();
        }
        long localRepoElapsedMs = (System.nanoTime() - localRepoStartNs) / NANOS_PER_MILLISECOND;
        log.info("Local repository: {}", localRepo);
        log.info("Local repository resolve time: {} ms", localRepoElapsedMs);

        long analyzeStartNs = System.nanoTime();
        DependencyNode root = DependencyTreeParser.parse(lines);
        int pruned = DependencyScopePruner.pruneNonPackagedScopes(root);
        if (pruned > 0) {
            log.info("Pruned {} non-packaged dependency node(s) (test/provided scopes)", pruned);
        }
        LocalArtifactResolver resolver = new LocalArtifactResolver(localRepo);
        AnalysisMode resolvedMode = resolveAnalysisMode(project);
        if (resolvedMode == AnalysisMode.PACKAGED) {
            Path jar = resolvePackagedJar(project);
            PackagedJarAnalyzer.Result result = new PackagedJarAnalyzer(resolver).applyPackagedSizes(root, jar);
            log.info("Analysis mode: PACKAGED ({})", jar);
            log.info("Packaged attribution: attributed={} B, unknown={} B, overlaps={}",
                    result.attributedBytes(),
                    result.unknownBytes(),
                    result.overlapEntryCount());
        } else {
            resolver.applySizes(root);
            log.info("Analysis mode: DEPENDENCY (local artifact sizes)");
        }
        long analyzeElapsedMs = (System.nanoTime() - analyzeStartNs) / NANOS_PER_MILLISECOND;
        log.info("Tree parse + artifact sizing time: {} ms", analyzeElapsedMs);

        String title = "Dependencies: " + project.getFileName() + " (" + root.key().artifactId() + ")";
        OutputFormat outFormat = resolvedFormat();
        Path out = resolvedOutput(project, outFormat);
        log.info("Output format: {}", outFormat);
        Generator generator = Generators.forOutputFormat(outFormat);
        long renderStartNs = System.nanoTime();
        generator.generate(root, o -> o.output(out)
                .width(width)
                .height(height)
                .title(title));
        long renderElapsedMs = (System.nanoTime() - renderStartNs) / NANOS_PER_MILLISECOND;
        log.info("Render time ({}): {} ms", outFormat, renderElapsedMs);
        long totalElapsedMs = (System.nanoTime() - runStartNs) / NANOS_PER_MILLISECOND;
        log.info("Total generation time: {} ms", totalElapsedMs);
        log.info("Wrote {}", out);
        System.out.println(out);
        return 0;
    }

    private AnalysisMode resolveAnalysisMode(Path project) throws IOException {
        if (analysisMode == AnalysisMode.DEPENDENCY || analysisMode == AnalysisMode.PACKAGED) {
            return analysisMode;
        }
        if (packagedJar != null && Files.isRegularFile(packagedJar.toAbsolutePath().normalize())) {
            return AnalysisMode.PACKAGED;
        }
        return PackagedJarAnalyzer.detectDefaultPackagedJar(project).isPresent()
                ? AnalysisMode.PACKAGED
                : AnalysisMode.DEPENDENCY;
    }

    private Path resolvePackagedJar(Path project) throws IOException {
        if (packagedJar != null) {
            return packagedJar.toAbsolutePath().normalize();
        }
        return PackagedJarAnalyzer.detectDefaultPackagedJar(project)
                .orElseThrow(() -> new IOException(
                        "No packaged JAR found in target/. Provide --packaged-jar or use --analysis-mode dependency."));
    }

    private static String projectNameForOutput(Path project) {
        Path fileName = project.getFileName();
        String raw = fileName == null ? "project" : fileName.toString();
        String normalized = raw.replaceAll("[^A-Za-z0-9._-]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-|-$", "");
        return normalized.isEmpty() ? "project" : normalized;
    }
}
