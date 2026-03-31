package org.acme.treemap.render;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.acme.treemap.OutputFormat;
import org.junit.jupiter.api.Test;

class GeneratorsTest {

    @Test
    void pngMapsToPngRenderer() {
        Generator g = Generators.forOutputFormat(OutputFormat.PNG);
        assertInstanceOf(PngRenderer.class, g);
    }

    @Test
    void htmlMapsToHtmlRenderer() {
        Generator g = Generators.forOutputFormat(OutputFormat.HTML);
        assertInstanceOf(HtmlRenderer.class, g);
    }

    @Test
    void jsonMapsToJsonExporter() {
        Generator g = Generators.forOutputFormat(OutputFormat.JSON);
        assertInstanceOf(JsonExporter.class, g);
    }

    @Test
    void yamlMapsToYamlExporter() {
        Generator g = Generators.forOutputFormat(OutputFormat.YAML);
        assertInstanceOf(YamlExporter.class, g);
    }
}
