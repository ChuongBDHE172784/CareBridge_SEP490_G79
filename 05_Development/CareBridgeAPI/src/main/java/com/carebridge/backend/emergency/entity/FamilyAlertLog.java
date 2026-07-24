package com.carebridge.backend.emergency.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "safety_event_actions")
@org.hibernate.annotations.SQLRestriction("action_type = 'FAMILY_ALERT'")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FamilyAlertLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "safety_event_action_id")
    private UUID id;

    @Column(name = "safety_event_id", nullable = false, unique = true)
    private UUID sessionId;

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

    @PrePersist
    void prepareCanonicalAction() {
        actionType = "FAMILY_ALERT";
        if (idempotencyKey == null) idempotencyKey = "family-alert:" + UUID.randomUUID();
    }
}
