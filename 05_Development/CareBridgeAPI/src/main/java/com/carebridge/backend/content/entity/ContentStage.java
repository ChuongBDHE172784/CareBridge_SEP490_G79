package com.carebridge.backend.content.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

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
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return POSTPARTUM;
        }
    }
}
