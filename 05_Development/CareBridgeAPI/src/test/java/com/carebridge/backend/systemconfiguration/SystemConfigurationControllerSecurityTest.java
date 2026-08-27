package com.carebridge.backend.systemconfiguration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.systemconfiguration.controller.SystemConfigurationController;
import com.carebridge.backend.systemconfiguration.dto.response.SystemConfigurationResponse;
import com.carebridge.backend.systemconfiguration.service.SystemConfigurationService;
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

@WebMvcTest(
        value = SystemConfigurationController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class SystemConfigurationControllerSecurityTest {

    private static final String BASE_URL = "/api/v1/admin/system-configuration";
    private static final String VALID_BODY = """
            {
              "aiModerationEnabled": true,
              "maintenanceModeEnabled": false,
              "rowVersion": 0
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SystemConfigurationService service;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void get_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(get(BASE_URL)).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-0000000000bb", roles = "MODERATOR")
    void endpoints_asModerator_return403() throws Exception {
        mockMvc.perform(get(BASE_URL)).andExpect(status().isForbidden());
        mockMvc.perform(put(BASE_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isForbidden());

        verify(service, never()).get(any());
        verify(service, never()).update(any(), any());
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-0000000000aa", roles = "SYSTEM_ADMIN")
    void update_missingVersion_returns400() throws Exception {
        String bodyWithoutVersion = VALID_BODY.replace(",\n  \"rowVersion\": 0", "");

        mockMvc.perform(put(BASE_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(bodyWithoutVersion))
                .andExpect(status().isBadRequest());

        verify(service, never()).update(any(), any());
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-0000000000aa", roles = "SYSTEM_ADMIN")
    void endpoints_asSystemAdmin_reachServiceThroughSecurityChain() throws Exception {
        SystemConfigurationResponse response = new SystemConfigurationResponse(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                true,
                false,
                3L,
                UUID.fromString("00000000-0000-0000-0000-0000000000aa"),
                Instant.parse("2026-07-28T02:00:00Z"));
        when(service.get(any())).thenReturn(response);
        when(service.update(any(), any())).thenReturn(response);

        mockMvc.perform(get(BASE_URL)).andExpect(status().isOk());
        mockMvc.perform(put(BASE_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk());

        verify(service).get(any());
        verify(service).update(any(), any());
    }
}
