package com.carebridge.backend.security.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.identity.service.SessionService;
import com.carebridge.backend.security.dto.request.ChangePasswordRequest;
import com.carebridge.backend.security.dto.request.DeactivateRequest;
import com.carebridge.backend.security.dto.request.ForgotPasswordRequest;
import com.carebridge.backend.security.dto.request.LoginRequest;
import com.carebridge.backend.security.dto.request.RefreshTokenRequest;
import com.carebridge.backend.security.dto.request.RegisterRequest;
import com.carebridge.backend.security.dto.request.ResendOtpRequest;
import com.carebridge.backend.security.dto.request.ResetPasswordRequest;
import com.carebridge.backend.security.dto.request.UpdateProfileRequest;
import com.carebridge.backend.security.dto.request.VerifyOtpRequest;
import com.carebridge.backend.security.dto.response.AuthResponse;
import com.carebridge.backend.security.dto.response.ForgotPasswordResponse;
import com.carebridge.backend.security.dto.response.OtpResendResponse;
import com.carebridge.backend.security.dto.response.OtpSendResponse;
import com.carebridge.backend.security.dto.response.ResetPasswordResponse;
import com.carebridge.backend.security.dto.response.UserProfileResponse;
import com.carebridge.backend.security.service.AuthService;
import com.carebridge.backend.security.service.ForgotPasswordService;
import com.carebridge.backend.security.service.ResetPasswordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication endpoints for registration, login, OTP verification, and user profile management")
public class AuthController {

    private final AuthService authService;
    private final SessionService sessionService;
    private final ForgotPasswordService forgotPasswordService;
    private final ResetPasswordService resetPasswordService;

    @PostMapping("/register")
    @Operation(
        summary = "Register new user account",
        description = "Create a new user account with email or phone. The user will receive an OTP for verification. Account remains inactive until OTP is verified.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Registration request containing email/phone, password, and role",
            content = @Content(schema = @Schema(implementation = RegisterRequest.class))
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Registration initiated, OTP sent", content = @Content(schema = @Schema(implementation = OtpSendResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request data"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Email or phone already registered")
        }
    )
    public ResponseEntity<ApiResponse<OtpSendResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(authService.register(request), "OTP sent"));
    }

    @PostMapping("/login")
    @Operation(
        summary = "User login with credentials",
        description = "Authenticate user with email/phone and password. Sends an OTP code which must be verified to complete authentication. Rate limited: max 5 attempts per 15 minutes per account. Account will be temporarily locked for 15 minutes after exceeding attempts.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Login credentials",
            content = @Content(schema = @Schema(implementation = LoginRequest.class))
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OTP sent successfully", content = @Content(schema = @Schema(implementation = OtpSendResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid credentials or user not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Account is disabled or locked"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "Account temporarily locked due to multiple failed attempts")
        }
    )
    public ResponseEntity<ApiResponse<OtpSendResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.login(request), "OTP sent"));
    }

    @PostMapping("/login-direct")
    @Operation(summary = "Direct login without OTP (dev/test only)")
    public ResponseEntity<ApiResponse<AuthResponse>> loginDirect(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.loginDirect(request), "Login successful"));
    }

    @PostMapping("/verify-otp")
    @Operation(
        summary = "Verify OTP and complete login/registration",
        description = "Verify the 6-digit OTP sent via email or SMS. Upon successful verification, the user account is activated (enabled=true), a new session is created, and access/refresh tokens are returned.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "OTP verification request",
            content = @Content(schema = @Schema(implementation = VerifyOtpRequest.class))
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OTP verified successfully", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid, expired OTP or verification attempts exceeded"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found or no pending OTP")
        }
    )
    public ResponseEntity<ApiResponse<AuthResponse>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.verifyOtp(request), "OTP verified"));
    }

    @PostMapping("/resend-otp")
    @Operation(
        summary = "Resend OTP verification code",
        description = "Request a new OTP to be sent via email and/or SMS. Rate limited: max 1 request per 60 seconds per identifier (phone/email). Invalidates previous OTPs.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Resend request with phone or email identifier",
            content = @Content(schema = @Schema(implementation = ResendOtpRequest.class))
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OTP resent successfully", content = @Content(schema = @Schema(implementation = OtpResendResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "No identifier provided, no pending OTP, or cooldown not elapsed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "Resend cooldown period not elapsed")
        }
    )
    public ResponseEntity<ApiResponse<OtpResendResponse>> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.resendOtp(request), "OTP resent successfully"));
    }

    @PostMapping("/refresh")
    @Operation(
        summary = "Refresh access token",
        description = "Exchange a valid refresh token for a new access token. Refresh token must be associated with an active user session.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Refresh token",
            content = @Content(schema = @Schema(implementation = RefreshTokenRequest.class))
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Token refreshed", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid refresh token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Refresh token revoked or expired")
        }
    )
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.refresh(request), "Token refreshed"));
    }

    @PostMapping("/logout")
    @Operation(
        summary = "User logout",
        description = "Revoke the refresh token and invalidate the current session. If no refresh token is provided, logs out the current session using the JWT sid claim. Access token remains valid until expiry (short-lived).",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Optional refresh token (if null, uses authenticated session's sid claim)",
            content = @Content(schema = @Schema(implementation = RefreshTokenRequest.class))
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Logged out successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
        }
    )
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestBody(required = false) RefreshTokenRequest request,
            Principal principal,
            HttpServletRequest httpRequest) {
        String refreshToken = (request != null) ? request.getRefreshToken() : null;
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        String ipAddress = httpRequest.getRemoteAddr();
        sessionService.logout(refreshToken, userId, ipAddress);
        return ResponseEntity.ok(ApiResponse.success(null, "Logged out"));
    }

    @GetMapping("/profile")
    @Operation(
        summary = "Get current user profile",
        description = "Retrieve the authenticated user's profile information including role, email, phone, and account status. Requires valid access token in Authorization header.",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile retrieved", content = @Content(schema = @Schema(implementation = UserProfileResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
        }
    )
    public ResponseEntity<ApiResponse<UserProfileResponse>> profile(Principal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                authService.getProfile(SecurityUtils.requireCurrentUserId(principal))));
    }

    @PutMapping("/profile")
    @Operation(
        summary = "Update current user profile",
        description = "Update user profile information such as phone number. Email changes require re-verification. Requires valid access token in Authorization header.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Profile update data",
            content = @Content(schema = @Schema(implementation = UpdateProfileRequest.class))
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile updated", content = @Content(schema = @Schema(implementation = UserProfileResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid update data"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
        }
    )
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            Principal principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                authService.updateProfile(SecurityUtils.requireCurrentUserId(principal), request),
                "Profile updated"));
    }

    @PostMapping("/forgot-password")
    @Operation(
        summary = "Request password reset",
        description = "Send a password reset link to the user's email or phone. Always returns 200 to prevent account enumeration. Rate limited: 3 requests per hour per contact.",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Reset instruction sent (or silently skipped if account not found)", content = @Content(schema = @Schema(implementation = ForgotPasswordResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid contact format"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "Rate limit exceeded")
        }
    )
    public ResponseEntity<ApiResponse<ForgotPasswordResponse>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            jakarta.servlet.http.HttpServletRequest httpRequest) {
        String ipAddress = httpRequest.getRemoteAddr();
        return ResponseEntity.ok(ApiResponse.success(
                forgotPasswordService.forgotPassword(request, ipAddress)));
    }

    @PostMapping("/reset-password")
    @Operation(
        summary = "Reset password using token",
        description = "Set a new password using the reset token received via email or SMS. Token is single-use and expires after 15 minutes.",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Password reset successfully", content = @Content(schema = @Schema(implementation = ResetPasswordResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid token, password mismatch, or weak password"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "Too many attempts for this token")
        }
    )
    public ResponseEntity<ApiResponse<ResetPasswordResponse>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(ApiResponse.success(resetPasswordService.resetPassword(request)));
    }

    @PutMapping("/change-password")
    @Operation(
        summary = "Change password (authenticated)",
        description = "Change the current user's password. Requires valid JWT. All active sessions are invalidated after a successful change.",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Password changed successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Wrong current password, mismatch, or weak new password"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
        }
    )
    public ResponseEntity<ApiResponse<Void>> changePassword(
            Principal principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(SecurityUtils.requireCurrentUserId(principal), request);
        return ResponseEntity.ok(ApiResponse.success(null, "Password changed successfully. Please log in again."));
    }

    @DeleteMapping("/deactivate")
    @Operation(
        summary = "Deactivate own account",
        description = "Permanently deactivate the authenticated user's account. Requires password confirmation. All sessions and device tokens are revoked immediately.",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Account deactivated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Incorrect password or account already deactivated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "SYSTEM_ADMIN cannot self-deactivate")
        }
    )
    public ResponseEntity<ApiResponse<Void>> deactivate(
            Principal principal,
            @Valid @RequestBody DeactivateRequest request) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        authService.deactivate(userId, request.getConfirmPassword());
        return ResponseEntity.ok(ApiResponse.success(null, "Account deactivated successfully"));
    }
}
