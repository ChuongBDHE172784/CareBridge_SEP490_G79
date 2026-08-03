package com.carebridge.backend.family.policy;

import com.carebridge.backend.family.CareGroupTestFactory;
import com.carebridge.backend.family.entity.CareGroupMember;
import com.carebridge.backend.family.entity.GroupMemberRole;
import com.carebridge.backend.family.entity.InviteStatus;
import com.carebridge.backend.family.entity.PermissionFlag;
import com.carebridge.backend.family.repository.CareGroupMemberRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.CALLS_REAL_METHODS;

@ExtendWith(MockitoExtension.class)
class CareGroupAuthorizationPolicyTest {

    @Mock private CareGroupMemberRepository memberRepository;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();
    @InjectMocks private CareGroupAuthorizationPolicy policy;

    private static final UUID GROUP_ID  = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID CALLER_ID = UUID.fromString("00000000-0000-0000-0000-000000000011");

    // ── TC-006: Non-owner (ACCEPTED but role=MEMBER) → canManagePermissions returns false ──

    @Test
    void canManagePermissions_acceptedMemberRoleNotOwner_returnsFalse() {
        CareGroupMember memberRoleMember = CareGroupTestFactory.makeCareGroupMember(m -> {
            m.setCareGroupId(GROUP_ID);
            m.setUserId(CALLER_ID);
            m.setMemberRole(GroupMemberRole.MEMBER); // explicitly NOT OWNER
            m.setInviteStatus(InviteStatus.ACCEPTED);
        });
        when(memberRepository.findByCareGroupIdAndUserId(GROUP_ID, CALLER_ID))
                .thenReturn(Optional.of(memberRoleMember));

        boolean result = policy.canManagePermissions(GROUP_ID, CALLER_ID);

        assertThat(result).isFalse();
    }

    @Test
    void canManagePermissions_ownerAccepted_returnsTrue() {
        CareGroupMember ownerMember = CareGroupTestFactory.makeCareGroupMember(m -> {
            m.setCareGroupId(GROUP_ID);
            m.setUserId(CALLER_ID);
            m.setMemberRole(GroupMemberRole.OWNER);
            m.setInviteStatus(InviteStatus.ACCEPTED);
        });
        when(memberRepository.findByCareGroupIdAndUserId(GROUP_ID, CALLER_ID))
                .thenReturn(Optional.of(ownerMember));

        boolean result = policy.canManagePermissions(GROUP_ID, CALLER_ID);

        assertThat(result).isTrue();
    }

    @Test
    void canManagePermissions_noMembership_returnsFalse() {
        when(memberRepository.findByCareGroupIdAndUserId(GROUP_ID, CALLER_ID))
                .thenReturn(Optional.empty());

        boolean result = policy.canManagePermissions(GROUP_ID, CALLER_ID);

        assertThat(result).isFalse();
    }

    // ── FAM73-TC-004: canAssignTasks returns false for non-member ─────────────

    @Test
    void canAssignTasks_nonMember_returnsFalse() {
        UUID unknownCaller = UUID.randomUUID();
        when(memberRepository.findByCareGroupIdAndUserId(GROUP_ID, unknownCaller))
                .thenReturn(Optional.empty());

        boolean result = policy.canAssignTasks(GROUP_ID, unknownCaller);

        assertThat(result).isFalse();
    }

    @Test
    void canAssignTasks_acceptedOwner_returnsTrue() {
        CareGroupMember ownerMember = CareGroupTestFactory.makeCareGroupMember(m -> {
            m.setCareGroupId(GROUP_ID);
            m.setUserId(CALLER_ID);
            m.setMemberRole(GroupMemberRole.OWNER);
            m.setInviteStatus(InviteStatus.ACCEPTED);
        });
        when(memberRepository.findByCareGroupIdAndUserId(GROUP_ID, CALLER_ID))
                .thenReturn(Optional.of(ownerMember));

        boolean result = policy.canAssignTasks(GROUP_ID, CALLER_ID);

        assertThat(result).isTrue();
    }

    @Test
    void canAssignTasks_acceptedMemberNotOwner_returnsFalse() {
        CareGroupMember nonOwner = CareGroupTestFactory.makeCareGroupMember(m -> {
            m.setCareGroupId(GROUP_ID);
            m.setUserId(CALLER_ID);
            m.setMemberRole(GroupMemberRole.MEMBER);
            m.setInviteStatus(InviteStatus.ACCEPTED);
        });
        when(memberRepository.findByCareGroupIdAndUserId(GROUP_ID, CALLER_ID))
                .thenReturn(Optional.of(nonOwner));

        boolean result = policy.canAssignTasks(GROUP_ID, CALLER_ID);

        assertThat(result).isFalse();
    }

    @Test
    void isMember_acceptedExpiredMembership_returnsFalse() {
        CareGroupMember expired = CareGroupTestFactory.makeCareGroupMember(m -> {
            m.setCareGroupId(GROUP_ID);
            m.setUserId(CALLER_ID);
            m.setInviteStatus(InviteStatus.ACCEPTED);
            m.setInviteExpiresAt(Instant.now().minusSeconds(60));
        });
        when(memberRepository.findByCareGroupIdAndUserId(GROUP_ID, CALLER_ID))
                .thenReturn(Optional.of(expired));

        assertThat(policy.isMember(GROUP_ID, CALLER_ID)).isFalse();
    }

    @Test
    void hasPermission_readsCamelCaseQuickNoteKeysWithoutChangingLegacyKeys() {
        CareGroupMember member = CareGroupTestFactory.makeCareGroupMember(m -> {
            m.setCareGroupId(GROUP_ID);
            m.setUserId(CALLER_ID);
            m.setPermissionJson("{\"quickNotes\":true,\"quickNoteWeight\":true,"
                    + "\"baby_view\":true,\"alerts\":false}");
        });
        when(memberRepository.findByCareGroupIdAndUserId(GROUP_ID, CALLER_ID))
                .thenReturn(Optional.of(member));

        assertThat(policy.hasPermission(GROUP_ID, CALLER_ID, PermissionFlag.QUICK_NOTES)).isTrue();
        assertThat(policy.hasPermission(GROUP_ID, CALLER_ID, PermissionFlag.QUICK_NOTE_WEIGHT)).isTrue();
        assertThat(policy.hasPermission(GROUP_ID, CALLER_ID, PermissionFlag.BABY_VIEW)).isTrue();
        assertThat(policy.hasPermission(GROUP_ID, CALLER_ID, PermissionFlag.ALERTS)).isFalse();
    }

    @Test
    void membershipLookup_prefersAcceptedMembershipOverOlderRevokedRow() {
        CareGroupMember revoked = CareGroupTestFactory.makeCareGroupMember(m -> {
            m.setCareGroupId(GROUP_ID);
            m.setUserId(CALLER_ID);
            m.setInviteStatus(InviteStatus.REVOKED);
        });
        CareGroupMember accepted = CareGroupTestFactory.makeCareGroupMember(m -> {
            m.setCareGroupId(GROUP_ID);
            m.setUserId(CALLER_ID);
            m.setInviteStatus(InviteStatus.ACCEPTED);
            m.setPermissionJson("{\"records\":true}");
        });
        CareGroupMemberRepository repository = mock(
                CareGroupMemberRepository.class, CALLS_REAL_METHODS);
        when(repository.findAllByCareGroupIdAndUserId(GROUP_ID, CALLER_ID))
                .thenReturn(java.util.List.of(revoked, accepted));

        assertThat(repository.findByCareGroupIdAndUserId(GROUP_ID, CALLER_ID))
                .containsSame(accepted);
    }
}
