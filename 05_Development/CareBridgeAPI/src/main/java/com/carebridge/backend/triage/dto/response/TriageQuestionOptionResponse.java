package com.carebridge.backend.triage.dto.response;

/** Stable machine code plus the Vietnamese label authored in the canonical catalogue. */
public record TriageQuestionOptionResponse(String optionCode, String displayText) {
}
