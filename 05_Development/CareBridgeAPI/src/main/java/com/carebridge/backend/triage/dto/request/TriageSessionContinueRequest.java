package com.carebridge.backend.triage.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A follow-up turn.
 *
 * <p>{@code answers} is the only way a client may answer planned questions. The chat asks up to
 * three questions per round, so answers arrive as a batch: sending them one at a time would burn a
 * {@code stateVersion} per question and turn one user action into three retryable requests.
 *
 * <p>The client supplies identifiers only; every clinical signal is derived server-side by
 * {@code CanonicalAnswerMapper}. {@code signals} and {@code measurements} remain in the shape purely
 * so an attempt to author clinical state is rejected loudly rather than ignored silently.
 */
public record TriageSessionContinueRequest(
        @NotNull UUID sessionId,
        @Min(0) int expectedStateVersion,
        @NotBlank @Size(max = 2000) String message,
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9_-]{16,64}$") String messageId,
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9_-]{16,64}$") String requestId,
        @Valid @Size(max = 8) List<TriageAnswerSelection> answers,
        Map<String, Object> signals,
        Map<String, Object> measurements) {

    public TriageSessionContinueRequest {
        answers = answers == null ? List.of() : List.copyOf(new ArrayList<>(answers));
        signals = signals == null ? new LinkedHashMap<>() : new LinkedHashMap<>(signals);
        measurements = measurements == null ? new LinkedHashMap<>() : new LinkedHashMap<>(measurements);
    }
}
