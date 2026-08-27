package com.carebridge.backend.triage.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.AssertTrue;

import java.math.BigDecimal;

/**
 * One answered question, as identifiers only.
 *
 * <p>The client states which question it was asked and which option it chose. It never states what
 * that means clinically — {@code CanonicalAnswerMapper} decides that on the server. Both fields are
 * pattern-constrained so a forged or malformed payload is rejected before it reaches the mapper.
 */
public record TriageAnswerSelection(
        @NotBlank @Pattern(regexp = "^[A-Z0-9_]{1,64}$") String questionId,
        @Pattern(regexp = "^[A-Z0-9_]{1,64}$") String optionCode,
        BigDecimal numericValue) {

    public TriageAnswerSelection(String questionId, String optionCode) {
        this(questionId, optionCode, null);
    }

    @AssertTrue(message = "answer must contain exactly one value")
    public boolean isExactlyOneValue() {
        return (optionCode != null && numericValue == null)
                || (optionCode == null && numericValue != null);
    }
}
