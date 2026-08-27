package com.carebridge.backend.directchat.mapper;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * TDS §9.2: cursor = base64("sortTsIsoInstant|kind|resourceId"). Pure, side-effect-free —
 * kept separate from repository/service so it can be unit-tested without a database.
 */
public final class TimelineCursorCodec {

    private static final String SEPARATOR = "\\|";

    private TimelineCursorCodec() {
    }

    public static String encode(Instant sortTs, String kind, UUID resourceId) {
        validateKind(kind);
        String raw = sortTs + "|" + kind + "|" + resourceId;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static DecodedCursor decode(String cursor) {
        String raw;
        try {
            raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Malformed cursor", ex);
        }
        String[] parts = raw.split(SEPARATOR, 3);
        if (parts.length != 3) {
            throw new IllegalArgumentException("Malformed cursor");
        }
        try {
            Instant sortTs = parseInstant(parts[0]);
            String kind = parts[1];
            validateKind(kind);
            UUID resourceId = UUID.fromString(parts[2]);
            return new DecodedCursor(sortTs, kind, resourceId);
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Malformed cursor", ex);
        }
    }

    private static Instant parseInstant(String value) {
        // Accept legacy millisecond cursors produced by the initial implementation while
        // preserving full PostgreSQL timestamp precision for all newly issued cursors.
        if (value.indexOf('T') < 0) {
            return Instant.ofEpochMilli(Long.parseLong(value));
        }
        return Instant.parse(value);
    }

    private static void validateKind(String kind) {
        if (!"MESSAGE".equals(kind) && !"CALL_EVENT".equals(kind)) {
            throw new IllegalArgumentException("Malformed cursor");
        }
    }

    public record DecodedCursor(Instant sortTs, String kind, UUID resourceId) {
    }
}
