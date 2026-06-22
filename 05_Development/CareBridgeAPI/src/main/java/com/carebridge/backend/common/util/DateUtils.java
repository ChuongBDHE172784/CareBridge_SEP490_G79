package com.carebridge.backend.common.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class DateUtils {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter INSTANT_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    private DateUtils() {
    }

    public static String formatInstant(Instant instant) {
        return instant == null ? null : INSTANT_FORMATTER.format(instant);
    }

    public static String formatLocalDate(LocalDate localDate) {
        return localDate == null ? null : DATE_FORMATTER.format(localDate);
    }

    public static LocalDate parseLocalDate(String value) {
        return value == null || value.isBlank() ? null : LocalDate.parse(value, DATE_FORMATTER);
    }

    public static LocalDate toLocalDate(Instant instant) {
        return instant == null ? null : instant.atZone(DEFAULT_ZONE).toLocalDate();
    }
}
