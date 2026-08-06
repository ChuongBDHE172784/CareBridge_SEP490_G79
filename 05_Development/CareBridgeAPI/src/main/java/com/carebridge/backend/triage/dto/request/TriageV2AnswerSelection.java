package com.carebridge.backend.triage.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * One answered question, as identifiers only.
 *
 * <p>The client states which question it was asked and which option it chose. It never states what
 * that means clinically — {@code CanonicalAnswerMapper} decides that on the server. Both fields are
 * pattern-constrained so a forged or malformed payload is rejected before it reaches the mapper.
 */
public record TriageV2AnswerSelection(
        @NotBlank @Pattern(regexp = "^[A-Z0-9_]{1,64}$") String questionId,
        @NotBlank @Pattern(regexp = "^[A-Z0-9_]{1,64}$") String optionCode) {
}
