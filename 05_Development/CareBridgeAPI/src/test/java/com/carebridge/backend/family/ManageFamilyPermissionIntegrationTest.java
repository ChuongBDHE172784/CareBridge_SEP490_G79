package com.carebridge.backend.family;

import com.carebridge.backend.family.dto.FamilyPermission;
import com.carebridge.backend.family.entity.CareGroup;
import com.carebridge.backend.family.entity.CareGroupMember;
import com.carebridge.backend.family.entity.CareGroupStatus;
import com.carebridge.backend.family.entity.GroupMemberRole;
import com.carebridge.backend.family.entity.InviteStatus;
import com.carebridge.backend.family.repository.CareGroupMemberRepository;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.notification.service.FcmService;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import com.carebridge.backend.testsupport.CanonicalUserFixture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class ManageFamilyPermissionIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private CareGroupRepository groupRepository;
    @Autowired private CareGroupMemberRepository memberRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    @MockitoBean private FcmService fcmService;

    private static final String OWNER_UUID = "00000000-0000-0000-0000-000000000011";

    // ── TC-INT-001: Full E2E — PATCH then GET reflects persisted state ─────────

    @Test
    @WithMockUser(username = OWNER_UUID, roles = "MOTHER")
    void updateAndGetFamilyPermission_patchThenGet_persistsAndReturnsCorrectState() throws Exception {
        UUID ownerId = UUID.fromString(OWNER_UUID);
        UUID memberUserId = UUID.randomUUID();
        CanonicalUserFixture.insertUser(jdbcTemplate, ownerId, "Permission owner", null, "MOTHER");
        CanonicalUserFixture.insertUser(jdbcTemplate, memberUserId, "Permission member", null, "FAMILY");
        String ownerToken = jwtTokenProvider.generateAccessToken(userRepository.findById(ownerId).orElseThrow());

        when(fcmService.sendToTokens(anyList(), any(), any())).thenReturn(0);

        // Seed: active care group + OWNER member + target ACCEPTED member
        CareGroup group = groupRepository.save(CareGroup.builder()
                .ownerUserId(ownerId)
                .groupName("UC72 Integration Test Group")
                .status(CareGroupStatus.ACTIVE)
                .build());

        memberRepository.save(CareGroupMember.builder()
                .careGroupId(group.getId())
                .userId(ownerId)
                .memberRole(GroupMemberRole.OWNER)
                .inviteStatus(InviteStatus.ACCEPTED)
                .joinedAt(Instant.now())
                .build());

        CareGroupMember target = memberRepository.save(CareGroupMember.builder()
                .careGroupId(group.getId())
                .userId(memberUserId)
                .memberRole(GroupMemberRole.MEMBER)
                .inviteStatus(InviteStatus.ACCEPTED)
                .joinedAt(Instant.now())
                .build());

        String permissionsUrl = "/api/v1/care-groups/" + group.getId()
                + "/members/" + target.getId() + "/permissions";

        // PATCH: update permissions
        mockMvc.perform(patch(permissionsUrl)
                        .header("Authorization", "Bearer " + ownerToken)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"calendar\":true,\"logs\":true,\"alerts\":false,\"records\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.calendar").value(true))
                .andExpect(jsonPath("$.data.logs").value(true))
                .andExpect(jsonPath("$.data.alerts").value(false))
                .andExpect(jsonPath("$.data.records").value(false));

        // GET: verify same flags returned
        mockMvc.perform(get(permissionsUrl)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.calendar").value(true))
                .andExpect(jsonPath("$.data.logs").value(true))
                .andExpect(jsonPath("$.data.alerts").value(false))
                .andExpect(jsonPath("$.data.records").value(false));

        // DB assertion: verify permission_json persisted correctly
        CareGroupMember saved = memberRepository.findById(target.getId()).orElseThrow();
        assertThat(saved.getPermissionJson()).isNotNull();
        FamilyPermission perm = FamilyPermission.fromJson(saved.getPermissionJson());
        assertThat(perm.isCalendar()).isTrue();
        assertThat(perm.isLogs()).isTrue();
        assertThat(perm.isAlerts()).isFalse();
        assertThat(perm.isRecords()).isFalse();
    }
}
