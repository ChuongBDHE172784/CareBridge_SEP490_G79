package com.carebridge.backend.audit.entity;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityEventNote {

    private UUID noteId;

    private Long eventId;

    private UUID securityEventId;

    private UUID authorId;

    private String noteText;

    @Builder.Default
    private String eventCategory = "SECURITY_INVESTIGATION_NOTE";

    private Instant createdAt;

    void validateInvestigationNote() {
        if (eventId == null || authorId == null || noteText == null || noteText.isBlank()) {
            throw new IllegalStateException(
                    "Security investigation notes require event, author, and note text");
        }
    }
}

