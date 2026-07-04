package com.carebridge.backend.exercise.dto;

import com.carebridge.backend.exercise.entity.AnalysisMode;
import com.carebridge.backend.exercise.entity.PostureFeedbackLevel;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreatePostureConfigRequest {

    @NotNull
    private UUID exerciseId;

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
}
