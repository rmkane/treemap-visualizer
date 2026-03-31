package org.acme.treemap;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.acme.treemap.maven.DependencyNode;
import org.acme.treemap.maven.DependencyTreeCache;
import org.acme.treemap.maven.DependencyTreeParser;
import org.acme.treemap.maven.LocalArtifactResolver;
import org.acme.treemap.maven.MavenInvoker;
import org.acme.treemap.maven.PomChecksum;
import org.acme.treemap.render.Generator;
import org.acme.treemap.render.Generators;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "treemap-visualize", description = "Treemap of Maven dependency sizes: PNG, HTML, JSON, or YAML (local ~/.m2 for sizes).", mixinStandardHelpOptions = true)
public final class Main implements Callable<Integer> {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    private static final long NANOS_PER_MILLISECOND = 1_000_000L;

    @Parameters(index = "0", arity = "0..1", defaultValue = ".", description = "Maven project directory (must contain pom.xml)")
    private Path projectDir;

    @Option(names = { "-o", "--output" }, description = "Output path (optional). Default: target/<artifactId>.<ext>")
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

    public static void main(String[] args) {
        int exit = new CommandLine(new Main()).execute(args);
        System.exit(exit);
    }

    static final class OutputFormatConverter implements CommandLine.ITypeConverter<OutputFormat> {
        @Override
        public OutputFormat convert(String value) {
            return OutputFormat.valueOf(value.toUpperCase(Locale.ROOT));
        }
    }

    OutputFormat resolvedFormat() {
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

    Path resolvedOutput(Path project, String artifactId, OutputFormat outFormat) {
        if (output != null) {
            return output.toAbsolutePath().normalize();
        }
        String ext = switch (outFormat) {
        case HTML -> "html";
        case JSON -> "json";
        case PNG -> "png";
        case YAML -> "yaml";
        };
        return project.resolve("target").resolve(artifactId + "." + ext).toAbsolutePath().normalize();
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
            log.info("Refreshing dependency tree (--refresh); running mvn dependency:tree…");
            lines = maven.dependencyTree();
            treeCache.save(pomDigest, lines);
        } else {
            Optional<List<String>> cached = treeCache.load(pomDigest);
            if (cached.isPresent()) {
                lines = cached.get();
                treeCacheHit = true;
                log.info("Using cached dependency tree for pom.xml (sha256 {}…)", pomDigest.substring(0, 12));
            } else {
                log.info("Running mvn dependency:tree…");
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
        new LocalArtifactResolver(localRepo).applySizes(root);
        long analyzeElapsedMs = (System.nanoTime() - analyzeStartNs) / NANOS_PER_MILLISECOND;
        log.info("Tree parse + artifact sizing time: {} ms", analyzeElapsedMs);

        String title = "Dependencies: " + project.getFileName() + " (" + root.key().artifactId() + ")";
        OutputFormat outFormat = resolvedFormat();
        Path out = resolvedOutput(project, root.key().artifactId(), outFormat);
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
}
