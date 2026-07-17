package com.carebridge.backend.security.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.mockito.Mockito.mock;

import com.carebridge.backend.identity.service.SessionService;
import com.carebridge.backend.security.service.AuthService;
import com.carebridge.backend.security.service.ForgotPasswordService;
import com.carebridge.backend.security.service.ResetPasswordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** RED contract for POST /api/v1/auth/federated. */
class FederatedAuthControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AuthController controller = new AuthController(
                mock(AuthService.class),
                mock(SessionService.class),
                mock(ForgotPasswordService.class),
                mock(ResetPasswordService.class));
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void validFirebaseIdToken_returnsCareBridgeSession() throws Exception {
        mockMvc.perform(post("/api/v1/auth/federated")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"valid-google-token\",\"deviceInfo\":\"MockMvc\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
    }

    @Test
    void invalidFirebaseIdToken_returnsNeutralUnauthorizedResponse() throws Exception {
        mockMvc.perform(post("/api/v1/auth/federated")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"invalid-token\",\"deviceInfo\":\"MockMvc\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Unable to authenticate"));
    }

    @Test
    void firebaseVerifierTimeout_returnsServiceUnavailable() throws Exception {
        mockMvc.perform(post("/api/v1/auth/federated")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"provider-timeout\",\"deviceInfo\":\"MockMvc\"}"))
                .andExpect(status().isServiceUnavailable());
    }
}
