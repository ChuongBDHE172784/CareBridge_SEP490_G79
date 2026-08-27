package com.carebridge.backend.directchat.dto.request;

import com.carebridge.backend.directchat.entity.CallType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InitiateCallRequest {

    @NotNull
    private CallType callType;
}
