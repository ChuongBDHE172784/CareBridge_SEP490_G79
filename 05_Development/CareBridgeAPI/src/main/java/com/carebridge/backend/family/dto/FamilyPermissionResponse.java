package com.carebridge.backend.family.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class FamilyPermissionResponse {

    private UUID memberId;
    private UUID careGroupId;
    private boolean calendar;
    private boolean logs;
    private boolean alerts;
    private boolean records;
    private Instant updatedAt;
}
