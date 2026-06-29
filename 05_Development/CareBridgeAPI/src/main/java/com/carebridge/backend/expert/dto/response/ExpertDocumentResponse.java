package com.carebridge.backend.expert.dto.response;

import com.carebridge.backend.expert.entity.VerificationDocStatus;
import com.carebridge.backend.expert.entity.VerificationDocType;
import java.time.Instant;
import java.util.UUID;

public record ExpertDocumentResponse(
    UUID id,
    UUID expertId,
    VerificationDocType docType,
    VerificationDocStatus status,
    String storageKey,
    String originalName,
    String mimeType,
    Long sizeBytes,
    Instant uploadedAt) {
}
