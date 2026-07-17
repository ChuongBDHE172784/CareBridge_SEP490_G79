package com.carebridge.backend.directchat.dto.response;

import java.time.Instant;
import java.util.UUID;

// ADR-MEDI-003 §9.4 — cursorAt == resolvedMessage.createdAt, never the server's now().
public record MarkReadResponse(Instant cursorAt, UUID cursorMessageId) {
}
