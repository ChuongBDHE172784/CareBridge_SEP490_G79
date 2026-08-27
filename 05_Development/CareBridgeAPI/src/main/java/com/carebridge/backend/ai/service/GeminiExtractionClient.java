package com.carebridge.backend.ai.service;

public interface GeminiExtractionClient {

    record ExtractionResult(
            String symptomListJson,
            Integer durationDays,
            String intensity,
            boolean emergencyFlag
    ) {}

    ExtractionResult extractStructuredData(String constrainedPrompt);
}
