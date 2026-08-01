package com.carebridge.backend.directchat.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendDirectMessageRequest {

    @NotNull
    private UUID clientMessageId;

    private String messageBody;

    private String messageType;

    private UUID attachmentId;
}
