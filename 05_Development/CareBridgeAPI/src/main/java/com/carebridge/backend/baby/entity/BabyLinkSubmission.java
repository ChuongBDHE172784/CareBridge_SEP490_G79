package com.carebridge.backend.baby.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="baby_link_submissions", uniqueConstraints=@UniqueConstraint(name="uq_baby_link_submission", columnNames={"owner_user_id","operation_type","submission_id"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class BabyLinkSubmission {
    @Id @GeneratedValue(strategy=GenerationType.UUID) @Column(name="link_submission_id") private UUID id;
    @Column(name="owner_user_id", nullable=false, updatable=false) private UUID ownerUserId;
    @Enumerated(EnumType.STRING) @Column(name="operation_type", nullable=false, updatable=false, length=30) private BabyLinkOperation operationType;
    @Column(name="submission_id", nullable=false, updatable=false) private UUID submissionId;
    @Column(name="semantic_intent", nullable=false, updatable=false, length=1000) private String semanticIntent;
    @Column(name="baby_id", nullable=false, updatable=false) private UUID babyId;
    @Column(name="journey_id", nullable=false, updatable=false) private UUID journeyId;
    @CreationTimestamp @Column(name="created_at", nullable=false, updatable=false) private Instant createdAt;
}
