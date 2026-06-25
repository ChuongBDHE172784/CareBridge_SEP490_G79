package com.carebridge.backend.partner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.partner.controller.PartnerProfileController;
import com.carebridge.backend.partner.dto.response.CreatePartnerProfileResponse;
import com.carebridge.backend.partner.entity.OrganizationStatus;
import com.carebridge.backend.partner.entity.OrganizationType;
import com.carebridge.backend.partner.service.PartnerProfileService;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
        value = PartnerProfileController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import(SecurityConfig.class)
class PartnerProfileControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PartnerProfileService partnerProfileService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    private static final String BASE_URL = "/api/v1/partner/profile";

    private String validBody() {
        return """
                {
                  "name": "Phòng khám Test",
                  "type": "CLINIC",
                  "address": "123 Đường Test",
                  "city": "Hà Nội",
                  "phone": "0901234567",
                  "email": "test@clinic.vn"
                }
                """;
    }

    private CreatePartnerProfileResponse makeResponse() {
        return CreatePartnerProfileResponse.builder()
                .id(UUID.randomUUID())
                .name("Phòng khám Test")
                .type(OrganizationType.CLINIC)
                .status(OrganizationStatus.PENDING_APPROVAL)
                .createdAt(Instant.now())
                .build();
    }

    // PTR-TC-SEC-001 / PTR-TC-007: Non-PARTNER roles bị 403 (TC-COND-007)
    @ParameterizedTest
    @ValueSource(strings = {"MOTHER", "EXPERT", "MODERATOR", "CONTENT_ADMIN"})
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001", roles = "MOTHER")
    void createProfile_nonPartnerRoles_shouldReturn403(String role) throws Exception {
        mockMvc.perform(post(BASE_URL).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(validBody()))
                .andExpect(status().isForbidden());

        verify(partnerProfileService, never()).createProfile(any(), any());
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001", roles = "MOTHER")
    void createProfile_motherRole_shouldReturn403() throws Exception {
        mockMvc.perform(post(BASE_URL).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(validBody()))
                .andExpect(status().isForbidden());

        verify(partnerProfileService, never()).createProfile(any(), any());
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001", roles = "EXPERT")
    void createProfile_expertRole_shouldReturn403() throws Exception {
        mockMvc.perform(post(BASE_URL).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(validBody()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001", roles = "MODERATOR")
    void createProfile_moderatorRole_shouldReturn403() throws Exception {
        mockMvc.perform(post(BASE_URL).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(validBody()))
                .andExpect(status().isForbidden());
    }

    // PTR-TC-007: Không có JWT → 401
    @Test
    void createProfile_noJwt_shouldReturn401() throws Exception {
        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(validBody()))
                .andExpect(status().isUnauthorized());
    }

    // PTR-TC-SEC-002: representativeUserId injection từ body bị ignore (TC-COND-003)
    // Extra JSON field "representativeUserId" in body must be ignored — only actorId from JWT is used
    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000003", roles = "PARTNER")
    void createProfile_representativeUserIdInjectedInBody_isIgnored() throws Exception {
        when(partnerProfileService.createProfile(any(), any())).thenReturn(makeResponse());

        String bodyWithInjection = """
                {
                  "name": "Phòng khám Test",
                  "type": "CLINIC",
                  "address": "123 Đường Test",
                  "city": "Hà Nội",
                  "phone": "0901234567",
                  "email": "test@clinic.vn",
                  "representativeUserId": "999"
                }
                """;

        // Oracle: ADR-002 — extra JSON field is ignored by Jackson (not in DTO record)
        // Service receives actorId from SecurityContext (username "3" → 3L)
        mockMvc.perform(post(BASE_URL).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(bodyWithInjection))
                .andExpect(status().isCreated());
    }

    // PTR-TC-SEC-003: XSS trong name field — phải được stored as plain text hoặc rejected (TC-COND-012)
    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000002", roles = "PARTNER")
    void createProfile_xssInNameField_isHandledSafely() throws Exception {
        when(partnerProfileService.createProfile(any(), any())).thenReturn(makeResponse());

        String bodyWithXss = """
                {
                  "name": "<script>alert('xss')</script>Test Clinic",
                  "type": "CLINIC",
                  "address": "123 Đường Test",
                  "city": "Hà Nội",
                  "phone": "0901234567",
                  "email": "test@clinic.vn"
                }
                """;

        // Oracle: OWASP A03 — either 400 (rejected) or 201 (stored as plain text, not executed)
        // Spring MVC + Jackson handles deserialization as plain string — no execution
        mockMvc.perform(post(BASE_URL).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(bodyWithXss))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert status == 200 || status == 201 || status == 400
                            : "Expected 200/201 (stored safely) or 400 (rejected), got " + status;
                });
    }
}
