package org.acme.treemap.render;

import org.acme.treemap.maven.DependencyNode;
import org.acme.treemap.util.ColorUtil;
import org.acme.treemap.util.RgbColor;
import org.acme.treemap.util.SizeFormatUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Renders an interactive HTML page with SVG treemap, hover highlight, and tooltips. */
public final class HtmlRenderer implements Renderer {

    /** Classpath resource path (alongside this class): {@code src/main/resources/org/acme/treemap/render/treemap-chart.html}. */
    public static final String CHART_TEMPLATE_RESOURCE = "treemap-chart.html";

    private static final int PADDING = 24;
    private static final int MIN_LABEL_PX = 48;
    /** Inset per edge so neighboring cells leave a gap (stroke/hover read more clearly). */
    private static final int CELL_GUTTER_PX = 1;

    /** Cached classpath template from {@code treemap-chart.html} beside this class in resources. */
    private static volatile String templateCache;

    public HtmlRenderer() {}

    @Override
    public void render(DependencyNode root, OutputOptions options) throws IOException {
        int width = options.width();
        int height = options.height();
        String title = options.title();
        StringBuilder svg = new StringBuilder(16_384);
        double plotX = PADDING;
        double plotY = PADDING + 28;
        double plotW = width - 2.0 * PADDING;
        double plotH = height - plotY - PADDING;
        if (plotW > 0 && plotH > 0) {
            appendNodeSvg(svg, root, plotX, plotY, plotW, plotH, 0);
        }

        String safeTitle = xmlText(title);
        String w = Integer.toString(width);
        String h = Integer.toString(height);
        String html = chartPageTemplate()
                .replace("@@TITLE_XML@@", safeTitle)
                .replace("@@CANVAS_WIDTH@@", w)
                .replace("@@CANVAS_HEIGHT@@", h)
                .replace("@@SVG_BODY@@", svg.toString());

        Files.writeString(options.output(), html, StandardCharsets.UTF_8);
    }

    private static String chartPageTemplate() throws IOException {
        String cached = templateCache;
        if (cached != null) {
            return cached;
        }
        synchronized (HtmlRenderer.class) {
            cached = templateCache;
            if (cached != null) {
                return cached;
            }
            try (InputStream in = HtmlRenderer.class.getResourceAsStream(CHART_TEMPLATE_RESOURCE)) {
                if (in == null) {
                    throw new IOException(
                            "Missing classpath resource org/acme/treemap/render/" + CHART_TEMPLATE_RESOURCE);
                }
                cached = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            templateCache = cached;
            return cached;
        }
    }

    private static void appendNodeSvg(
            StringBuilder svg, DependencyNode node, double x, double y, double w, double h, int depth) {
        List<DependencyNode> kids = new ArrayList<>(node.children());
        if (kids.isEmpty()) {
            appendLeaf(svg, node, x, y, w, h, depth);
            return;
        }

        kids.sort(Comparator.comparingLong(DependencyNode::subtreeUniqueBytes).reversed());
        List<Double> weights = new ArrayList<>(kids.size());
        for (DependencyNode k : kids) {
            weights.add(Math.max(1.0, (double) k.subtreeUniqueBytes()));
        }

        boolean horizontalStrip = (depth % 2) == 0;
        List<TreemapLayout.Rect> rects = TreemapLayout.layoutStrip(x, y, w, h, horizontalStrip, weights);
        for (int i = 0; i < kids.size(); i++) {
            TreemapLayout.Rect r = rects.get(i);
            appendNodeSvg(svg, kids.get(i), r.x(), r.y(), r.w(), r.h(), depth + 1);
        }
    }

    private static void appendLeaf(StringBuilder svg, DependencyNode node, double x, double y, double w, double h, int depth) {
        int ix = (int) Math.floor(x);
        int iy = (int) Math.floor(y);
        int iw = Math.max(1, (int) Math.ceil(x + w) - ix);
        int ih = Math.max(1, (int) Math.ceil(y + h) - iy);

        int g = CELL_GUTTER_PX;
        ix += g;
        iy += g;
        iw = Math.max(1, iw - 2 * g);
        ih = Math.max(1, ih - 2 * g);

        RgbColor fillColor = ColorUtil.fillFromKey(node.key().shortLabel(), depth);
        String fill = fillColor.toCssRgb();
        String coord = node.key().groupId()
                + ":"
                + node.key().artifactId()
                + ":"
                + node.key().packaging()
                + ":"
                + node.key().version()
                + ":"
                + node.key().scope();
        String tipPayload = buildTipHtml(node, coord);

        svg.append(String.format(
                Locale.ROOT,
                "<rect class=\"cell\" x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" fill=\"%s\" data-html-tip=\"%s\">",
                ix,
                iy,
                iw,
                ih,
                fill,
                escapeAttr(tipPayload)));
        svg.append("<title>")
                .append(xmlText(coord))
                .append('\n')
                .append(xmlText("JAR on disk: " + SizeFormatUtil.humanBytes(node.selfSizeBytes())))
                .append('\n')
                .append(xmlText("Subtree (unique): " + SizeFormatUtil.humanBytes(node.subtreeUniqueBytes())))
                .append("</title></rect>\n");

        if (Math.min(iw, ih) >= MIN_LABEL_PX) {
            String textColor = ColorUtil.contrastText(fillColor).toCssRgb();
            String artifact = xmlText(node.key().artifactId());
            String sizeLabel = xmlText(SizeFormatUtil.humanBytes(node.selfSizeBytes()));
            int fs = Math.max(9, Math.min(14, iw / 12));
            int fs2 = Math.max(8, Math.min(11, iw / 14));
            svg.append(String.format(
                    Locale.ROOT,
                    "<text x=\"%d\" y=\"%d\" fill=\"%s\" font-size=\"%d\" font-family=\"system-ui,sans-serif\">%s</text>\n",
                    ix + 4,
                    iy + 4 + fs,
                    textColor,
                    fs,
                    artifact));
            svg.append(String.format(
                    Locale.ROOT,
                    "<text x=\"%d\" y=\"%d\" fill=\"%s\" font-size=\"%d\" font-family=\"system-ui,sans-serif\" opacity=\"0.9\">%s</text>\n",
                    ix + 4,
                    iy + 4 + fs + fs2 + 2,
                    textColor,
                    fs2,
                    sizeLabel));
        }
    }

    private static String buildTipHtml(DependencyNode node, String coord) {
        return "<div class=\"lbl\">"
                + escapeForHtmlFragment(node.key().artifactId())
                + "</div><div class=\"coord\">"
                + escapeForHtmlFragment(coord)
                + "</div><div>"
                + escapeForHtmlFragment("JAR on disk: " + SizeFormatUtil.humanBytes(node.selfSizeBytes()))
                + "</div><div>"
                + escapeForHtmlFragment("Subtree (unique): " + SizeFormatUtil.humanBytes(node.subtreeUniqueBytes()))
                + "</div>";
    }

    private static String escapeAttr(String s) {
        return s.replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\n", " ")
                .replace("\r", "");
    }

    private static String escapeForHtmlFragment(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static String xmlText(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
