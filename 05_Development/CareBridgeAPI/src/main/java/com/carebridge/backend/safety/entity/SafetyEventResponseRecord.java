package com.carebridge.backend.safety.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "safety_event_actions")
@org.hibernate.annotations.SQLRestriction("action_type = 'RESPONSE'")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SafetyEventResponseRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "safety_event_action_id")
    private UUID id;

    @Column(name = "safety_event_id", nullable = false, unique = true)
    private UUID safetyEventId;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(name = "response_type", nullable = false, length = 30)
    private String responseType;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "responded_at", nullable = false)
    private Instant respondedAt;

    @Column(name = "created_by_user_id")
    private UUID createdBy;

    @Column(name = "actor_type", nullable = false, length = 20)
    private String actorType;

    @Builder.Default
    @Column(name = "action_type", nullable = false, updatable = false)
    private String actionType = "RESPONSE";

    @Column(name = "idempotency_key", nullable = false, updatable = false)
    private String idempotencyKey;

    @PrePersist
    void prepareCanonicalAction() {
        actionType = "RESPONSE";
        if (idempotencyKey == null) idempotencyKey = "response:" + UUID.randomUUID();
    }
}
