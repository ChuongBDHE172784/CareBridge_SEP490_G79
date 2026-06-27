package com.carebridge.backend.security.service;

import com.carebridge.backend.security.dto.request.ResetPasswordRequest;
import com.carebridge.backend.security.dto.response.ResetPasswordResponse;

public interface ResetPasswordService {

    ResetPasswordResponse resetPassword(ResetPasswordRequest request);
}
