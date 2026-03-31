package org.acme.treemap.util;

import java.awt.Color;

/** Small immutable RGB value object used by renderers. */
public record RgbColor(int red, int green, int blue) {

    public RgbColor {
        if (red < 0 || red > 255 || green < 0 || green > 255 || blue < 0 || blue > 255) {
            throw new IllegalArgumentException("RGB values must be in [0,255]");
        }
    }

    public Color toAwtColor() {
        return new Color(red, green, blue);
    }

    public String toCssRgb() {
        return "rgb(" + red + "," + green + "," + blue + ")";
    }
}
