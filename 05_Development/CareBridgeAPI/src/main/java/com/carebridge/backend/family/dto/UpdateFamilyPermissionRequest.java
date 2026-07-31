package com.carebridge.backend.family.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateFamilyPermissionRequest {

    private Boolean calendar;
    private Boolean logs;
    private Boolean alerts;
    private Boolean records;
    private Boolean checklistView;
    private Boolean checklistComplete;
    private Boolean quickNotes;
    private Boolean quickNoteWeight;
    private Boolean quickNoteHydration;
    private Boolean quickNoteEpds;
    private Boolean quickNoteFetalMovement;

    public boolean hasAtLeastOneField() {
        return calendar != null || logs != null || alerts != null || records != null
                || checklistView != null || checklistComplete != null
                || quickNotes != null || quickNoteWeight != null || quickNoteHydration != null
                || quickNoteEpds != null || quickNoteFetalMovement != null;
    }
}
