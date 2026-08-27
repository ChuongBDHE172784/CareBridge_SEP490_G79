package com.carebridge.backend.triage.dto.request;

import com.carebridge.backend.triage.OriginDashboard;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public record TriageSessionStartRequest(
        UUID profileId,
        @NotNull @Pattern(regexp = "MOTHER|BABY|UNKNOWN") String selectedTarget,
        @NotNull @Pattern(regexp = "PRECONCEPTION|POSSIBLE_PREGNANCY|PREGNANCY|POSTPARTUM_MOTHER|INFANT_0_12M|TODDLER_12_24M")
        String selectedStage,
        Map<String, Object> journeyContext,
        @NotBlank @Size(max = 2000) String message,
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9_-]{16,64}$") String messageId,
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9_-]{16,64}$") String requestId,
        @NotNull Map<String, Object> consentContext,
        Map<String, Object> signals,
        Map<String, Object> measurements,
        UUID journeyId,
        OriginDashboard originDashboard,
        UUID originReferenceId) {

    public TriageSessionStartRequest {
        journeyContext = copy(journeyContext);
        consentContext = copy(consentContext);
        signals = copy(signals);
        measurements = copy(measurements);
    }

    /** Source-compatible constructor for direct entries without a lifecycle origin. */
    public TriageSessionStartRequest(
            UUID profileId, String selectedTarget, String selectedStage,
            Map<String, Object> journeyContext, String message, String messageId,
            String requestId, Map<String, Object> consentContext,
            Map<String, Object> signals, Map<String, Object> measurements) {
        this(profileId, selectedTarget, selectedStage, journeyContext, message, messageId,
                requestId, consentContext, signals, measurements, null, null, null);
    }

    private static Map<String, Object> copy(Map<String, Object> value) {
        return value == null ? new LinkedHashMap<>() : new LinkedHashMap<>(value);
    }
}
