package com.carebridge.backend.exercise.dto;

import java.math.BigDecimal;
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
public class PostureFeedbackResponse {

    private String postureCode;
    private BigDecimal confidenceScore;
    private String severity;
    private String feedbackText;
}
