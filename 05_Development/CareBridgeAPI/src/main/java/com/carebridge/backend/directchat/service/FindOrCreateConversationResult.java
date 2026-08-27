package com.carebridge.backend.directchat.service;

import com.carebridge.backend.directchat.dto.response.DirectConversationResponse;

// created=false when an existing conversation was reused (200), true when a new row was
// inserted (201) — same idempotent-response pattern as SendDirectMessageResult.
public record FindOrCreateConversationResult(DirectConversationResponse conversation, boolean created) {
}
