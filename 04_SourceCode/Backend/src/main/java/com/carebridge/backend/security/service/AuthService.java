package com.carebridge.backend.security.service;

import com.carebridge.backend.security.dto.request.LoginRequest;
import com.carebridge.backend.security.dto.request.RefreshTokenRequest;
import com.carebridge.backend.security.dto.request.RegisterRequest;
import com.carebridge.backend.security.dto.request.UpdateProfileRequest;
import com.carebridge.backend.security.dto.request.VerifyOtpRequest;
import com.carebridge.backend.security.dto.response.AuthResponse;
import com.carebridge.backend.security.dto.response.OtpSendResponse;
import com.carebridge.backend.security.dto.response.UserProfileResponse;
import java.util.UUID;

public interface AuthService {

    OtpSendResponse register(RegisterRequest request);

    OtpSendResponse login(LoginRequest request);

    AuthResponse verifyOtp(VerifyOtpRequest request);

    AuthResponse refresh(RefreshTokenRequest request);

    void logout(String refreshToken, UUID userId);

    UserProfileResponse getProfile(UUID userId);

    UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request);
}
