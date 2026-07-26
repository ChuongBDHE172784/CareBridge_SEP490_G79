package com.carebridge.backend.health.entity;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * API compatibility projection. The canonical association lives directly in
 * attachments.health_record_id rather than a separate join table.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthRecordFile {
    private UUID id;
    private UUID healthRecordId;
    private UUID fileId;
    private int displayOrder;
    private Instant createdAt;
}
