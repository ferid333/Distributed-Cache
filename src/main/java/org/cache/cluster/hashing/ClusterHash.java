package org.cache.cluster.hashing;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class ClusterHash {

    private static final String HASH_ALGORITHM = "SHA-256";

    private ClusterHash() {
    }

    public static long hashLong(String value) {
        return ByteBuffer.wrap(hash(value)).getLong() & Long.MAX_VALUE;
    }

    public static String hashHex(String value) {
        return HexFormat.of().formatHex(hash(value));
    }

    private static byte[] hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            return digest.digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(HASH_ALGORITHM + " hash algorithm is not available", exception);
        }
    }
}
