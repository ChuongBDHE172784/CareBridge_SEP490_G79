package com.carebridge.backend.security.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.common.exception.GlobalExceptionHandler;
import com.carebridge.backend.security.dto.response.FederatedAuthResponse;
import com.carebridge.backend.security.dto.response.LinkedGoogleIdentityResponse;
import com.carebridge.backend.security.federation.FederatedProvider;
import com.carebridge.backend.security.exception.FederatedAuthException;
import com.carebridge.backend.identity.service.SessionService;
import com.carebridge.backend.security.service.AuthService;
import com.carebridge.backend.security.service.ForgotPasswordService;
import com.carebridge.backend.security.service.FederatedAuthService;
import com.carebridge.backend.security.service.ResetPasswordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.time.Instant;
import java.security.Principal;
import java.util.UUID;

/** RED contract for POST /api/v1/auth/federated. */
class FederatedAuthControllerTest {

    private MockMvc mockMvc;
    private FederatedAuthService federatedAuthService;

    @BeforeEach
    void setUp() {
        federatedAuthService = mock(FederatedAuthService.class);
        AuthController controller = new AuthController(
                mock(AuthService.class),
                mock(SessionService.class),
                mock(ForgotPasswordService.class),
                mock(ResetPasswordService.class), federatedAuthService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void validFirebaseIdToken_returnsCareBridgeSession() throws Exception {
        when(federatedAuthService.authenticate(any())).thenReturn(FederatedAuthResponse.builder()
                .accessToken("access").refreshToken("refresh").newUser(false).profileCompleted(true).build());
        mockMvc.perform(post("/api/v1/auth/federated")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"valid-google-token\",\"deviceInfo\":\"MockMvc\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
    }

    @Test
    void verifiedPhoneRegistration_returnsCreatedCareBridgeSession() throws Exception {
        when(federatedAuthService.registerPhone(any())).thenReturn(FederatedAuthResponse.builder()
                .accessToken("access").refreshToken("refresh").newUser(true).profileCompleted(false).build());

        mockMvc.perform(post("/api/v1/auth/phone/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idToken":"valid-phone-token","name":"Phone User",
                                 "email":"optional@example.com","phone":"0901111021",
                                 "password":"Strong@123","role":"MOTHER","deviceInfo":"MockMvc"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.newUser").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access"));
    }

    @Test
    void verifiedPhoneLogin_returnsExistingCareBridgeSession() throws Exception {
        when(federatedAuthService.loginPhone(any())).thenReturn(FederatedAuthResponse.builder()
                .accessToken("access").refreshToken("refresh").newUser(false).profileCompleted(true).build());

        mockMvc.perform(post("/api/v1/auth/phone/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"valid-phone-token\",\"deviceInfo\":\"MockMvc\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.newUser").value(false))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh"));
    }

    @Test
    void verifiedPhoneLogin_newPhone_returnsCreatedCareBridgeSession() throws Exception {
        when(federatedAuthService.loginPhone(any())).thenReturn(FederatedAuthResponse.builder()
                .accessToken("access").refreshToken("refresh").newUser(true).profileCompleted(false).build());

        mockMvc.perform(post("/api/v1/auth/phone/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"valid-phone-token\",\"deviceInfo\":\"MockMvc\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.newUser").value(true))
                .andExpect(jsonPath("$.data.profileCompleted").value(false));
    }

    @Test
    void phoneRegistration_missingEmail_isRejectedBeforeService() throws Exception {
        mockMvc.perform(post("/api/v1/auth/phone/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idToken":"valid-phone-token","name":"Phone User",
                                 "phone":"0901111021","password":"Strong@123"}
                                """))
                .andExpect(status().isBadRequest());

        verify(federatedAuthService, never()).registerPhone(any());
    }

    @Test
    void phoneRegistration_invalidPhonePayload_isRejectedBeforeService() throws Exception {
        mockMvc.perform(post("/api/v1/auth/phone/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idToken":"valid-phone-token","name":"Phone User",
                                 "phone":"+14155552671","password":"Strong@123"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidFirebaseIdToken_returnsNeutralUnauthorizedResponse() throws Exception {
        when(federatedAuthService.authenticate(any())).thenThrow(FederatedAuthException.invalidProof());
        mockMvc.perform(post("/api/v1/auth/federated")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"invalid-token\",\"deviceInfo\":\"MockMvc\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Unable to authenticate"));
    }

    @Test
    void firebaseVerifierTimeout_returnsServiceUnavailable() throws Exception {
        when(federatedAuthService.authenticate(any())).thenThrow(FederatedAuthException.unavailable());
        mockMvc.perform(post("/api/v1/auth/federated")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"provider-timeout\",\"deviceInfo\":\"MockMvc\"}"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void getGoogleIdentity_authenticatedUser_returnsLinkStatus() throws Exception {
        UUID userId = UUID.randomUUID();
        when(federatedAuthService.getGoogleIdentity(userId))
                .thenReturn(new LinkedGoogleIdentityResponse(
                        FederatedProvider.GOOGLE, false, null, null));

        mockMvc.perform(get("/api/v1/auth/identities/google")
                        .principal(principal(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.provider").value("GOOGLE"))
                .andExpect(jsonPath("$.data.linked").value(false));
    }

    @Test
    void linkGoogleIdentity_authenticatedUser_returnsLinkedIdentityWithoutSessionTokens() throws Exception {
        UUID userId = UUID.randomUUID();
        when(federatedAuthService.linkGoogleIdentity(any(), any()))
                .thenReturn(new LinkedGoogleIdentityResponse(
                        FederatedProvider.GOOGLE, true, "google@example.com", Instant.now()));

        mockMvc.perform(post("/api/v1/auth/identities/google")
                        .principal(principal(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"fresh-google-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.linked").value(true))
                .andExpect(jsonPath("$.data.email").value("google@example.com"))
                .andExpect(jsonPath("$.data.accessToken").doesNotExist())
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist());
    }

    @Test
    void getGoogleIdentity_anonymousCaller_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/auth/identities/google"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void linkGoogleIdentity_anonymousCaller_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/auth/identities/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"fresh-google-token\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void linkGoogleIdentity_blankToken_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/auth/identities/google")
                        .principal(principal(UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void linkGoogleIdentity_oversizedToken_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/auth/identities/google")
                        .principal(principal(UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"" + "x".repeat(8193) + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void linkGoogleIdentity_subjectConflict_returnsNeutralConflictCode() throws Exception {
        when(federatedAuthService.linkGoogleIdentity(any(), any()))
                .thenThrow(FederatedAuthException.identityOwnedByAnotherUser());

        mockMvc.perform(post("/api/v1/auth/identities/google")
                        .principal(principal(UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"owned-google-token\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("AUTH-FED-006"))
                .andExpect(jsonPath("$.message").value("This Google account cannot be linked"));
    }

    private Principal principal(UUID userId) {
        return userId::toString;
    }
}
