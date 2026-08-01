package com.carebridge.backend.integration.gemini.dto;

import com.carebridge.backend.content.entity.ContentStage;
import java.util.UUID;

public record RagAudienceContext(UUID callerId, boolean mother, ContentStage triageStage) {

    /** Public RAG callers retain the existing generic-role behavior. */
    public RagAudienceContext(UUID callerId, boolean mother) {
        this(callerId, mother, null);
    }
}
