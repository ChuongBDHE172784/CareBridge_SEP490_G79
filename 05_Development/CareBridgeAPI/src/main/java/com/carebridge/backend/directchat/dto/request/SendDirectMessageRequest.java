package com.carebridge.backend.directchat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

// BR-DCC-016: no messageType field — every message this pass is TEXT, set server-side.
@Getter
@Setter
public class SendDirectMessageRequest {

    @NotNull
    private UUID clientMessageId;

    @NotBlank
    private String messageBody;
}
