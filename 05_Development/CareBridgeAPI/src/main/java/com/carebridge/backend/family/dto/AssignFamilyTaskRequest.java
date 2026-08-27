package com.carebridge.backend.family.dto;

import com.carebridge.backend.checklist.model.ChecklistTargetSubject;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class AssignFamilyTaskRequest {

    /** User ID (account) of the target ACCEPTED member in the group. */
    @NotNull
    private UUID assigneeMemberId;

    @NotBlank
    @Size(max = 255)
    private String title;

    @Size(max = 2000)
    private String description;

    /** Must be strictly after now() — ADR-FAM-033. */
    @NotNull
    private Instant dueAt;

    /** V2 requires an explicit MOTHER/BABY target; recipient role is independent. */
    @NotNull
    private ChecklistTargetSubject targetSubject;
}
