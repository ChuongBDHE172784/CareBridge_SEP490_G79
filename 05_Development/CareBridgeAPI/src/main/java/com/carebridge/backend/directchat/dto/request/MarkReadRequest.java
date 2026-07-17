package com.carebridge.backend.directchat.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

// ADR-MEDI-003: lastSeenMessageId is mandatory — no implicit "mark everything as read up to now()"
// fallback (that's exactly the race the client-supplied-cursor design fixes, see TDS §6.5).
@Getter
@Setter
public class MarkReadRequest {

    @NotNull
    private UUID lastSeenMessageId;
}
