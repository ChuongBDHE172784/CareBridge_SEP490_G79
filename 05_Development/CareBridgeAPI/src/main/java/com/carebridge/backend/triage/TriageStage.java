package com.carebridge.backend.triage;

public enum TriageStage {
    PRECONCEPTION,
    PREGNANCY,
    POSTPARTUM,
    INFANT,
    TODDLER;

    public boolean isMaternal() {
        return this == PRECONCEPTION || this == PREGNANCY || this == POSTPARTUM;
    }

    public boolean isPediatric() {
        return this == INFANT || this == TODDLER;
    }
}
