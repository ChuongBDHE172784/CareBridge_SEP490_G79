package com.carebridge.backend.content.dto.response;

import java.time.Instant;
import java.util.UUID;

/** Admin-only provenance projection; consumer checklist payloads never expose it. */
public record ChecklistProvenanceResponse(
        String schema,
        String sourceArtifactPath,
        String sourceArtifactSha256,
        String importBatchId,
        UUID importCorrelationId,
        String normalizerId,
        String copyReviewPolicy,
        String provenanceStatus,
        String cadenceReviewStatus,
        UUID cadenceReviewerUserId,
        Instant cadenceReviewedAt,
        String reviewAuthorityId,
        UUID copyReviewerUserId,
        String qualificationEvidenceRef,
        Instant credentialVerifiedAt,
        UUID contentOwnerUserId,
        Instant contentOwnerApprovedAt,
        Instant copyReviewedAt,
        String sourceTitle,
        String sourceRelationship,
        String sourceOrganization,
        String sourceVersionOrPublicationDate,
        String sourceUrl,
        String sourceLanguage,
        String renderedLanguage,
        String translationProvenance,
        String priorityNarrative,
        String priorityNarrativeMode,
        String sourceLocator,
        String renderedManifestSchema,
        String renderedManifestCanonicalization,
        String renderedManifestHash,
        String validityMode,
        String validUntil,
        String revokedAt) {
}
