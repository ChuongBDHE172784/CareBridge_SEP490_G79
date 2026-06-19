package com.carebridge.backend.carejourney.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "postpartum_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostpartumLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "postpartum_log_id")
    private UUID postpartumLogId;

    @Column(name = "journey_id")
    private UUID journeyId;

    @Column(name = "log_date")
    private LocalDate logDate;

    @Column(name = "pain_level")
    private String painLevel;

    @Column(name = "bleeding_level", length = 20)
    private String bleedingLevel;

    @Column(name = "mood_level")
    private String moodLevel;

    @Column(name = "sleep_hours")
    private BigDecimal sleepHours;

    @Column(name = "breastfeeding_note", columnDefinition = "TEXT")
    private String breastfeedingNote;

    @Column(name = "symptom_note", columnDefinition = "TEXT")
    private String symptomNote;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
