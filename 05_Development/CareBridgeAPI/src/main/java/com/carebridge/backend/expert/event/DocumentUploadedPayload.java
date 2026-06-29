package com.carebridge.backend.expert.event;

import java.util.UUID;

public record DocumentUploadedPayload(
    UUID documentId,
    UUID expertId,
    UUID accountId) {
}
