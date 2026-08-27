package com.carebridge.backend.triage.dto.response;

import com.carebridge.backend.triage.entity.HealthMemoryEntry;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class HealthMemoryEntryResponse {
    UUID id;
    String relatedStage;
    String summaryText;
    UUID sourceSessionId;
    Instant createdAt;
    Instant expiresAt;

    public static HealthMemoryEntryResponse from(HealthMemoryEntry entry) {
        return builder().id(entry.getId()).relatedStage(entry.getRelatedStage().name())
                .summaryText(entry.getSummaryText()).sourceSessionId(entry.getSourceSessionId())
                .createdAt(entry.getCreatedAt()).expiresAt(entry.getExpiresAt()).build();
    }
}
