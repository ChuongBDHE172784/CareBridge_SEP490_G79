package com.carebridge.backend.family.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class CalendarItemDto {
    private UUID taskId;
    private String title;
    private String description;
    private Instant dueAt;
    private String status;
    private UUID assignedTo;
    /** Open — resolved from accounts table; v1 may be null if unresolvable. */
    private String assignedToDisplayName;
}
