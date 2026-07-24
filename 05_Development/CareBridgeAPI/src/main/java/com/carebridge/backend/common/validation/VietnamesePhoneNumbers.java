package com.carebridge.backend.common.validation;

import java.util.regex.Pattern;

/** Normalizes Vietnamese mobile numbers to the canonical E.164 representation. */
public final class VietnamesePhoneNumbers {

    public static final String INVALID_FORMAT_MESSAGE =
            "Invalid phone format. Use a Vietnamese mobile number (+84xxxxxxxxx)";

    private static final Pattern ALLOWED_INPUT = Pattern.compile("[+0-9\\s().-]+");
    private static final Pattern VIETNAMESE_MOBILE_E164 =
            Pattern.compile("\\+84[35789][0-9]{8}");

    private VietnamesePhoneNumbers() {}

    /**
     * Returns {@code null} for a missing value, otherwise returns a validated E.164 number.
     * Local {@code 0...}, country-code {@code 84...}, and canonical {@code +84...} forms are
     * accepted; formatting separators are removed.
     *
     * @throws IllegalArgumentException when the supplied value is not a Vietnamese mobile number
     */
    public static String normalizeToE164(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (!ALLOWED_INPUT.matcher(value).matches()) {
            throw new IllegalArgumentException(INVALID_FORMAT_MESSAGE);
        }

        String compact = value.replaceAll("[\\s().-]", "");
        String normalized;
        if (compact.startsWith("+84")) {
            normalized = compact;
        } else if (compact.startsWith("84")) {
            normalized = "+" + compact;
        } else if (compact.startsWith("0")) {
            normalized = "+84" + compact.substring(1);
        } else {
            throw new IllegalArgumentException(INVALID_FORMAT_MESSAGE);
        }

        if (!VIETNAMESE_MOBILE_E164.matcher(normalized).matches()) {
            throw new IllegalArgumentException(INVALID_FORMAT_MESSAGE);
        }
        return normalized;
    }

    public static boolean isValidOrBlank(String value) {
        try {
            normalizeToE164(value);
            return true;
        } catch (IllegalArgumentException invalidPhone) {
            return false;
        }
    }
}
