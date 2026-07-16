package com.carebridge.backend.triage.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntakeConversationResponse {
    private String status;
    private String intakeSessionId;
    private String stage;
    @Builder.Default
    private Map<String, Object> mergedIntake = new LinkedHashMap<>();
    @Builder.Default
    private List<Object> normalizedSymptomDetails = new ArrayList<>();
    private String assistantMessage;
    @Builder.Default
    private List<Object> questions = new ArrayList<>();
    private Integer round;
    private Map<String, Object> triageResult;
}
