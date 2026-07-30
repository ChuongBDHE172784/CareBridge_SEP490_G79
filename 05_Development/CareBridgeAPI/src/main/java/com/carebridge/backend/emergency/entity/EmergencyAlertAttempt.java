package com.carebridge.backend.emergency.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "safety_events")
@org.hibernate.annotations.SQLRestriction("action_type = 'ALERT_ATTEMPT'")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyAlertAttempt {
    @Id
    @Column(name = "safety_event_id", nullable = false)
    private UUID id;
    @Column(name = "parent_event_id", nullable = false, updatable = false)
    private UUID emergencySessionId;
    @Column(name = "status", nullable = false, length = 20)
    private String status;
    @Column(name = "detected_at", nullable = false)
    private Instant startedAt;
    @Column(name = "response_at")
    private Instant completedAt;
    @Transient
    private Instant leaseExpiresAt;
    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;
    @Transient
    private int successfulRecipientCount;
    @Transient
    private int failedRecipientCount;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Column(name = "action_type", nullable = false, updatable = false)
    private String actionType = "ALERT_ATTEMPT";
    @Column(name = "idempotency_key", nullable = false, updatable = false)
    private String idempotencyKey;
    @Column(name = "event_type", nullable = false, updatable = false)
    private String eventType = "ACTION";
    @Column(name = "record_type", nullable = false, updatable = false)
    private String recordType = "SAFETY_ACTION";
    @Column(name = "alert_generation", nullable = false)
    private long alertGeneration = 0;
    @Column(name = "alert_successful_recipient_count", nullable = false)
    private int alertSuccessfulRecipientCount = 0;
    @Column(name = "alert_failed_recipient_count", nullable = false)
    private int alertFailedRecipientCount = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "location_snapshot_jsonb", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> attemptMetadata = new HashMap<>();

    @PrePersist
    @PreUpdate
    void writeAttemptMetadata() {
        actionType = "ALERT_ATTEMPT";
        recordType = "SAFETY_ACTION";
        if (idempotencyKey == null) idempotencyKey = "alert-attempt:" + UUID.randomUUID();
        if (startedAt == null) startedAt = Instant.now();
        if (attemptMetadata == null) attemptMetadata = new HashMap<>();
        put("leaseExpiresAt", leaseExpiresAt == null ? null : leaseExpiresAt.toString());
        put("successfulRecipientCount", successfulRecipientCount);
        put("failedRecipientCount", failedRecipientCount);
    }

    @PostLoad
    void readAttemptMetadata() {
        if (attemptMetadata == null) return;
        Object lease = attemptMetadata.get("leaseExpiresAt");
        leaseExpiresAt = lease == null ? null : Instant.parse(lease.toString());
        successfulRecipientCount = integer("successfulRecipientCount");
        failedRecipientCount = integer("failedRecipientCount");
    }

    private void put(String key, Object value) {
        if (value == null) attemptMetadata.remove(key); else attemptMetadata.put(key, value);
    }

    private int integer(String key) {
        Object value = attemptMetadata.get(key);
        return value instanceof Number number ? number.intValue()
                : value == null ? 0 : Integer.parseInt(value.toString());
    }
}
