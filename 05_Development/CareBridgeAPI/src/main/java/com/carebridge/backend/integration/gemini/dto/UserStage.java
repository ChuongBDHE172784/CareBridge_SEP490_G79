package com.carebridge.backend.integration.gemini.dto;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum UserStage {
    PRE_PREGNANCY,
    PREGNANCY,
    POSTPARTUM;

    @JsonCreator
    public static UserStage fromApiValue(String value) {
        if ("BABY_CARE".equalsIgnoreCase(value)) {
            return POSTPARTUM;
        }
        return valueOf(value.toUpperCase());
    }
}
