package com.carebridge.backend.expert.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "expert_contribution_events", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContributionPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "contribution_event_id", nullable = false, updatable = false)
    private UUID pointRecordId;

    @Column(name = "points", nullable = false)
    private Integer points;

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime recordedAt;

    @Column(name = "source_id")
    private UUID sourceId;

    @Column(name = "source_type", length = 40)
    private String sourceType;

    @Column(name = "actor_user_id", nullable = false)
    private UUID userId;
}
