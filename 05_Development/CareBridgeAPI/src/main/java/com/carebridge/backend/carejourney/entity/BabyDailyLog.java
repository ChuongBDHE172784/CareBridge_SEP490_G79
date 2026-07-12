package com.carebridge.backend.carejourney.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "baby_daily_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BabyDailyLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "baby_log_id", updatable = false, nullable = false)
    private UUID babyLogId;

    @Column(name = "baby_id", nullable = false)
    private UUID babyId;

    @Column(name = "log_type", nullable = false, length = 30)
    private String logType;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "quantity", precision = 10, scale = 2)
    private BigDecimal quantity;

    @Column(name = "unit", length = 20)
    private String unit;

    @Column(name = "note", columnDefinition = "text")
    private String note;

    @Column(name = "recorded_by")
    private UUID recordedBy;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BabyDailyLogStatus status = BabyDailyLogStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
