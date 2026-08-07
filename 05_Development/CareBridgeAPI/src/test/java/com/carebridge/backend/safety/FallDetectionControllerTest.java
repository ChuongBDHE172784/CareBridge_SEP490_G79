package com.carebridge.backend.safety;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.safety.controller.FallDetectionController;
import com.carebridge.backend.safety.dto.response.ImuMonitoringSessionResponse;
import com.carebridge.backend.safety.dto.response.SafetyConfigResponse;
import com.carebridge.backend.safety.service.IFallDetectionService;
import com.carebridge.backend.safety.service.ISafetyConfigService;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = FallDetectionController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class FallDetectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IFallDetectionService fallDetectionService;

    @MockitoBean
    private ISafetyConfigService safetyConfigService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000020", roles = "OPERATIONS")
    void disable_wrongRole_shouldReturn403() throws Exception {
        // DIS-TC-006
        mockMvc.perform(post("/api/v1/safety/fall-detection/disable"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000030", roles = "MOTHER")
    void enable_shouldUseSensitivityLevelFromSafetyConfig() throws Exception {
        // FD-TC-006 — C4: sensitivityLevel must come from SafetyConfig, not hardcoded
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000030");
        when(safetyConfigService.getConfig(userId))
                .thenReturn(SafetyConfigResponse.builder()
                        .userId(userId)
                        .fallDetectionEnabled(true)
                        .sensitivityLevel("HIGH")
                        .emergencyAutoAlert(true)
                        .build());
        when(fallDetectionService.enable(userId, "HIGH"))
                .thenReturn(ImuMonitoringSessionResponse.builder()
                        .sessionId(UUID.randomUUID())
                        .userId(userId)
                        .status("ACTIVE")
                        .sensitivityLevel("HIGH")
                        .build());

        mockMvc.perform(post("/api/v1/safety/fall-detection/enable"))
                .andExpect(status().isCreated());

        verify(fallDetectionService).enable(eq(userId), eq("HIGH"));
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000030", roles = "MOTHER")
    void listSafetyEvents_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/v1/safety/events"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000030", roles = "MOTHER")
    void confirmSafetyCheck_shouldReturn200() throws Exception {
        UUID eventId = UUID.fromString("00000000-0000-0000-0000-000000000099");

        mockMvc.perform(post("/api/v1/safety/events/{id}/confirm", eventId)
                        .contentType("application/json")
                        .content("{\"note\":\"Safe\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000030", roles = "MOTHER")
    void reportFalsePositive_shouldReturn200() throws Exception {
        UUID eventId = UUID.fromString("00000000-0000-0000-0000-000000000099");

        mockMvc.perform(post("/api/v1/safety/events/{id}/false-positive", eventId)
                        .contentType("application/json")
                        .content("{\"note\":\"Phone dropped\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000030", roles = "MOTHER")
    void reportFalsePositive_noteLongerThanResponseContract_shouldReturn400() throws Exception {
        UUID eventId = UUID.fromString("00000000-0000-0000-0000-000000000099");
        String oversizedNote = "x".repeat(501);

        mockMvc.perform(post("/api/v1/safety/events/{id}/false-positive", eventId)
                        .contentType("application/json")
                        .content("{\"note\":\"" + oversizedNote + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000030", roles = "MOTHER")
    void sendEmergencyAlert_shouldReturn202() throws Exception {
        UUID eventId = UUID.fromString("00000000-0000-0000-0000-000000000099");

        mockMvc.perform(post("/api/v1/safety/events/{id}/emergency-alert", eventId))
                .andExpect(status().isAccepted());
    }
}
