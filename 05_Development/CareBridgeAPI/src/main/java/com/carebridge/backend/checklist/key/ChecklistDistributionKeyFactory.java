package com.carebridge.backend.checklist.key;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/** Creates versioned deterministic keys for checklist parents and children. */
public final class ChecklistDistributionKeyFactory {

    private static final String KEY_VERSION = "v1";
    private static final String ABSENT = "<ABSENT>";
    private static final String NEUTRAL_WINDOW = "NONE";

    private ChecklistDistributionKeyFactory() {
    }

    public static String instanceKey(
            UUID templateVersionId,
            UUID recipientUserId,
            String recipientRole,
            UUID careGroupId,
            String careContextType,
            UUID careContextId,
            String windowStart,
            String windowEnd) {
        validateWindowBounds(windowStart, windowEnd);
        return hash(
                uuidOrAbsent(templateVersionId),
                uuid(recipientUserId),
                enumToken(recipientRole),
                uuidOrAbsent(careGroupId),
                enumToken(careContextType),
                uuid(careContextId),
                windowToken(windowStart),
                windowToken(windowEnd));
    }

    public static String userCreatedInstanceKey(
            UUID recipientUserId,
            String recipientRole,
            UUID careGroupId,
            String careContextType,
            UUID careContextId,
            LocalDate windowStart,
            LocalDate windowEnd) {
        validateWindowBounds(windowStart, windowEnd);
        return hash(
                ABSENT,
                uuid(recipientUserId),
                enumToken(recipientRole),
                uuidOrAbsent(careGroupId),
                enumToken(careContextType),
                uuid(careContextId),
                dateOrAbsent(windowStart),
                dateOrAbsent(windowEnd));
    }

    public static String childKey(UUID checklistInstanceId, UUID templateItemVersionId) {
        return hash(uuid(checklistInstanceId), uuid(templateItemVersionId));
    }

    /** Stable lock scope shared by distribution, lifecycle cancellation and task actions. */
    public static String lifecycleScopeKey(
            UUID templateVersionId,
            UUID recipientUserId,
            String recipientRole,
            UUID careGroupId,
            String careContextType,
            UUID careContextId) {
        return hash(
                "LIFECYCLE_SCOPE",
                uuidOrAbsent(templateVersionId),
                uuid(recipientUserId),
                enumToken(recipientRole),
                uuidOrAbsent(careGroupId),
                enumToken(careContextType),
                uuid(careContextId));
    }

    public static String userCreatedChildKey(UUID checklistInstanceId, UUID clientTaskId) {
        return hash(uuid(checklistInstanceId), "USER_CREATED", uuid(clientTaskId));
    }

    private static String hash(String... values) {
        StringBuilder canonical = new StringBuilder(KEY_VERSION);
        for (String value : values) {
            byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
            canonical.append(bytes.length).append(':').append(value);
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the Java platform", exception);
        }
    }

    private static String uuid(UUID value) {
        return Objects.requireNonNull(value, "UUID key token is required")
                .toString()
                .toLowerCase(Locale.ROOT);
    }

    private static String uuidOrAbsent(UUID value) {
        return value == null ? ABSENT : uuid(value);
    }

    private static String enumToken(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Enum key token is required");
        }
        return Normalizer.normalize(value.trim().toUpperCase(Locale.ROOT), Normalizer.Form.NFC);
    }

    private static String windowToken(String value) {
        if (value == null || value.isBlank()) {
            return ABSENT;
        }
        String canonical = Normalizer.normalize(value.trim(), Normalizer.Form.NFC);
        if (NEUTRAL_WINDOW.equals(canonical)) {
            return NEUTRAL_WINDOW;
        }
        try {
            return LocalDate.parse(canonical).toString();
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Window key token must be NONE or an ISO-8601 local date", exception);
        }
    }

    private static String dateOrAbsent(LocalDate value) {
        return value == null ? ABSENT : value.toString();
    }

    private static void validateWindowBounds(String start, String end) {
        boolean startAbsent = start == null || start.isBlank();
        boolean endAbsent = end == null || end.isBlank();
        if (startAbsent != endAbsent) {
            throw new IllegalArgumentException("Window bounds must both be present or absent");
        }
        if (startAbsent) {
            return;
        }
        boolean startNeutral = NEUTRAL_WINDOW.equals(start.trim());
        boolean endNeutral = NEUTRAL_WINDOW.equals(end.trim());
        if (startNeutral != endNeutral) {
            throw new IllegalArgumentException("Neutral windows require NONE for both bounds");
        }
        if (!startNeutral) {
            LocalDate startDate = parseWindowDate(start);
            LocalDate endDate = parseWindowDate(end);
            if (endDate.isBefore(startDate)) {
                throw new IllegalArgumentException("Window end must not precede window start");
            }
        }
    }

    private static void validateWindowBounds(LocalDate start, LocalDate end) {
        if ((start == null) != (end == null)) {
            throw new IllegalArgumentException("Window bounds must both be present or absent");
        }
        if (start != null && end.isBefore(start)) {
            throw new IllegalArgumentException("Window end must not precede window start");
        }
    }

    private static LocalDate parseWindowDate(String value) {
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Window key token must be NONE or an ISO-8601 local date", exception);
        }
    }
}
