package org.acme.treemap.render;

import java.util.ArrayList;
import java.util.List;

/**
 * Nested strip treemap (alternating slice-and-dice): children split the parent rectangle in proportion
 * to their weights. Deterministic and fills the area without overlap.
 */
public final class TreemapLayout {

    private TreemapLayout() {}

    /**
     * Partitions {@code (x,y,w,h)} into one rectangle per weight. Splits along the horizontal axis when
     * {@code rowIsHorizontal} is true (vertical cuts), else vertical axis (horizontal cuts).
     */
    public static List<Rect> layoutStrip(
            double x, double y, double w, double h, boolean rowIsHorizontal, List<Double> weights) {
        List<Rect> out = new ArrayList<>(weights.size());
        double sum = weights.stream().mapToDouble(Double::doubleValue).sum();
        if (sum <= 0 || w <= 0 || h <= 0) {
            for (int i = 0; i < weights.size(); i++) {
                out.add(new Rect(x, y, 0, 0));
            }
            return out;
        }
        if (rowIsHorizontal) {
            double cx = x;
            for (double wt : weights) {
                double cw = w * (wt / sum);
                out.add(new Rect(cx, y, cw, h));
                cx += cw;
            }
        } else {
            double cy = y;
            for (double wt : weights) {
                double ch = h * (wt / sum);
                out.add(new Rect(x, cy, w, ch));
                cy += ch;
            }
        }
        return out;
    }

    public record Rect(double x, double y, double w, double h) {}
}
