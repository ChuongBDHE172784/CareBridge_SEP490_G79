package com.carebridge.backend.expert.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.expert.dto.response.ExpertDirectoryResponse;
import com.carebridge.backend.expert.dto.response.ExpertProfileDetailResponse;
import com.carebridge.backend.expert.service.IExpertProfileService;
import com.carebridge.backend.expert.verificationstatus.VerificationStatus;
import com.carebridge.backend.expertverification.adapter.CompreFacePipelineAdapter;
import com.carebridge.backend.expertverification.adapter.FaceVerificationAdapter;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;
import java.util.List;
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

// MEDI-TC-001b, MEDI-TC-002b, MEDI-TC-004, MEDI-TC-023
@WebMvcTest(
        value = ExpertProfileController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class))
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class ExpertProfileControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private IExpertProfileService expertProfileService;
    @MockitoBean private FaceVerificationAdapter faceVerificationAdapter;
    @MockitoBean private CompreFacePipelineAdapter compreFacePipelineAdapter;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private UserRepository userRepository;

    private static final UUID EXPERT_PROFILE_ID = UUID.randomUUID();

    // MEDI-TC-001b — GET /expert/profiles/{id} response has displayName, email, phoneNumber from users
    @Test
    @WithMockUser(username = "aaaaaaaa-0000-0000-0000-000000000001", roles = "MOTHER")
    void getPublicProfile_returnsDisplayNameAndContact() throws Exception {
        ExpertProfileDetailResponse response = ExpertProfileDetailResponse.builder()
                .expertProfileId(EXPERT_PROFILE_ID)
                .displayName("Nguyễn Văn A")
                .email("expert.a@carebridge.vn")
                .phoneNumber("0912345678")
                .phone("0912345678")
                .verificationStatus(VerificationStatus.APPROVED)
                .build();
        when(expertProfileService.getPublicProfile(EXPERT_PROFILE_ID)).thenReturn(response);

        mockMvc.perform(get("/api/v1/expert/profiles/" + EXPERT_PROFILE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.displayName").value("Nguyễn Văn A"))
                .andExpect(jsonPath("$.data.email").value("expert.a@carebridge.vn"))
                .andExpect(jsonPath("$.data.phoneNumber").value("0912345678"));
    }

    // MEDI-TC-002b — q > 100 chars boundary
    @Test
    @WithMockUser(username = "aaaaaaaa-0000-0000-0000-000000000001", roles = "MOTHER")
    void getDirectory_qOver100Chars_rejected() throws Exception {
        String q101 = "a".repeat(101);
        when(expertProfileService.getPublicDirectory(null, null, 0, 10))
                .thenReturn(new ExpertDirectoryResponse(List.of(), 0, 10, 0, 0, List.of()));

        mockMvc.perform(get("/api/v1/expert/directory").param("q", q101))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @WithMockUser(username = "aaaaaaaa-0000-0000-0000-000000000001", roles = "MOTHER")
    void getDirectory_qExactly100Chars_accepted() throws Exception {
        String q100 = "a".repeat(100);
        when(expertProfileService.getPublicDirectory(null, q100, 0, 10))
                .thenReturn(new ExpertDirectoryResponse(List.of(), 0, 10, 0, 0, List.of()));

        mockMvc.perform(get("/api/v1/expert/directory").param("q", q100))
                .andExpect(status().isOk());
    }

    // MEDI-TC-004 — size > 50 boundary (regression guard, oracle corrected from fictional EXP-010)
    @Test
    @WithMockUser(username = "aaaaaaaa-0000-0000-0000-000000000001", roles = "MOTHER")
    void getDirectory_sizeOver50_rejectedAsGenericValidationError() throws Exception {
        mockMvc.perform(get("/api/v1/expert/directory").param("size", "51"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }
}
