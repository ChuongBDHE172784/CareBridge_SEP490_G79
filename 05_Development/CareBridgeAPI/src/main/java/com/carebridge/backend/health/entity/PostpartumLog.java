package com.carebridge.backend.health.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

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
    @Column(name = "postpartum_log_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "journey_id", nullable = false)
    private UUID journeyId;

    @Column(name = "log_date", nullable = false)
    private LocalDate logDate;

    @Column(name = "pain_level")
    private Integer painLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "bleeding_level", length = 20)
    private BleedingLevel bleedingLevel;

    @Column(name = "mood_level")
    private Integer moodLevel;

    @Column(name = "sleep_hours", precision = 4, scale = 1)
    private BigDecimal sleepHours;

    @Column(name = "breastfeeding_note", columnDefinition = "text")
    private String breastfeedingNote;

    @Column(name = "symptom_note", columnDefinition = "text")
    private String symptomNote;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
