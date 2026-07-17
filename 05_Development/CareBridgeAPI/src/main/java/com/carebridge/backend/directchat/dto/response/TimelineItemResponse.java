package com.carebridge.backend.directchat.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

// Flat shape, "kind" discriminator — matches TDS §9.2 sample. Null fields omitted from
// the JSON response rather than modeled as a Jackson polymorphic hierarchy — this stays
// a plain read DTO, not a domain type, so the flat shape is the pragmatic choice.
@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TimelineItemResponse {
    private final String kind; // "MESSAGE" | "CALL_EVENT"

    // MESSAGE fields
    private final UUID messageId;
    private final UUID clientMessageId; // lets the client reconcile an optimistic send against a later timeline refetch
    private final UUID senderUserId;
    private final String messageType;
    private final String messageBody;
    private final Instant createdAt;

    // CALL_EVENT fields
    private final UUID callId;
    private final String callType;
    private final String callStatus;
    private final UUID initiatedByUserId;
    private final Integer durationSeconds;
    private final Instant initiatedAt;
    private final Instant answeredAt;
    private final Instant endedAt;
}
