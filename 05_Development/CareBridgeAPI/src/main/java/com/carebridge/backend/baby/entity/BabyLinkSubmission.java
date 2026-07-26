package com.carebridge.backend.baby.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "audit_events")
@SQLRestriction("event_category LIKE 'BABY_LINK_%'")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BabyLinkSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "audit_event_id")
    private UUID id;

    @Column(name = "actor_user_id", nullable = false, updatable = false)
    private UUID ownerUserId;

    @Transient private BabyLinkOperation operationType;

    @Transient private UUID submissionId;

    @Transient private String semanticIntent;

    @Column(name = "resource_id", nullable = false, updatable = false)
    private UUID babyId;

    @Builder.Default
    @Column(name = "resource_type", nullable = false, updatable = false, length = 100)
    private String resourceType = "care_subjects";

    @Column(name = "subject_reference_id", nullable = false, updatable = false)
    private UUID journeyId;

    @CreationTimestamp
    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "event_category", nullable = false, updatable = false, length = 80)
    private String eventCategory;

    @Builder.Default
    @Transient
    private String eventOrigin = "BABY_LINK";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Transient private Instant effectiveAt;

    @PrePersist
    void prepareCanonicalEvent() {
        if (operationType == null && eventCategory != null) {
            operationType = BabyLinkOperation.valueOf(eventCategory.substring("BABY_LINK_".length()));
        }
        eventCategory = "BABY_LINK_" + operationType.name();
        effectiveAt = createdAt == null ? Instant.now() : createdAt;
        Map<String, Object> value = payload == null ? new LinkedHashMap<>() : new LinkedHashMap<>(payload);
        value.put("operationType", operationType.name());
        value.put("submissionId", submissionId);
        value.put("semanticIntent", semanticIntent);
        payload = value;
    }

    @PostLoad
    void hydrateCanonicalEvent() {
        effectiveAt = createdAt;
        if (eventCategory != null) {
            operationType = BabyLinkOperation.valueOf(eventCategory.substring("BABY_LINK_".length()));
        }
        if (payload != null) {
            Object submission = payload.get("submissionId");
            submissionId = submission == null ? null : UUID.fromString(submission.toString());
            Object intent = payload.get("semanticIntent");
            semanticIntent = intent == null ? null : intent.toString();
        }
    }

    @PreUpdate
    @PreRemove
    void rejectMutation() {
        throw new UnsupportedOperationException("Baby link submissions are append-only");
    }
}
