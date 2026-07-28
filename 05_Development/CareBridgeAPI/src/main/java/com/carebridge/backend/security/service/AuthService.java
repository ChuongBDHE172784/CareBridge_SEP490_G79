package com.carebridge.backend.security.service;

import com.carebridge.backend.security.dto.request.ChangePasswordRequest;
import com.carebridge.backend.security.dto.request.LoginRequest;
import com.carebridge.backend.security.dto.request.RefreshTokenRequest;
import com.carebridge.backend.security.dto.request.RegisterRequest;
import com.carebridge.backend.security.dto.request.ResendOtpRequest;
import com.carebridge.backend.security.dto.request.SelectRoleRequest;
import com.carebridge.backend.security.dto.request.UpdateProfileRequest;
import com.carebridge.backend.security.dto.request.VerifyOtpRequest;
import com.carebridge.backend.security.dto.response.AuthResponse;
import com.carebridge.backend.security.dto.response.OtpResendResponse;
import com.carebridge.backend.security.dto.response.OtpSendResponse;
import com.carebridge.backend.security.dto.response.UserProfileResponse;

public interface AuthService {

    OtpSendResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse verifyOtp(VerifyOtpRequest request);

    OtpResendResponse resendOtp(ResendOtpRequest request);

    AuthResponse refresh(RefreshTokenRequest request);

    void logout(String refreshToken, java.util.UUID userId);

    UserProfileResponse getProfile(java.util.UUID userId);

    UserProfileResponse updateProfile(java.util.UUID userId, UpdateProfileRequest request);

    UserProfileResponse selectRole(java.util.UUID userId, SelectRoleRequest request);

    void changePassword(java.util.UUID userId, ChangePasswordRequest request);

    void deactivate(java.util.UUID userId, String confirmPassword);
}
