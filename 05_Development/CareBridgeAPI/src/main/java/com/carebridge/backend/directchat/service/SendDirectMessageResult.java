package com.carebridge.backend.directchat.service;

import com.carebridge.backend.directchat.dto.response.TimelineItemResponse;

// created=false on idempotent retry (200), true on first insert (201) — mirrors the
// pattern already proven for consultation chat idempotency in the prior UC-144 pass.
public record SendDirectMessageResult(TimelineItemResponse message, boolean created) {
}
