package com.carebridge.backend.family.entity;

/**
 * Permission flag keys used by CareGroupAccessPolicy.hasPermission().
 * Open — shape owned by sibling UC72_ManageFamilyPermission; keys map to
 * boolean fields in permission_json (ADR-FAM-003).
 */
public enum PermissionFlag {
    CALENDAR("calendar"),
    LOGS("logs"),
    ALERTS("alerts"),
    RECORDS("records"),
    BABY_VIEW("baby_view"),
    BABY_JOURNAL_WRITE("baby_journal_write"),
    BABY_GROWTH_WRITE("baby_growth_write"),
    QUICK_NOTES("quickNotes"),
    QUICK_NOTE_WEIGHT("quickNoteWeight"),
    QUICK_NOTE_HYDRATION("quickNoteHydration"),
    QUICK_NOTE_EPDS("quickNoteEpds"),
    QUICK_NOTE_FETAL_MOVEMENT("quickNoteFetalMovement");

    private final String jsonKey;

    PermissionFlag(String jsonKey) {
        this.jsonKey = jsonKey;
    }

    public String jsonKey() {
        return jsonKey;
    }
}
