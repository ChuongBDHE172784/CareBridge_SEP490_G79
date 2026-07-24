package com.carebridge.backend.integration.gemini.dto;

import java.util.UUID;

public record RagAudienceContext(UUID callerId, boolean mother) {
}
