package com.carebridge.backend.content.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Locale;

public enum ContentStage {
    PRE_PREGNANCY,
    PREGNANCY,
    POSTPARTUM;

    @JsonCreator
    public static ContentStage fromApiValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if ("BABY_CARE".equalsIgnoreCase(value)) {
            return POSTPARTUM;
        }
        return valueOf(value.toUpperCase(Locale.ROOT));
    }
}
