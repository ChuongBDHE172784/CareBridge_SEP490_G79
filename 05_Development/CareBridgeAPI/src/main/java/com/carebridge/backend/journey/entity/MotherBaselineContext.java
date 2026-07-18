package com.carebridge.backend.journey.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "mother_baseline_contexts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MotherBaselineContext {

    @Id
    @Column(name = "baseline_id", nullable = false)
    private UUID id;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(name = "submission_id", nullable = false)
    private UUID submissionId;

    @Column(nullable = false)
    private long revision;

    @Column(name = "schema_version", nullable = false, length = 40)
    private String schemaVersion;

    @Column(name = "source", nullable = false, length = 30)
    private String source;

    @Enumerated(EnumType.STRING)
    @Column(name = "lifecycle_goal", nullable = false, length = 40)
    private LifecycleGoal lifecycleGoal;

    @Column(nullable = false, length = 20)
    private String locale;

    @Column(name = "time_zone", nullable = false, length = 80)
    private String timeZone;

    @Column(nullable = false, length = 300)
    private String preferences;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;
}
