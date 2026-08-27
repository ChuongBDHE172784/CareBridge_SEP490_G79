package com.carebridge.backend.safety.entity;

import com.carebridge.backend.safety.ImuSessionStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "safety_monitoring_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImuMonitoringSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "monitoring_session_id")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private ImuSessionStatus status;

    @Column(name = "sensitivity_level", nullable = false, length = 10)
    private String sensitivityLevel;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "created_by")
    private UUID createdBy;
}
