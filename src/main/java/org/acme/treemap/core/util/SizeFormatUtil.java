package org.acme.treemap.core.util;

import java.util.Locale;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Generic byte-size formatting helpers. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SizeFormatUtil {

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
