package com.carebridge.backend.family.controller;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.family.dto.InviteFamilyMemberRequest;
import com.carebridge.backend.family.dto.InviteFamilyMemberResponse;
import com.carebridge.backend.family.entity.InviteChannel;
import com.carebridge.backend.family.service.ICareCalendarService;
import com.carebridge.backend.family.service.ICareGroupService;
import com.carebridge.backend.family.service.ICareTaskService;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = CareGroupController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class CareGroupControllerInviteTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @MockitoBean private ICareGroupService careGroupService;
    @MockitoBean private ICareTaskService careTaskService;
    @MockitoBean private ICareCalendarService careCalendarService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private UserRepository userRepository;

    private static final UUID GROUP_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final String BASE_URL = "/api/v1/care-groups/" + GROUP_ID + "/invitations";

    // ── TC-008: Invalid channel value ────────────────────────────────────────

    @Test
    @WithMockUser(roles = "MOTHER")
    void inviteFamilyMember_invalidChannelValue_returns400() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"channel\": \"EMAIL\"}"))
                .andExpect(status().isBadRequest());
    }

    // ── TC-010: Malformed phone format ────────────────────────────────────────

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000011", roles = "MOTHER")
    void inviteFamilyMember_malformedPhone_returns400() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"channel\": \"PHONE\", \"phone\": \"not-a-phone!!\"}"))
                .andExpect(status().isBadRequest());
    }

    // ── TC-021: Controller delegates to service (no business logic) ───────────

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000011", roles = "MOTHER")
    void inviteFamilyMember_validRequest_delegatesToService() throws Exception {
        InviteFamilyMemberResponse mockResponse = InviteFamilyMemberResponse.builder()
                .careGroupMemberId(UUID.randomUUID())
                .channel(InviteChannel.PHONE)
                .inviteToken("sometoken123")
                .inviteExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .invitedPhone("+84912345678")
                .build();

        when(careGroupService.inviteFamilyMember(eq(GROUP_ID), any(InviteFamilyMemberRequest.class), any(UUID.class)))
                .thenReturn(mockResponse);

        mockMvc.perform(post(BASE_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"channel\": \"PHONE\", \"phone\": \"+84912345678\"}"))
                .andExpect(status().isCreated());

        verify(careGroupService).inviteFamilyMember(eq(GROUP_ID), any(InviteFamilyMemberRequest.class), any(UUID.class));
    }

    // ── TC-022: Response shape only DTO fields ────────────────────────────────

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000011", roles = "MOTHER")
    void inviteFamilyMember_response_containsOnlyDtoFields() throws Exception {
        UUID memberId = UUID.randomUUID();
        InviteFamilyMemberResponse mockResponse = InviteFamilyMemberResponse.builder()
                .careGroupMemberId(memberId)
                .channel(InviteChannel.LINK)
                .inviteToken("linktoken456")
                .inviteExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .build();

        when(careGroupService.inviteFamilyMember(eq(GROUP_ID), any(InviteFamilyMemberRequest.class), any(UUID.class)))
                .thenReturn(mockResponse);

        mockMvc.perform(post(BASE_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"channel\": \"LINK\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.careGroupMemberId").exists())
                .andExpect(jsonPath("$.data.channel").value("LINK"))
                .andExpect(jsonPath("$.data.inviteToken").exists())
                .andExpect(jsonPath("$.data.inviteExpiresAt").exists());
    }
}
