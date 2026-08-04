package com.carebridge.backend.safety;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.safety.controller.SensorSelfTestController;
import com.carebridge.backend.safety.service.impl.SensorSelfTestService;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = SensorSelfTestController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class SensorSelfTestControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private SensorSelfTestService sensorSelfTestService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private UserRepository userRepository;

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000030", roles = "MOTHER")
    void create_validPayloadReturnsCreated() throws Exception {
        mockMvc.perform(post("/api/v1/safety/events/sensor-self-test")
                        .contentType("application/json")
                        .content("""
                                {"testId":"gesture-1","detectedAt":"2026-08-04T10:00:00Z",
                                 "accelerationMagnitude":17.2,"gyroscopeMagnitude":3.4}
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000030", roles = "MOTHER")
    void complete_invalidOutcomeReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/safety/events/{eventId}/sensor-self-test/complete",
                        "00000000-0000-0000-0000-000000000099")
                        .contentType("application/json")
                        .content("{\"outcome\":\"CALL_115\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000030", roles = "PARTNER")
    void create_wrongRoleReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/safety/events/sensor-self-test")
                        .contentType("application/json")
                        .content("""
                                {"testId":"gesture-1","detectedAt":"2026-08-04T10:00:00Z",
                                 "accelerationMagnitude":17.2,"gyroscopeMagnitude":3.4}
                                """))
                .andExpect(status().isForbidden());
    }
}
