package com.carebridge.backend.journey.entity;

public enum PregnancyOutcomeType {
    ONGOING,
    UNKNOWN,
    LIVE_BIRTH,
    PREGNANCY_LOSS,
    STILLBIRTH;

    public boolean transitionsToPostpartum() {
        return this == LIVE_BIRTH || this == PREGNANCY_LOSS || this == STILLBIRTH;
    }
}
