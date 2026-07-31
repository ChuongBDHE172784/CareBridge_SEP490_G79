package com.carebridge.backend.reminder.notification.service;

import com.carebridge.backend.common.exception.BusinessException;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class AppointmentNotificationRuleValidator {

    public static final List<Integer> SYSTEM_DEFAULTS = List.of(-1440, -30, 0, 15);
    public static final String DEFAULT_TIME_ZONE = "Asia/Ho_Chi_Minh";
    public static final int MIN_OFFSET_MINUTES = -43200;
    public static final int MAX_OFFSET_MINUTES = 10080;
    public static final int MAX_RULES = 10;

    public List<Integer> normalize(List<Integer> offsets) {
        if (offsets == null) {
            return new ArrayList<>(SYSTEM_DEFAULTS);
        }
        TreeSet<Integer> normalized = new TreeSet<>();
        for (Integer offset : offsets) {
            if (offset == null) {
                throw invalid("Notification offset must not be null");
            }
            if (offset < MIN_OFFSET_MINUTES || offset > MAX_OFFSET_MINUTES) {
                throw invalid("Notification offset must be between -43200 and 10080 minutes");
            }
            normalized.add(offset);
        }
        if (normalized.size() > MAX_RULES) {
            throw invalid("Appointment notifications support at most 10 unique times");
        }
        return List.copyOf(normalized);
    }

    public String normalizeTimeZone(String timeZone) {
        String candidate = timeZone == null || timeZone.isBlank() ? DEFAULT_TIME_ZONE : timeZone.trim();
        try {
            return ZoneId.of(candidate).getId();
        } catch (DateTimeException exception) {
            throw invalid("Invalid appointment time zone");
        }
    }

    private BusinessException invalid(String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, "REM-017", message);
    }
}
