package com.carebridge.backend.triage.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public record TriageV2StartRequest(
        UUID profileId,
        @Pattern(regexp = "MOTHER|BABY|UNKNOWN") String selectedTarget,
        Map<String, Object> journeyContext,
        @NotBlank @Size(max = 2000) String message,
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9_-]{16,64}$") String messageId,
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9_-]{16,64}$") String requestId,
        @NotNull Map<String, Object> consentContext,
        Map<String, Object> signals,
        Map<String, Object> measurements) {

    public TriageV2StartRequest {
        journeyContext = copy(journeyContext);
        consentContext = copy(consentContext);
        signals = copy(signals);
        measurements = copy(measurements);
    }

    private static Map<String, Object> copy(Map<String, Object> value) {
        return value == null ? new LinkedHashMap<>() : new LinkedHashMap<>(value);
    }
}
