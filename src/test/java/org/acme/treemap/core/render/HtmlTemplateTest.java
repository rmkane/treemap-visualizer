package org.acme.treemap.core.render;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.acme.treemap.core.maven.ArtifactKey;
import org.acme.treemap.core.maven.DependencyNode;

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
        DependencyNode child = new DependencyNode(
                new ArtifactKey("g.child", "b", "jar", "1", "compile", Optional.empty()));
        child.setSelfSizeBytes(2);
        root.children().add(child);

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
        assertTrue(html.contains("aria-label=\""));
        assertTrue(!html.contains("</title></rect>"));
        assertTrue(html.contains("Parent: g:a:jar:1:compile"));
        assertTrue(!html.contains("@@"), "no leftover template placeholders");
    }

    @Test
    void tinyCellsStillRenderTextLabel(@TempDir Path dir) throws IOException {
        ArtifactKey rootKey = new ArtifactKey("g", "artifact-with-long-name", "jar", "1", "compile", Optional.empty());
        DependencyNode root = new DependencyNode(rootKey);
        root.setSelfSizeBytes(1);

        Path out = dir.resolve("tiny.html");
        new HtmlRenderer()
                .generate(root, o -> o
                        .output(out)
                        .width(70)
                        .height(80)
                        .title("Tiny"));

        String html = Files.readString(out);
        assertTrue(html.contains("<text "), "small cells should still include a visible label");
    }
}
