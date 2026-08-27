package com.carebridge.backend.aimoderation.policy;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Content-version fingerprint used for scan idempotency. Line endings are normalized and the
 * text trimmed so cosmetic transport differences don't force redundant rescans, but content
 * is otherwise hashed as-is (case matters — it is part of content identity).
 */
public final class AiContentHasher {

    private AiContentHasher() {
    }

    public static String sha256Hex(String text) {
        String normalized = normalize(text);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(normalized.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    static String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\r\n", "\n").replace('\r', '\n').strip();
    }
}
