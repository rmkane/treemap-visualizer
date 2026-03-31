package org.acme.treemap;

import org.acme.treemap.maven.DependencyNode;
import org.acme.treemap.maven.DependencyTreeParser;
import org.acme.treemap.maven.LocalArtifactResolver;
import org.acme.treemap.maven.MavenInvoker;
import org.acme.treemap.render.TreemapPngRenderer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(
        name = "treemap-visualize",
        description = "Draw a PNG treemap of Maven dependency sizes using the local ~/.m2 repository.",
        mixinStandardHelpOptions = true)
public final class Main implements Callable<Integer> {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    @Parameters(
            index = "0",
            arity = "0..1",
            defaultValue = ".",
            description = "Maven project directory (must contain pom.xml)")
    private Path projectDir;

    @Option(
            names = {"-o", "--output"},
            defaultValue = "dependency-treemap.png",
            description = "Output PNG path")
    private Path output;

    @Option(names = "--width", defaultValue = "1200", description = "Image width in pixels")
    private int width;

    @Option(names = "--height", defaultValue = "800", description = "Image height in pixels")
    private int height;

    public static void main(String[] args) {
        int exit = new CommandLine(new Main()).execute(args);
        System.exit(exit);
    }

    @Override
    public Integer call() throws Exception {
        Path project = projectDir.toAbsolutePath().normalize();
        Path pom = project.resolve("pom.xml");
        if (!Files.isRegularFile(pom)) {
            System.err.println("No pom.xml in " + project);
            return 2;
        }

        log.info("Project directory: {}", project);
        MavenInvoker maven = new MavenInvoker(project);
        log.info("Running mvn dependency:tree...");
        List<String> lines = maven.dependencyTree();

        Path localRepo;
        try {
            localRepo = maven.resolveLocalRepository();
        } catch (Exception e) {
            log.warn("Could not resolve settings.localRepository ({}), using ~/.m2/repository", e.toString());
            localRepo = LocalArtifactResolver.defaultLocalRepository();
        }
        log.info("Local repository: {}", localRepo);

        DependencyNode root = DependencyTreeParser.parse(lines);
        new LocalArtifactResolver(localRepo).applySizes(root);

        String title = "Dependencies: " + project.getFileName() + " (" + root.key().artifactId() + ")";
        Path out = output.toAbsolutePath().normalize();
        TreemapPngRenderer.render(root, out, width, height, title);
        log.info("Wrote {}", out);
        System.out.println(out);
        return 0;
    }
}
