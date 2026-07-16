package com.carebridge.backend.triage;

public enum TriageStage {
    PRECONCEPTION,
    PREGNANCY,
    INFANT,
    TODDLER;

    public boolean isMaternal() {
        return this == PRECONCEPTION || this == PREGNANCY;
    }

    public boolean isPediatric() {
        return this == INFANT || this == TODDLER;
    }
}
