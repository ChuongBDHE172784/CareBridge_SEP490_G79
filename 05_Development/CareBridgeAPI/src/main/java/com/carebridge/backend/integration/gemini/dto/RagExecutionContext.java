package com.carebridge.backend.integration.gemini.dto;

import com.carebridge.backend.content.entity.ContentStage;

public record RagExecutionContext(boolean mother, ContentStage canonicalStage, UserStage promptStage) {
}
