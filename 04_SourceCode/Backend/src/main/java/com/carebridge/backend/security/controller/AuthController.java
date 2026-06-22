package com.carebridge.backend.security.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.security.dto.request.LoginRequest;
import com.carebridge.backend.security.dto.request.RefreshTokenRequest;
import com.carebridge.backend.security.dto.request.RegisterRequest;
import com.carebridge.backend.security.dto.request.ResendOtpRequest;
import com.carebridge.backend.security.dto.request.UpdateProfileRequest;
import com.carebridge.backend.security.dto.request.VerifyOtpRequest;
import com.carebridge.backend.security.dto.response.AuthResponse;
import com.carebridge.backend.security.dto.response.RegisterResponse;
import com.carebridge.backend.security.dto.response.OtpResendResponse;
import com.carebridge.backend.security.dto.response.OtpSendResponse;
import com.carebridge.backend.security.dto.response.UserProfileResponse;
import com.carebridge.backend.security.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @PostMapping("/register")
    @Operation(
        summary = "Register new user account",
        description = "Create a new user account with email or phone. The user will receive an OTP for verification. Account remains inactive until OTP is verified.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Registration request containing email/phone, password, and role",
            content = @Content(schema = @Schema(implementation = RegisterRequest.class))
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Registration initiated", content = @Content(schema = @Schema(implementation = RegisterResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request data"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Email or phone already registered")
        }
    )
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Registration initiated"));
    }

    @PostMapping("/login")
    @Operation(
        summary = "User login with credentials",
        description = "Authenticate user with email/phone and password. Returns OTP send confirmation. OTP must be verified to obtain access tokens.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Login credentials",
            content = @Content(schema = @Schema(implementation = LoginRequest.class))
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OTP sent", content = @Content(schema = @Schema(implementation = OtpSendResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid credentials or user not found")
        }
    )
    public ResponseEntity<ApiResponse<OtpSendResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.login(request), "OTP sent"));
    }

    @PostMapping("/verify-otp")
    @Operation(
        summary = "Verify OTP and activate account",
        description = "Verify the 6-digit OTP sent via email or SMS. Upon successful verification, the user account is activated (enabled=true) and access/refresh tokens are returned.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "OTP verification request with phone/email and the 6-digit code",
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
        description = "Revoke the refresh token and invalidate the current session. Access token remains valid until expiry (short-lived).",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Optional refresh token (if null, uses token from Authorization header)",
            content = @Content(schema = @Schema(implementation = RefreshTokenRequest.class))
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Logged out successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
        }
    )
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestBody(required = false) RefreshTokenRequest request,
            Principal principal) {
        String refreshToken = request == null ? null : request.getRefreshToken();
        authService.logout(refreshToken, SecurityUtils.requireCurrentUserId(principal));
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
}
