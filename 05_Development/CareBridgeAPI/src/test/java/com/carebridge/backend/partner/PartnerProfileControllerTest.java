package com.carebridge.backend.partner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.partner.controller.PartnerProfileController;
import com.carebridge.backend.partner.dto.response.CreatePartnerProfileResponse;
import com.carebridge.backend.partner.entity.OrganizationStatus;
import com.carebridge.backend.partner.entity.OrganizationType;
import com.carebridge.backend.partner.service.PartnerProfileService;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
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
class PartnerProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PartnerProfileService partnerProfileService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    private static final String BASE_URL = "/api/v1/partner/profile";

    private CreatePartnerProfileResponse makeResponse(UUID id) {
        return CreatePartnerProfileResponse.builder()
                .id(id)
                .name("Phòng khám Test")
                .type(OrganizationType.CLINIC)
                .status(OrganizationStatus.PENDING_APPROVAL)
                .createdAt(Instant.now())
                .build();
    }

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

    // PTR-TC-001: Happy path — PARTNER role creates profile → 201 (TC-COND-001)
    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000002", roles = "PARTNER")
    void createProfile_asPartnerRole_validRequest_shouldReturn201() throws Exception {
        UUID newId = UUID.randomUUID();
        when(partnerProfileService.createProfile(any(), eq(UUID.fromString("00000000-0000-0000-0000-000000000002")))).thenReturn(makeResponse(newId));

        mockMvc.perform(post(BASE_URL).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(validBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PENDING_APPROVAL"))
                .andExpect(jsonPath("$.data.type").value("CLINIC"))
                .andExpect(jsonPath("$.data.name").value("Phòng khám Test"));
    }

    // PTR-TC-006: name rỗng → 400 validation error (TC-COND-006)
    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000002", roles = "PARTNER")
    void createProfile_emptyName_shouldReturn400() throws Exception {
        mockMvc.perform(post(BASE_URL).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"","type":"CLINIC","address":"A","city":"HN","phone":"0901234567","email":"t@t.vn"}
                        """))
                .andExpect(status().isBadRequest());

        verify(partnerProfileService, never()).createProfile(any(), any());
    }

    // PTR-TC-006: type invalid → 400
    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000002", roles = "PARTNER")
    void createProfile_invalidType_shouldReturn400() throws Exception {
        mockMvc.perform(post(BASE_URL).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Test","type":"INVALID","address":"A","city":"HN","phone":"0901234567","email":"t@t.vn"}
                        """))
                .andExpect(status().isBadRequest());

        verify(partnerProfileService, never()).createProfile(any(), any());
    }

    // PTR-TC-006: phone validation — invalid phones reject with 400 (TC-COND-006)
    @ParameterizedTest
    @ValueSource(strings = {"01234567", "abc123", "", "12345678901", "1234"})
    @WithMockUser(username = "00000000-0000-0000-0000-000000000002", roles = "PARTNER")
    void createProfile_invalidPhone_shouldReturn400(String phone) throws Exception {
        String body = String.format(
                "{\"name\":\"Test\",\"type\":\"CLINIC\",\"address\":\"A\",\"city\":\"HN\","
                        + "\"phone\":\"%s\",\"email\":\"t@t.vn\"}", phone);

        mockMvc.perform(post(BASE_URL).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest());

        verify(partnerProfileService, never()).createProfile(any(), any());
    }

    // PTR-TC-006: valid phone formats → 201 (TC-COND-006)
    @ParameterizedTest
    @ValueSource(strings = {"0901234567", "0912345678", "+84901234567"})
    @WithMockUser(username = "00000000-0000-0000-0000-000000000002", roles = "PARTNER")
    void createProfile_validPhone_shouldPassValidation(String phone) throws Exception {
        UUID newId = UUID.randomUUID();
        when(partnerProfileService.createProfile(any(), any())).thenReturn(makeResponse(newId));

        String body = String.format(
                "{\"name\":\"Test Clinic\",\"type\":\"CLINIC\",\"address\":\"A\",\"city\":\"HN\","
                        + "\"phone\":\"%s\",\"email\":\"test@clinic.vn\"}", phone);

        mockMvc.perform(post(BASE_URL).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isCreated());
    }

    // PTR-TC-006: invalid email → 400
    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000002", roles = "PARTNER")
    void createProfile_invalidEmail_shouldReturn400() throws Exception {
        mockMvc.perform(post(BASE_URL).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Test","type":"CLINIC","address":"A","city":"HN","phone":"0901234567","email":"not-email"}
                        """))
                .andExpect(status().isBadRequest());
    }

    // PTR-TC-009: website optional — request không có website vẫn thành công (TC-COND-009)
    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000002", roles = "PARTNER")
    void createProfile_withoutWebsite_shouldReturn201() throws Exception {
        UUID newId = UUID.randomUUID();
        when(partnerProfileService.createProfile(any(), any())).thenReturn(makeResponse(newId));

        mockMvc.perform(post(BASE_URL).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(validBody()))
                .andExpect(status().isCreated());
    }

    // PTR-TC-009: website invalid URL → 400
    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000002", roles = "PARTNER")
    void createProfile_invalidWebsite_shouldReturn400() throws Exception {
        mockMvc.perform(post(BASE_URL).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Test","type":"CLINIC","address":"A","city":"HN",
                         "phone":"0901234567","email":"t@t.vn","website":"not-a-url"}
                        """))
                .andExpect(status().isBadRequest());
    }

    // PTR-TC-007: unauthenticated → 401 (TC-COND-007)
    @Test
    void createProfile_unauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(validBody()))
                .andExpect(status().isUnauthorized());
    }
}
