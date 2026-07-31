package com.carebridge.backend.family;

import com.carebridge.backend.family.entity.CareGroupMember;
import com.carebridge.backend.family.entity.GroupMemberRole;
import com.carebridge.backend.family.entity.InviteStatus;
import com.carebridge.backend.family.entity.PermissionFlag;
import com.carebridge.backend.family.policy.CareGroupAuthorizationPolicy;
import com.carebridge.backend.family.repository.CareGroupMemberRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for UC-74 PDPA-critical permission checks in CareGroupAuthorizationPolicy.
 *
 * <p>FAM-UC74-TC-008 and FAM-UC74-TC-009 both test the default-deny behavior of
 * {@link CareGroupAuthorizationPolicy#hasPermission(UUID, UUID, PermissionFlag)}.
 * A wrong default (allowing access) is a PII exposure bug under BR-PRIVACY / PDPA.
 *
 * <p>Note: these tests directly test a method that IS already implemented (not a stub),
 * so they may PASS in the Red Gate run. This is acceptable because the policy class
 * was implemented as part of shared UC72/73/74 infrastructure — the Red Gate stubs
 * apply specifically to {@code CareCalendarServiceImpl} / {@code CareTaskServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class CareGroupAccessPolicyTest {

    @Mock private CareGroupMemberRepository memberRepository;
    @Spy  private ObjectMapper objectMapper;
    @InjectMocks private CareGroupAuthorizationPolicy policy;

    private static final UUID GROUP_ID = CareCalendarTestFactory.GROUP_CG_001;
    private static final UUID ACC_006   = CareCalendarTestFactory.ACC_006; // permission_json = NULL
    private static final UUID ACC_007   = CareCalendarTestFactory.ACC_007; // missing "calendar" key

    // ── FAM-UC74-TC-008: permission_json is NULL → default-deny ──────────────

    @Test
    void hasPermission_permissionJsonNull_returnsFalse() {
        CareGroupMember member = CareCalendarTestFactory.makeMember(
                GROUP_ID, ACC_006, InviteStatus.ACCEPTED, null); // FX-009: NULL json
        when(memberRepository.findByCareGroupIdAndUserId(GROUP_ID, ACC_006))
                .thenReturn(Optional.of(member));

        boolean result = policy.hasPermission(GROUP_ID, ACC_006, PermissionFlag.CALENDAR);

        assertThat(result).isFalse(); // MUST be false — wrong default is a PII leak (BR-PRIVACY)
    }

    // ── FAM-UC74-TC-009: permission_json missing "calendar" key → default-deny ─

    @Test
    void hasPermission_missingCalendarKey_returnsFalse() {
        CareGroupMember member = CareCalendarTestFactory.makeMember(
                GROUP_ID, ACC_007, InviteStatus.ACCEPTED, "{\"tasks\":true}"); // FX-010
        when(memberRepository.findByCareGroupIdAndUserId(GROUP_ID, ACC_007))
                .thenReturn(Optional.of(member));

        boolean result = policy.hasPermission(GROUP_ID, ACC_007, PermissionFlag.CALENDAR);

        assertThat(result).isFalse(); // key absent → default-deny
    }

    // ── Positive case: calendar=true → granted ────────────────────────────────

    @Test
    void hasPermission_calendarTrue_returnsTrue() {
        UUID userId = UUID.randomUUID();
        CareGroupMember member = CareCalendarTestFactory.makeMember(
                GROUP_ID, userId, InviteStatus.ACCEPTED, "{\"calendar\":true}");
        when(memberRepository.findByCareGroupIdAndUserId(GROUP_ID, userId))
                .thenReturn(Optional.of(member));

        boolean result = policy.hasPermission(GROUP_ID, userId, PermissionFlag.CALENDAR);

        assertThat(result).isTrue();
    }

    @Test
    void hasPermission_uppercaseChecklistViewGrant_returnsTrue() {
        UUID userId = UUID.randomUUID();
        CareGroupMember member = CareCalendarTestFactory.makeMember(
                GROUP_ID, userId, InviteStatus.ACCEPTED, "{\"CHECKLIST_VIEW\":true}");
        when(memberRepository.findByCareGroupIdAndUserId(GROUP_ID, userId))
                .thenReturn(Optional.of(member));

        assertThat(policy.hasPermission(GROUP_ID, userId, PermissionFlag.CHECKLIST_VIEW)).isTrue();
    }

    @Test
    void hasPermission_revokedMemberWithStaleGrant_returnsFalse() {
        UUID userId = UUID.randomUUID();
        CareGroupMember member = CareCalendarTestFactory.makeMember(
                GROUP_ID, userId, InviteStatus.REVOKED, "{\"CHECKLIST_VIEW\":true}");
        when(memberRepository.findByCareGroupIdAndUserId(GROUP_ID, userId))
                .thenReturn(Optional.of(member));

        assertThat(policy.hasPermission(GROUP_ID, userId, PermissionFlag.CHECKLIST_VIEW)).isFalse();
    }

    // ── Malformed JSON → default-deny (no exception propagated) ──────────────

    @Test
    void hasPermission_malformedJson_returnsFalseNoException() {
        UUID userId = UUID.randomUUID();
        CareGroupMember member = CareCalendarTestFactory.makeMember(
                GROUP_ID, userId, InviteStatus.ACCEPTED, "{not valid json}");
        when(memberRepository.findByCareGroupIdAndUserId(GROUP_ID, userId))
                .thenReturn(Optional.of(member));

        boolean result = policy.hasPermission(GROUP_ID, userId, PermissionFlag.CALENDAR);

        assertThat(result).isFalse(); // graceful degradation — parse failure → deny
    }

    // ── No membership row at all → default-deny ───────────────────────────────

    @Test
    void hasPermission_stringChecklistBoolean_returnsFalse() {
        UUID userId = UUID.randomUUID();
        CareGroupMember member = CareCalendarTestFactory.makeMember(
                GROUP_ID, userId, InviteStatus.ACCEPTED, "{\"CHECKLIST_VIEW\":\"true\"}");
        when(memberRepository.findByCareGroupIdAndUserId(GROUP_ID, userId))
                .thenReturn(Optional.of(member));

        assertThat(policy.hasPermission(GROUP_ID, userId, PermissionFlag.CHECKLIST_VIEW)).isFalse();
    }

    @Test
    void hasPermission_noMemberRow_returnsFalse() {
        UUID stranger = UUID.randomUUID();
        when(memberRepository.findByCareGroupIdAndUserId(GROUP_ID, stranger))
                .thenReturn(Optional.empty());

        boolean result = policy.hasPermission(GROUP_ID, stranger, PermissionFlag.CALENDAR);

        assertThat(result).isFalse();
    }
}
