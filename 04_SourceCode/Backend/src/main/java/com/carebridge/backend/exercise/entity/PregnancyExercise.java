package com.carebridge.backend.exercise.entity;

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
@Table(name = "pregnancy_exercises")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PregnancyExercise {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "exercise_id")
    private UUID exerciseId;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "trimester_scope", length = 30)
    private String trimesterScope;

    @Column(name = "difficulty_level", length = 20)
    private String difficultyLevel;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "instruction_content", columnDefinition = "TEXT")
    private String instructionContent;

    @Column(name = "media_url", length = 500)
    private String mediaUrl;

    @Column(name = "safety_warning", columnDefinition = "TEXT")
    private String safetyWarning;

    @Column(name = "supports_posture_analysis")
    private Boolean supportsPostureAnalysis;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "version_no")
    private Integer versionNo;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
