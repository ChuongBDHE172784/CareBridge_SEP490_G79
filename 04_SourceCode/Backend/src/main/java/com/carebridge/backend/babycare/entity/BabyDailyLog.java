package com.carebridge.backend.babycare.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    @Column(name = "baby_log_id")
    private UUID babyLogId;

    @Column(name = "baby_id")
    private UUID babyId;

    @Column(name = "log_type", length = 30)
    private String logType;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "quantity")
    private BigDecimal quantity;

    @Column(name = "unit", length = 20)
    private String unit;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "recorded_by")
    private UUID recordedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
