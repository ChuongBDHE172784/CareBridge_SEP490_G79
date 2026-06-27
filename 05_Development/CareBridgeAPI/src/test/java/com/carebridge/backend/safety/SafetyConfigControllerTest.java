package com.carebridge.backend.safety;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.safety.controller.SafetyConfigController;
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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = SafetyConfigController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class SafetyConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ISafetyConfigService safetyConfigService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    private static final String BASE_URL = "/api/v1/safety/config";

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000010", roles = "MOTHER")
    void configure_invalidSensitivityLevel_shouldReturn400() throws Exception {
        // SCONFIG-TC-005
        mockMvc.perform(put(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fallDetectionEnabled\":true,\"sensitivityLevel\":\"INVALID\",\"emergencyAutoAlert\":true}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000020", roles = "PARTNER")
    void configure_wrongRole_shouldReturn403() throws Exception {
        // SCONFIG-TC-006
        mockMvc.perform(put(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fallDetectionEnabled\":true,\"sensitivityLevel\":\"MEDIUM\",\"emergencyAutoAlert\":true}"))
                .andExpect(status().isForbidden());
    }
}
