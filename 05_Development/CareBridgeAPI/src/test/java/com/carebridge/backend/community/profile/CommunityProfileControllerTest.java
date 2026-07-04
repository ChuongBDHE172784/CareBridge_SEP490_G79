package com.carebridge.backend.community.profile;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.community.controller.CommunityProfileController;
import com.carebridge.backend.community.dto.response.CommunityProfileResponse;
import com.carebridge.backend.community.service.CommunityProfileService;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;
import java.time.Instant;
import java.util.UUID;
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

/**
 * COMM-TC-020-003/004 and COMM-TC-021-004/005 (Test-Specs §4) — controller
 * validation/auth checks, no business logic in the controller.
 */
@WebMvcTest(
        value = CommunityProfileController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class))
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class CommunityProfileControllerTest {

    private static final String BASE_URL = "/api/v1/community/profiles";
    private static final String USER_ID = "00000000-0000-0000-0000-000000000020";

    @Autowired private MockMvc mockMvc;

    @MockitoBean private CommunityProfileService profileService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private UserRepository userRepository;

    // ── UC-20 Create ──────────────────────────────────────────────────────

    // COMM-TC-020-003: empty displayName → 400
    @Test
    @WithMockUser(username = USER_ID, roles = "MOTHER")
    void createProfile_emptyDisplayName_returns400() throws Exception {
        String body = """
                {"displayName": "", "bio": "Test bio", "interestStage": "PREGNANCY"}
                """;

        mockMvc.perform(post(BASE_URL).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(profileService, never()).createProfile(any(), any());
    }

    // COMM-TC-020-003c: displayName > 100 chars → 400
    @Test
    @WithMockUser(username = USER_ID, roles = "MOTHER")
    void createProfile_displayNameTooLong_returns400() throws Exception {
        String longName = "A".repeat(101);
        String body = "{\"displayName\": \"" + longName + "\"}";

        mockMvc.perform(post(BASE_URL).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // COMM-TC-020-004: no JWT → 401
    @Test
    void createProfile_noJwt_returns401() throws Exception {
        String body = """
                {"displayName": "SomeUser", "bio": "Test"}
                """;

        mockMvc.perform(post(BASE_URL).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());

        verify(profileService, never()).createProfile(any(), any());
    }

    // Happy path: valid create → 201
    @Test
    @WithMockUser(username = USER_ID, roles = "MOTHER")
    void createProfile_validRequest_returns201() throws Exception {
        CommunityProfileResponse response = CommunityProfileResponse.builder()
                .communityProfileId(UUID.randomUUID())
                .userId(UUID.fromString(USER_ID))
                .displayName("TestMother20")
                .visible(true)
                .createdAt(Instant.now())
                .build();
        when(profileService.createProfile(eq(UUID.fromString(USER_ID)), any())).thenReturn(response);

        String body = """
                {"displayName": "TestMother20", "bio": "bio", "interestStage": "PREGNANCY", "isVisible": true}
                """;

        mockMvc.perform(post(BASE_URL).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.displayName").value("TestMother20"));
    }

    // ── UC-21 Update ──────────────────────────────────────────────────────

    // COMM-TC-021-004: no JWT → 401
    @Test
    void updateProfile_noJwt_returns401() throws Exception {
        String body = """
                {"displayName": "SomeUpdate", "isVisible": true}
                """;

        mockMvc.perform(put(BASE_URL + "/me").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());

        verify(profileService, never()).updateProfile(any(), any());
    }

    // COMM-TC-021-005: displayName > 100 chars → 400
    @Test
    @WithMockUser(username = USER_ID, roles = "MOTHER")
    void updateProfile_displayNameTooLong_returns400() throws Exception {
        String longName = "C".repeat(101);
        String body = "{\"displayName\": \"" + longName + "\", \"isVisible\": true}";

        mockMvc.perform(put(BASE_URL + "/me").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(profileService, never()).updateProfile(any(), any());
    }

    // Happy path: valid update → 200
    @Test
    @WithMockUser(username = USER_ID, roles = "MOTHER")
    void updateProfile_validRequest_returns200() throws Exception {
        CommunityProfileResponse response = CommunityProfileResponse.builder()
                .communityProfileId(UUID.randomUUID())
                .userId(UUID.fromString(USER_ID))
                .displayName("UpdatedName")
                .visible(false)
                .createdAt(Instant.now())
                .build();
        when(profileService.updateProfile(eq(UUID.fromString(USER_ID)), any())).thenReturn(response);

        String body = """
                {"displayName": "UpdatedName", "isVisible": false}
                """;

        mockMvc.perform(put(BASE_URL + "/me").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.displayName").value("UpdatedName"))
                .andExpect(jsonPath("$.data.visible").value(false));
    }
}
