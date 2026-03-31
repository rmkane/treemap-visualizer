package org.acme.treemap.core.maven;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** SHA-256 fingerprint of a POM file (for cache invalidation). */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PomChecksum {

    public static String sha256Hex(Path pom) throws IOException {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(pom));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
