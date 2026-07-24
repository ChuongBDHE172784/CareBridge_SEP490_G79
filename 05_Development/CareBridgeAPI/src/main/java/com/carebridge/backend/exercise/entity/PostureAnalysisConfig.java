package com.carebridge.backend.exercise.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "care_item_templates")
@org.hibernate.annotations.SQLRestriction("entry_type = 'POSTURE_CONFIG'")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostureAnalysisConfig {

    @Id
    @Column(name = "template_id", nullable = false)
    private UUID postureConfigId;

    @Column(name = "parent_template_id", nullable = false)
    private UUID exerciseId;

    @Column(name = "configured_by", nullable = false)
    private UUID configuredBy;

    @Column(name = "analysis_mode", length = 30)
    private String analysisMode;

    @Column(name = "rule_or_model_version", length = 80)
    private String ruleOrModelVersion;

    @Column(name = "confidence_threshold")
    private BigDecimal confidenceThreshold;

    @Column(name = "feedback_level", length = 30)
    private String feedbackLevel;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "configuration_jsonb", nullable = false, columnDefinition = "jsonb")
    private String configJson;

    @Column(name = "effective_from", nullable = false)
    private OffsetDateTime effectiveFrom;

    @Column(name = "effective_to")
    private OffsetDateTime effectiveTo;

    @Column(name = "template_status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Builder.Default
    @Column(name = "entry_type", nullable = false, updatable = false, length = 30)
    private String entryType = "POSTURE_CONFIG";

    @Column(name = "title", nullable = false, length = 255)
    private String canonicalTitle;

    @Column(name = "is_active", nullable = false)
    private Boolean active;

    @Builder.Default
    @Column(name = "version", nullable = false)
    private Integer version = 1;

    @Column(name = "configuration_hash", nullable = false, length = 128)
    private String configurationHash;

    @PrePersist
    @PreUpdate
    void prepareCanonicalTemplate() {
        canonicalTitle = "Posture config " + (ruleOrModelVersion == null ? postureConfigId : ruleOrModelVersion);
        active = "ACTIVE".equals(status);
        if (configJson == null) {
            configJson = "{}";
        }
        configurationHash = sha256(configJson);
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
