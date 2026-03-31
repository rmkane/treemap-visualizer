package org.acme.treemap.render;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.acme.treemap.OutputFormat;
import org.junit.jupiter.api.Test;

class RenderersTest {

    @Test
    void pngMapsToPngRenderer() {
        Renderer r = Renderers.forOutputFormat(OutputFormat.PNG);
        assertInstanceOf(PngRenderer.class, r);
    }

    @Test
    void htmlMapsToHtmlRenderer() {
        Renderer r = Renderers.forOutputFormat(OutputFormat.HTML);
        assertInstanceOf(HtmlRenderer.class, r);
    }

    @Test
    void jsonMapsToJsonRenderer() {
        Renderer r = Renderers.forOutputFormat(OutputFormat.JSON);
        assertInstanceOf(JsonRenderer.class, r);
    }

    @Test
    void yamlMapsToYamlRenderer() {
        Renderer r = Renderers.forOutputFormat(OutputFormat.YAML);
        assertInstanceOf(YamlRenderer.class, r);
    }
}
