package com.carebridge.backend.security.service;

import com.carebridge.backend.security.dto.request.ForgotPasswordRequest;
import com.carebridge.backend.security.dto.response.ForgotPasswordResponse;
import com.carebridge.backend.security.entity.User;

public interface ForgotPasswordService {

    ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request, String ipAddress);

    User validateToken(String token);

    void consumeToken(String token);
}
