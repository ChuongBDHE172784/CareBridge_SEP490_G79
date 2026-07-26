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
public class SecurityEvent {

    private Long id;

    /**
     * Internal canonical identifier. The public contract intentionally keeps the
     * historical Long id while persistence uses audit_events.audit_event_id.
     */
    private UUID auditEventId;

    private Instant occurredAt;

    private SecurityEventType eventType;

    private UUID userId;

    private String ipAddress;

    private String details;

    private String userAgent;

    private String payload;

    private UUID correlationId;

    @Builder.Default
    private String severity = "MEDIUM";

    @Builder.Default
    private String status = "OPEN";

    private UUID reviewedBy;

    private Instant reviewedAt;
}
