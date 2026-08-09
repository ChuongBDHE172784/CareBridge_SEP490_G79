package com.carebridge.backend.moderation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.common.exception.BusinessException;
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
import org.springframework.http.HttpStatus;
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

    private static final String MODERATOR_ID = "00000000-0000-0000-0000-0000000000bb";

    /**
     * The read endpoint is only {@code isAuthenticated()} at the controller — the health-record
     * boundary is owned by the service, which admits the owner or an ACCEPTED care-group member and
     * nobody else. A moderator therefore reaches the service *as itself* and is refused there; the
     * moderator authority must never be turned into an elevated caller id along the way.
     */
    @Test
    @WithMockUser(username = MODERATOR_ID, roles = "MODERATOR")
    void moderatorCannotReadAnyHealthRecord() throws Exception {
        UUID recordId = UUID.randomUUID();
        when(healthRecordService.getHealthRecord(any(), any()))
                .thenThrow(new BusinessException(HttpStatus.FORBIDDEN, "HEALTH-004",
                        "Access denied to health record"));

        mockMvc.perform(get("/api/v1/health-records/" + recordId))
                .andExpect(status().isForbidden());

        verify(healthRecordService).getHealthRecord(recordId, UUID.fromString(MODERATOR_ID));
    }

    @Test
    void unauthenticatedReadIsRejectedBeforeReachingTheService() throws Exception {
        mockMvc.perform(get("/api/v1/health-records/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(healthRecordService);
    }
}
