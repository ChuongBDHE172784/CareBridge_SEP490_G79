package com.carebridge.backend.exercise.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "pregnancy_exercises")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PregnancyExercise {

    @Id
    @Column(name = "exercise_id", nullable = false)
    private UUID exerciseId;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "trimester_scope", length = 50)
    private TrimesterScope trimesterScope;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty_level", length = 30)
    private DifficultyLevel difficultyLevel;

    @Column(name = "duration_minutes")
    private Short durationMinutes;

    @Column(name = "instruction_content", columnDefinition = "text")
    private String instructionContent;

    @Column(name = "media_url", columnDefinition = "text")
    private String mediaUrl;

    @Column(name = "safety_warning", columnDefinition = "text")
    private String safetyWarning;

    @Column(name = "supports_posture_analysis", nullable = false)
    private Boolean supportsPostureAnalysis;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ExerciseStatus status;

    @Column(name = "version_no", nullable = false)
    private Integer versionNo;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
