package com.carebridge.backend.moderation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.health.controller.HealthRecordController;
import com.carebridge.backend.health.service.IHealthRecordService;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** BR-MODERATION-02: moderation authority must never grant health-record access. */
@WebMvcTest(value = HealthRecordController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class))
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class ModeratorHealthPrivacySecurityTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean IHealthRecordService healthRecordService;
    @MockitoBean JwtTokenProvider jwtTokenProvider;
    @MockitoBean UserRepository userRepository;

    @Test
    @WithMockUser(username = "moderator-id", roles = "MODERATOR")
    void moderatorCannotReadAnyHealthRecord() throws Exception {
        mockMvc.perform(get("/api/v1/health-records/" + UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }
}
