package com.carebridge.backend.expert.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "audit_events", schema = "public")
@SQLRestriction("event_category = 'EXPERT_CONTRIBUTION'")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContributionPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "audit_event_id", nullable = false, updatable = false)
    private UUID pointRecordId;

    @jakarta.persistence.Transient
    private Integer points;

    @jakarta.persistence.Transient
    private String reason;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime recordedAt;

    @Column(name = "subject_reference_id")
    private UUID sourceId;

    @Column(name = "resource_type", length = 100)
    private String sourceType;

    @Column(name = "actor_user_id", nullable = false)
    private UUID userId;

    @Builder.Default
    @Column(name = "event_category", nullable = false, length = 80)
    private String eventCategory = "EXPERT_CONTRIBUTION";

    @Builder.Default
    @Column(name = "event_origin", length = 80)
    private String eventOrigin = "EXPERT_CONTRIBUTION";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @PrePersist
    void preparePayload() {
        payload = payload == null ? new HashMap<>() : new HashMap<>(payload);
        payload.put("points", points == null ? 0 : points);
        if (reason != null) payload.put("reason", reason);
    }

    @PostLoad
    void hydratePayload() {
        if (payload == null) return;
        Object value = payload.get("points");
        if (value instanceof Number number) points = number.intValue();
        if (payload.get("reason") != null) reason = payload.get("reason").toString();
    }
}
