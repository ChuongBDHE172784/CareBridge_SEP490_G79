package com.carebridge.backend.exercise.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminPostureConfigResponse {

    private UUID postureConfigId;
    private UUID exerciseId;
    private UUID configuredBy;
    private String analysisMode;
    private String ruleOrModelVersion;
    private BigDecimal confidenceThreshold;
    private String feedbackLevel;
    private String configJson;
    private OffsetDateTime effectiveFrom;
    private OffsetDateTime effectiveTo;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
