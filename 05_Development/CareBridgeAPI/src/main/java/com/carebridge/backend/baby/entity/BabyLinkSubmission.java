package com.carebridge.backend.baby.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="mother_journey_events", uniqueConstraints=@UniqueConstraint(name="uq_baby_link_submission", columnNames={"owner_user_id","operation_type","submission_id","legacy_source"}))
@org.hibernate.annotations.SQLRestriction("legacy_source = 'BABY_LINK'")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class BabyLinkSubmission {
    @Id @GeneratedValue(strategy=GenerationType.UUID) @Column(name="event_id") private UUID id;
    @Column(name="owner_user_id", nullable=false, updatable=false) private UUID ownerUserId;
    @Enumerated(EnumType.STRING) @Column(name="operation_type", nullable=false, updatable=false, length=30) private BabyLinkOperation operationType;
    @Column(name="submission_id", nullable=false, updatable=false) private UUID submissionId;
    @Column(name="semantic_intent", nullable=false, updatable=false, length=1000) private String semanticIntent;
    @Column(name="care_subject_id", nullable=false, updatable=false) private UUID babyId;
    @Column(name="mother_journey_id", nullable=false, updatable=false) private UUID journeyId;
    @CreationTimestamp @Column(name="recorded_at", nullable=false, updatable=false) private Instant createdAt;
    @Builder.Default @Column(name="event_type", nullable=false, updatable=false, length=60) private String eventType="BABY_LINK_SUBMISSION";
    @Builder.Default @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name="event_payload_jsonb", nullable=false, columnDefinition="jsonb") private String payloadJson="{}";
    @Builder.Default @Column(name="schema_version", nullable=false, updatable=false, length=30) private String schemaVersion="1";
    @Column(name="effective_at", nullable=false, updatable=false) private Instant effectiveAt;
    @Builder.Default @Column(name="legacy_source", nullable=false, updatable=false, length=60) private String legacySource="BABY_LINK";
    @Column(name="legacy_id", nullable=false, updatable=false, length=100) private String legacyId;

    @PrePersist void prepareCanonicalEvent() {
        effectiveAt = createdAt == null ? Instant.now() : createdAt;
        legacyId = id.toString();
        eventType = "BABY_LINK_" + operationType.name();
    }
}
