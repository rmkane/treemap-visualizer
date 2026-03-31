package org.acme.treemap.util;

import java.awt.Color;

/** Generic color helpers independent of renderer/output format. */
public final class ColorUtil {

    private ColorUtil() {}

    public static RgbColor fillFromKey(String key, int depth) {
        int h = Math.floorMod(key.hashCode(), 360);
        float s = 0.35f + 0.08f * (depth % 3);
        float b = 0.82f - 0.06f * (depth % 2);
        Color c = Color.getHSBColor(h / 360f, s, b);
        return new RgbColor(c.getRed(), c.getGreen(), c.getBlue());
    }

    public static RgbColor contrastText(RgbColor bg) {
        double lum = (0.299 * bg.red() + 0.587 * bg.green() + 0.114 * bg.blue()) / 255.0;
        return lum > 0.6 ? new RgbColor(30, 30, 30) : new RgbColor(255, 255, 255);
    }
}
