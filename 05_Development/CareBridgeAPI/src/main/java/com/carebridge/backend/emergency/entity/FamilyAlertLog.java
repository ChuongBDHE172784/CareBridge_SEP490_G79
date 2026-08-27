package com.carebridge.backend.emergency.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "safety_events")
@org.hibernate.annotations.SQLRestriction("action_type = 'FAMILY_ALERT'")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FamilyAlertLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "safety_event_id")
    private UUID id;

    @Column(name = "parent_event_id", nullable = false)
    private UUID sessionId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "created_at", nullable = false)
    private Instant sentAt;

    @Column(name = "recipient_count", nullable = false)
    private int recipientCount;

    @Column(name = "location_included", nullable = false)
    private boolean locationIncluded;

    @Column(name = "created_by_text", nullable = false)
    private String createdBy;

    @Builder.Default
    @Column(name = "action_type", nullable = false, updatable = false)
    private String actionType = "FAMILY_ALERT";
    @Column(name = "idempotency_key", nullable = false, updatable = false)
    private String idempotencyKey;
    @Builder.Default
    @Column(name = "event_type", nullable = false, updatable = false)
    private String eventType = "ACTION";
    @Builder.Default
    @Column(name = "record_type", nullable = false, updatable = false)
    private String recordType = "SAFETY_ACTION";
    @Builder.Default
    @Column(name = "alert_generation", nullable = false)
    private long alertGeneration = 0;
    @Builder.Default
    @Column(name = "alert_successful_recipient_count", nullable = false)
    private int alertSuccessfulRecipientCount = 0;
    @Builder.Default
    @Column(name = "alert_failed_recipient_count", nullable = false)
    private int alertFailedRecipientCount = 0;
    @Column(name = "detected_at", nullable = false, updatable = false)
    private Instant detectedAt;

    @PrePersist
    void prepareCanonicalAction() {
        actionType = "FAMILY_ALERT";
        recordType = "SAFETY_ACTION";
        if (idempotencyKey == null) idempotencyKey = "family-alert:" + UUID.randomUUID();
        if (detectedAt == null) detectedAt = sentAt == null ? Instant.now() : sentAt;
    }
}
