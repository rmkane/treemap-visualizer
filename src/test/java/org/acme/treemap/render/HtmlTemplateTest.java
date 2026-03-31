package org.acme.treemap.render;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.acme.treemap.maven.ArtifactKey;
import org.acme.treemap.maven.DependencyNode;

class HtmlTemplateTest {

    @Test
    void chartTemplateOnClasspath() {
        assertNotNull(
                HtmlRenderer.class.getResourceAsStream(HtmlRenderer.CHART_TEMPLATE_RESOURCE));
    }

    @Test
    void renderSubstitutesPlaceholders(@TempDir Path dir) throws IOException {
        ArtifactKey rootKey = new ArtifactKey("g", "a", "jar", "1", "compile", Optional.empty());
        DependencyNode root = new DependencyNode(rootKey);
        root.setSelfSizeBytes(1);

        Path out = dir.resolve("out.html");
        new HtmlRenderer()
                .generate(root, o -> o
                        .output(out)
                        .width(400)
                        .height(300)
                        .title("Test & Co <proj>"));

        String html = Files.readString(out);
        assertTrue(html.contains("Test &amp; Co &lt;proj&gt;"), "title should be XML-escaped");
        assertTrue(html.contains("width=\"400\"") && html.contains("height=\"300\""));
        assertTrue(html.contains("<rect class=\"cell\""));
        assertTrue(!html.contains("@@"), "no leftover template placeholders");
    }
}
