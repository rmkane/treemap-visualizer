package org.acme.treemap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import org.acme.treemap.core.OutputFormat;

import picocli.CommandLine;

class MainFormatTest {

    @Test
    void infersHtmlFromOutputPath() {
        Main m = new Main();
        new CommandLine(m).parseArgs("-o", "report.html");
        assertEquals(OutputFormat.HTML, m.resolvedFormat());
    }

    @Test
    void infersHtmlFromHtm() {
        Main m = new Main();
        new CommandLine(m).parseArgs("-o", "report.htm");
        assertEquals(OutputFormat.HTML, m.resolvedFormat());
    }

    @Test
    void defaultsToPngForUnknownExtension() {
        Main m = new Main();
        new CommandLine(m).parseArgs("-o", "out.png");
        assertEquals(OutputFormat.PNG, m.resolvedFormat());
    }

    @Test
    void explicitFormatOverridesExtension() {
        Main m = new Main();
        new CommandLine(m).parseArgs("-o", "out.png", "--format", "html");
        assertEquals(OutputFormat.HTML, m.resolvedFormat());
    }

    @Test
    void infersJsonFromExtension() {
        Main m = new Main();
        new CommandLine(m).parseArgs("-o", "deps.json");
        assertEquals(OutputFormat.JSON, m.resolvedFormat());
    }

    @Test
    void infersYamlFromYamlExtension() {
        Main m = new Main();
        new CommandLine(m).parseArgs("-o", "deps.yaml");
        assertEquals(OutputFormat.YAML, m.resolvedFormat());
    }

    @Test
    void infersYamlFromYmlExtension() {
        Main m = new Main();
        new CommandLine(m).parseArgs("-o", "deps.yml");
        assertEquals(OutputFormat.YAML, m.resolvedFormat());
    }

    @Test
    void defaultOutputUsesProjectNamePrefix() {
        Main m = new Main();
        Path out = m.resolvedOutput(Path.of("/tmp/acme-api-mvc"), OutputFormat.HTML);
        assertEquals(Path.of("/tmp/acme-api-mvc/target/treemap-acme-api-mvc.html").toAbsolutePath().normalize(), out);
    }

    @Test
    void acceptsLowercaseAnalysisMode() {
        Main m = new Main();
        assertDoesNotThrow(() -> new CommandLine(m).parseArgs("--analysis-mode", "dependency"));
    }

    @Test
    void acceptsLowercaseViewMode() {
        Main m = new Main();
        assertDoesNotThrow(() -> new CommandLine(m).parseArgs("--view", "flat"));
    }
}
