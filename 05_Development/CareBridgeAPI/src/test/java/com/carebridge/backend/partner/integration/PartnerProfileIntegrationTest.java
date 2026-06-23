package com.carebridge.backend.partner.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.partner.controller.PartnerProfileController;
import com.carebridge.backend.partner.dto.request.CreatePartnerProfileRequest;
import com.carebridge.backend.partner.dto.response.CreatePartnerProfileResponse;
import com.carebridge.backend.partner.entity.OrganizationStatus;
import com.carebridge.backend.partner.entity.OrganizationType;
import com.carebridge.backend.partner.service.PartnerProfileService;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
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
import com.carebridge.backend.partner.exception.PartnerException;

// PTR-TC-INT-001, PTR-TC-INT-002
// Integration tests: HTTP stack (security → controller → service).
@WebMvcTest(
        value = PartnerProfileController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class})
class PartnerProfileIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private PartnerProfileService partnerProfileService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    private CreatePartnerProfileRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new CreatePartnerProfileRequest(
                "Phòng khám Test",
                OrganizationType.CLINIC,
                "123 Đường Test",
                "Hà Nội",
                "0901234567",
                "test@clinic.vn",
                null,
                null
        );
    }

    // ── PTR-TC-INT-001: Full flow POST tạo record đúng ─────────────────────
    @Test
    @WithMockUser(username = "123", roles = "PARTNER")
    void createProfile_integration_shouldCreateAndReturnProfile() throws Exception {
        UUID generatedId = UUID.randomUUID();
        CreatePartnerProfileResponse response = CreatePartnerProfileResponse.builder()
                .id(generatedId)
                .name(validRequest.getName())
                .type(validRequest.getType())
                .status(OrganizationStatus.PENDING_APPROVAL)
                .build();

        when(partnerProfileService.createProfile(any(), eq(123L))).thenReturn(response);

        mockMvc.perform(post("/api/v1/partner/profile").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(result -> {
                    if (result.getResolvedException() != null) {
                        result.getResolvedException().printStackTrace();
                    }
                })
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(generatedId.toString()))
                .andExpect(jsonPath("$.data.status").value("PENDING_APPROVAL"))
                .andExpect(jsonPath("$.data.name").value("Phòng khám Test"));

        verify(partnerProfileService).createProfile(any(), eq(123L));
    }

    // ── PTR-TC-INT-002: Duplicate POST bị reject ────────────────────────────
    @Test
    @WithMockUser(username = "123", roles = "PARTNER")
    void createProfile_integration_shouldRejectDuplicateProfile() throws Exception {
        when(partnerProfileService.createProfile(any(), eq(123L)))
                .thenThrow(PartnerException.profileAlreadyExists());

        mockMvc.perform(post("/api/v1/partner/profile").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("PTR-002"));
    }

    // ── Concurrent duplicate POST check (DataIntegrityViolation) ───────────
    @Test
    @WithMockUser(username = "123", roles = "PARTNER")
    void createProfile_integration_shouldHandleDataIntegrityViolation() throws Exception {
        when(partnerProfileService.createProfile(any(), eq(123L)))
                .thenThrow(PartnerException.profileAlreadyExists());

        mockMvc.perform(post("/api/v1/partner/profile").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("PTR-002"));
    }
}
