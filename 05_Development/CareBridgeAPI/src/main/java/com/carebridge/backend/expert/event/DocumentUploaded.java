package com.carebridge.backend.expert.event;

import java.util.UUID;

public record DocumentUploaded(
    UUID eventId,
    String eventType,
    java.time.Instant occurredAt,
    String version,
    DocumentUploadedPayload payload,
    java.util.Map<String, String> metadata) {

  public static DocumentUploaded of(
      UUID documentId, UUID expertId, UUID accountId) {
    return new DocumentUploaded(
        UUID.randomUUID(),
        "DocumentUploaded",
        java.time.Instant.now(),
        "1.0",
        new DocumentUploadedPayload(documentId, expertId, accountId),
        java.util.Map.of());
  }
}
