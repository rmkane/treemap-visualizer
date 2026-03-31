package org.acme.treemap.maven;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** SHA-256 fingerprint of a POM file (for cache invalidation). */
public final class PomChecksum {

    private PomChecksum() {}

    public static String sha256Hex(Path pom) throws IOException {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(pom));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
