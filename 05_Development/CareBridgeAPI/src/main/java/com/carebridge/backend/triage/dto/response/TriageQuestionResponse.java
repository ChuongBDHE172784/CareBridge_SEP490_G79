package com.carebridge.backend.triage.dto.response;

import java.util.List;

/** User-facing content for one canonical planned question. */
public record TriageQuestionResponse(
        String questionId,
        String text,
        String answerType,
        List<TriageQuestionOptionResponse> options) {
}
