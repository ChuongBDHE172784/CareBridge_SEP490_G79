package com.carebridge.backend.family.entity;

/**
 * Permission flag keys stored in care_group_members.permission_json.
 */
public enum PermissionFlag {
    CALENDAR("calendar"),
    LOGS("logs"),
    ALERTS("alerts"),
    RECORDS("records"),
    BABY_VIEW("baby_view"),
    BABY_JOURNAL_WRITE("baby_journal_write"),
    BABY_GROWTH_WRITE("baby_growth_write"),
    CHECKLIST_VIEW("CHECKLIST_VIEW"),
    CHECKLIST_COMPLETE("CHECKLIST_COMPLETE"),
    QUICK_NOTES("quickNotes"),
    QUICK_NOTE_WEIGHT("quickNoteWeight"),
    QUICK_NOTE_HYDRATION("quickNoteHydration"),
    QUICK_NOTE_EPDS("quickNoteEpds"),
    QUICK_NOTE_FETAL_MOVEMENT("quickNoteFetalMovement"),
    QUICK_NOTE_BLOOD_PRESSURE("quickNoteBloodPressure"),
    QUICK_NOTE_BLOOD_GLUCOSE("quickNoteBloodGlucose");

    private final String jsonKey;

    PermissionFlag(String jsonKey) {
        this.jsonKey = jsonKey;
    }

    public String jsonKey() {
        return jsonKey;
    }
}
