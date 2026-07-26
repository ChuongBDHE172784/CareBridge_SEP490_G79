package com.carebridge.backend.triage.dto.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.carebridge.backend.triage.TriageStage;
import com.carebridge.backend.triage.OriginDashboard;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartIntakeConversationRequest {
    // Accepted for backward compatibility, but Spring always replaces it with DB UUID.
    private String intakeSessionId;

    @Size(max = 64, message = "clientRequestId must not exceed 64 characters")
    @Pattern(regexp = "^[A-Za-z0-9_-]{16,64}$", message = "clientRequestId must be a safe idempotency key")
    private String clientRequestId;

    @Size(max = 2000, message = "initialText must not exceed 2000 characters")
    private String initialText;

    @Builder.Default
    private Map<String, Object> currentIntake = new LinkedHashMap<>();

    private TriageStage stage;
    private UUID babyProfileId;
    private UUID motherProfileId;
    private UUID journeyId;
    private OriginDashboard originDashboard;
    private UUID originReferenceId;

    @JsonAnySetter
    public void rejectUnknownField(String fieldName, Object ignoredValue) {
        throw new IllegalArgumentException("Unsupported conversation start field: " + fieldName);
    }
}
