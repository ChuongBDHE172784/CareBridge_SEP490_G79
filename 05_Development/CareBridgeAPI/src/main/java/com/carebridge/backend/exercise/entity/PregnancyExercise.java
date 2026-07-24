package com.carebridge.backend.exercise.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "care_item_templates")
@org.hibernate.annotations.SQLRestriction("entry_type = 'EXERCISE_TEMPLATE'")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PregnancyExercise {

    @Id
    @Column(name = "template_id", nullable = false)
    private UUID exerciseId;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", length = 30)
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
    @Column(name = "template_status", nullable = false, length = 20)
    private ExerciseStatus status;

    @Column(name = "version", nullable = false)
    private Integer versionNo;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "is_active", nullable = false)
    private Boolean active;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "configuration_jsonb", nullable = false, columnDefinition = "jsonb")
    private String configurationJson = "{}";

    @Column(name = "configuration_hash", nullable = false, length = 128)
    private String configurationHash;

    @Column(name = "effective_from")
    private OffsetDateTime effectiveFrom;

    @Column(name = "entry_type", nullable = false, updatable = false, length = 30)
    private String entryType = "EXERCISE_TEMPLATE";

    @PrePersist
    @PreUpdate
    void prepareCanonicalTemplate() {
        active = status == ExerciseStatus.PUBLISHED;
        if (effectiveFrom == null) {
            effectiveFrom = createdAt;
        }
        String payload = String.format(
                java.util.Locale.ROOT,
                "{\"difficulty\":\"%s\",\"durationMinutes\":%s,\"supportsPostureAnalysis\":%s}",
                difficultyLevel == null ? "" : difficultyLevel.name(),
                durationMinutes == null ? "null" : durationMinutes,
                Boolean.TRUE.equals(supportsPostureAnalysis));
        configurationJson = payload;
        configurationHash = sha256(payload);
    }

    private static String sha256(String value) {
        try {
            byte[] bytes = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(bytes);
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required", exception);
        }
    }
}
