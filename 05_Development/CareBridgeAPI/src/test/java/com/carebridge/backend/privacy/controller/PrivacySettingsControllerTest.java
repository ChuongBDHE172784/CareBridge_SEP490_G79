package com.carebridge.backend.privacy.controller;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.privacy.dto.PrivacySettingsResponse;
import com.carebridge.backend.privacy.dto.UpdatePrivacySettingsRequest;
import com.carebridge.backend.privacy.entity.ProfileVisibility;
import com.carebridge.backend.privacy.service.PrivacySettingsService;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = PrivacySettingsController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class PrivacySettingsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @MockitoBean
    private PrivacySettingsService privacySettingsService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    private static final String BASE_URL = "/api/v1/privacy-settings/me";
    private static final String USER_ID = "00000000-0000-0000-0000-000000000001";

    private PrivacySettingsResponse sampleResponse() {
        return new PrivacySettingsResponse(
                UUID.randomUUID(),
                UUID.fromString(USER_ID),
                "FRIENDS_ONLY",
                false,
                false,
                false,
                Instant.now()
        );
    }

    @Test
    @DisplayName("PRIV-TC-CTR-001: GET /me returns 200 for authenticated user")
    @WithMockUser(username = USER_ID)
    void getMySettings_authenticated_returns200() throws Exception {
        when(privacySettingsService.getSettings(any(UUID.class), any())).thenReturn(sampleResponse());

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.profileVisibility").value("FRIENDS_ONLY"));
    }

    @Test
    @DisplayName("PRIV-TC-CTR-002: GET /me returns 401 for unauthenticated")
    void getMySettings_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PRIV-TC-CTR-003: PUT /me returns 200 with updated settings")
    @WithMockUser(username = USER_ID)
    void updateMySettings_validRequest_returns200() throws Exception {
        UpdatePrivacySettingsRequest request = new UpdatePrivacySettingsRequest(
                ProfileVisibility.PUBLIC, true, true, false);

        PrivacySettingsResponse updated = new PrivacySettingsResponse(
                UUID.randomUUID(),
                UUID.fromString(USER_ID),
                "PUBLIC",
                true,
                true,
                false,
                Instant.now()
        );

        when(privacySettingsService.updateSettings(any(UUID.class), any(), any())).thenReturn(updated);

        mockMvc.perform(put(BASE_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.profileVisibility").value("PUBLIC"))
                .andExpect(jsonPath("$.data.locationSharingEnabled").value(true));
    }

    @Test
    @DisplayName("PRIV-TC-CTR-004: PUT /me returns 401 for unauthenticated")
    void updateMySettings_unauthenticated_returns401() throws Exception {
        UpdatePrivacySettingsRequest request = new UpdatePrivacySettingsRequest(
                ProfileVisibility.PUBLIC, null, null, null);

        mockMvc.perform(put(BASE_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
