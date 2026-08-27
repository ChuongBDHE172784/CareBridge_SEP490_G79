package com.carebridge.backend.exercise;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.exercise.controller.AdminPostureConfigController;
import com.carebridge.backend.exercise.service.IPostureConfigService;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
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

@WebMvcTest(
        value = AdminPostureConfigController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class AdminPostureConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IPostureConfigService postureConfigService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    private static final UUID EXERCISE_ID = UUID.randomUUID();

    // === PAC-TC-THRESH-006 ===
    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    @DisplayName("PAC-TC-THRESH-006: confidenceThreshold=1.5 via POST returns 400 PAC-002, service never invoked")
    void createConfig_thresholdAboveOne_returns400() throws Exception {
        String body = """
                {"exerciseId":"%s","analysisMode":"MODEL_BASED","confidenceThreshold":1.5,"feedbackLevel":"DETAILED"}
                """.formatted(EXERCISE_ID);

        mockMvc.perform(post("/api/v1/admin/posture-configs")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(postureConfigService);
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    @DisplayName("PAC-TC-THRESH: confidenceThreshold=-0.01 via POST returns 400, service never invoked")
    void createConfig_thresholdBelowZero_returns400() throws Exception {
        String body = """
                {"exerciseId":"%s","analysisMode":"MODEL_BASED","confidenceThreshold":-0.01,"feedbackLevel":"DETAILED"}
                """.formatted(EXERCISE_ID);

        mockMvc.perform(post("/api/v1/admin/posture-configs")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(postureConfigService);
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    @DisplayName("PAC-TC-THRESH: confidenceThreshold missing via POST returns 400")
    void createConfig_thresholdMissing_returns400() throws Exception {
        String body = """
                {"exerciseId":"%s","analysisMode":"MODEL_BASED","feedbackLevel":"DETAILED"}
                """.formatted(EXERCISE_ID);

        mockMvc.perform(post("/api/v1/admin/posture-configs")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }
}
