package com.carebridge.backend.exercise.dto;

import com.carebridge.backend.exercise.entity.AnalysisMode;
import com.carebridge.backend.exercise.entity.PostureFeedbackLevel;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class    UpdatePostureConfigRequest {

    @NotNull
    private AnalysisMode analysisMode;

    @Size(max = 80)
    private String ruleOrModelVersion;

    @NotNull
    @DecimalMin(value = "0.0")
    @DecimalMax(value = "1.0")
    private BigDecimal confidenceThreshold;

    private PostureFeedbackLevel feedbackLevel;

    private String configJson;
    // No exerciseId (path variable), no effectiveFrom (ADR-PAC-003 — system-set)
}
