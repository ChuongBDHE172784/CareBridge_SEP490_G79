package com.carebridge.backend.security.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ForgotPasswordResponse {

    private String message;
    private int expiresIn;
}
