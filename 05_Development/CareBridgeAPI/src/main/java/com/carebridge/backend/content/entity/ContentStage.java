package com.carebridge.backend.content.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Locale;

public enum ContentStage {
    PRE_PREGNANCY,
    PREGNANCY,
    POSTPARTUM,
    BABY_CARE;

    @JsonCreator
    public static ContentStage fromApiValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return valueOf(value.toUpperCase(Locale.ROOT));
    }
}
