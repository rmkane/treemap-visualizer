package org.acme.treemap;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
