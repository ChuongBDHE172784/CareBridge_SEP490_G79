package com.carebridge.backend.family.dto;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FamilyPermissionResponse {

    private UUID memberId;
    private UUID careGroupId;
    private boolean calendar;
    private boolean logs;
    private boolean alerts;
    private boolean records;
    private boolean checklistView;
    private boolean checklistComplete;
    private boolean quickNotes;
    private boolean quickNoteWeight;
    private boolean quickNoteHydration;
    private boolean quickNoteEpds;
    private boolean quickNoteFetalMovement;
    private Instant updatedAt;
}
