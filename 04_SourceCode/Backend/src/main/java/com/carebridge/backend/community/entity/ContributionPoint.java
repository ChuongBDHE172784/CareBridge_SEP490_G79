package com.carebridge.backend.community.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
@Table(name = "contribution_points")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContributionPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "point_record_id")
    private UUID pointRecordId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "source_type", length = 40)
    private String sourceType;

    @Column(name = "source_id")
    private UUID sourceId;

    @Column(name = "points")
    private Integer points;

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "recorded_at")
    private Instant recordedAt;
}
