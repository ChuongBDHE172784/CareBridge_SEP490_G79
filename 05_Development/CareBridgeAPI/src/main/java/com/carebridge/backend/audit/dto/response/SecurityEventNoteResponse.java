package com.carebridge.backend.audit.dto.response;

import java.time.Instant;
import java.util.UUID;

public record SecurityEventNoteResponse(
    UUID noteId,
    Long eventId,
    UUID authorId,
    String noteText,
    Instant createdAt
) {}
