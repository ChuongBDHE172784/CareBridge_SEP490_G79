package com.carebridge.backend.security.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.security.dto.request.LoginRequest;
import com.carebridge.backend.security.dto.response.AuthResponse;
import com.carebridge.backend.security.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Explicitly opt-in credential login used by local and test fixtures.
 *
 * <p>The bean is absent unless the property is set to true, so non-local runtimes do not
 * register an OTP-bypass route. The default is fail-closed in {@code application.yaml}.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Profile("(dev | test) & !prod")
@ConditionalOnProperty(
        prefix = "carebridge.auth",
        name = "login-direct-enabled",
        havingValue = "true")
@Tag(name = "Authentication", description = "Local/test-only direct authentication")
public class DirectLoginController {

    private final AuthService authService;

    @PostMapping("/login-direct")
    @Operation(summary = "Direct login without OTP (explicit local/test opt-in only)")
    public ResponseEntity<ApiResponse<AuthResponse>> loginDirect(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(authService.loginDirect(request), "Login successful"));
    }
}
