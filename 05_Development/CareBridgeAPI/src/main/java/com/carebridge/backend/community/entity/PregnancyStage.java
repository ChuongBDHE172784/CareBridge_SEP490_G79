package com.carebridge.backend.community.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum PregnancyStage {
    PRE_PREGNANCY,
    PREGNANCY,
    POSTPARTUM;

    @JsonCreator
    public static PregnancyStage fromApiValue(String value) {
        if ("BABY_CARE".equalsIgnoreCase(value)) {
            return POSTPARTUM;
        }
        return valueOf(value.toUpperCase());
    }
}
