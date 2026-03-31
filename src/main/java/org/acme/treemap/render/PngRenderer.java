package org.acme.treemap.render;

import org.acme.treemap.maven.DependencyNode;
import org.acme.treemap.util.ColorUtil;
import org.acme.treemap.util.RgbColor;
import org.acme.treemap.util.SizeFormatUtil;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.imageio.ImageIO;

/** Generates a dependency tree as a nested treemap PNG. */
public final class PngRenderer implements Generator {

    private static final int PADDING = 24;
    private static final int MIN_LABEL_PX = 48;

    public PngRenderer() {}

    @Override
    public void generate(DependencyNode root, OutputOptions options) throws IOException {
        int width = options.width();
        int height = options.height();
        String title = options.title();
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new Color(0x1a, 0x1d, 0x23));
        g.fillRect(0, 0, width, height);

        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        g.drawString(title, PADDING, PADDING);

        double plotX = PADDING;
        double plotY = PADDING + 28;
        double plotW = width - 2.0 * PADDING;
        double plotH = height - plotY - PADDING;
        if (plotW > 0 && plotH > 0) {
            drawNode(g, root, plotX, plotY, plotW, plotH, 0);
        }
        g.dispose();
        ImageIO.write(img, "png", options.output().toFile());
    }

    private static void drawNode(Graphics2D g, DependencyNode node, double x, double y, double w, double h, int depth) {
        List<DependencyNode> kids = new ArrayList<>(node.children());
        if (kids.isEmpty()) {
            fillCell(g, node, x, y, w, h, depth);
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
            drawNode(g, kids.get(i), r.x(), r.y(), r.w(), r.h(), depth + 1);
        }
    }

    private static void fillCell(Graphics2D g, DependencyNode node, double x, double y, double w, double h, int depth) {
        int ix = (int) Math.floor(x);
        int iy = (int) Math.floor(y);
        int iw = Math.max(1, (int) Math.ceil(x + w) - ix);
        int ih = Math.max(1, (int) Math.ceil(y + h) - iy);

        RgbColor fillRgb = ColorUtil.fillFromKey(node.key().shortLabel(), depth);
        Color fill = fillRgb.toAwtColor();
        g.setColor(fill);
        g.fillRect(ix, iy, iw, ih);
        g.setColor(new Color(0, 0, 0, 60));
        g.setStroke(new BasicStroke(1f));
        g.drawRect(ix, iy, iw, ih);

        if (Math.min(iw, ih) >= MIN_LABEL_PX) {
            g.setColor(ColorUtil.contrastText(fillRgb).toAwtColor());
            String label = node.key().artifactId();
            String sub = SizeFormatUtil.humanBytes(node.selfSizeBytes());
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, Math.min(14, iw / 12)));
            FontMetrics fm = g.getFontMetrics();
            drawClipped(g, label, ix + 4, iy + fm.getAscent() + 4, iw - 8);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, Math.min(11, iw / 14)));
            fm = g.getFontMetrics();
            drawClipped(g, sub, ix + 4, iy + fm.getAscent() + 20, iw - 8);
        }
    }

    private static void drawClipped(Graphics2D g, String text, int tx, int ty, int maxW) {
        FontMetrics fm = g.getFontMetrics();
        String t = text;
        if (fm.stringWidth(t) <= maxW) {
            g.drawString(t, tx, ty);
            return;
        }
        while (t.length() > 3 && fm.stringWidth(t + "…") > maxW) {
            t = t.substring(0, t.length() - 1);
        }
        g.drawString(t + "…", tx, ty);
    }
}
