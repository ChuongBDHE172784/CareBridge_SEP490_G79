package com.carebridge.backend.checklist.distribution;

/** Exact Family checklist permission truth table. */
public class ChecklistFamilyPermissionPolicy {

    public boolean canRead(boolean accepted, boolean checklistView, boolean checklistComplete) {
        return accepted && checklistView;
    }

    public boolean canAct(boolean accepted, boolean checklistView, boolean checklistComplete) {
        return accepted && checklistView && checklistComplete;
    }
}
