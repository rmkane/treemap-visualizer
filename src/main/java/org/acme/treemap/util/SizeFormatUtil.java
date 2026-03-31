package org.acme.treemap.util;

import java.util.Locale;

/** Generic byte-size formatting helpers. */
public final class SizeFormatUtil {

    private SizeFormatUtil() {}

    public static String humanBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        double kb = bytes / 1024.0;
        if (kb < 1024) {
            return String.format(Locale.ROOT, "%.1f KB", kb);
        }
        return String.format(Locale.ROOT, "%.2f MB", kb / 1024.0);
    }
}
