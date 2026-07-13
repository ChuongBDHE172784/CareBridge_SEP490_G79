package com.carebridge.backend.triage.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
public class ContinueIntakeConversationRequest {
    @NotBlank(message = "intakeSessionId is required")
    @Pattern(
            regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89aAbB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$",
            message = "intakeSessionId must be a UUID"
    )
    private String intakeSessionId;

    // Compatibility-only. Canonical current intake is loaded from DB.
    @Builder.Default
    private Map<String, Object> currentIntake = new LinkedHashMap<>();

    @Size(max = 50, message = "messages must not contain more than 50 items")
    @Builder.Default
    private List<Map<String, Object>> messages = new ArrayList<>();

    @Size(max = 20, message = "newAnswers must not contain more than 20 fields")
    @Builder.Default
    private Map<String, Object> newAnswers = new LinkedHashMap<>();

    // Compatibility-only. Canonical round is loaded from DB.
    private Integer round;
}
