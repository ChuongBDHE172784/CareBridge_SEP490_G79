package com.carebridge.backend.family.service;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.family.dto.AcceptInvitationByTokenResponse;
import com.carebridge.backend.family.entity.CareGroupMember;
import com.carebridge.backend.family.entity.GroupMemberRole;
import com.carebridge.backend.family.entity.InviteChannel;
import com.carebridge.backend.family.entity.InviteStatus;
import com.carebridge.backend.family.policy.CareGroupAuthorizationPolicy;
import com.carebridge.backend.family.repository.CareGroupMemberRepository;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.family.service.impl.CareGroupServiceImpl;
import com.carebridge.backend.notification.repository.DeviceTokenRepository;
import com.carebridge.backend.notification.service.FcmService;
import com.carebridge.backend.security.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * UC-83: Accept Care Group Invitation via token.
 * Tests the service layer (acceptInvitationByToken) using Mockito.
 * Oracle: ADR-FAM-006 (lazy expiry), ADR-FAM-007 (phone-match), ADR-FAM-008 (atomic accept).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CareGroupServiceImplAcceptInvitationTest {

    @Mock private CareGroupRepository groupRepository;
    @Mock private CareGroupMemberRepository memberRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditService auditService;
    @Mock private CareGroupAuthorizationPolicy authorizationPolicy;
    @Mock private InviteTokenGenerator tokenGenerator;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private FcmService fcmService;
    @Mock private DeviceTokenRepository deviceTokenRepository;

    @InjectMocks
    private CareGroupServiceImpl service;

    private static final UUID CALLER_ID = UUID.fromString("00000000-0000-0000-0000-000000000a01");
    private static final UUID GROUP_ID  = UUID.fromString("00000000-0000-0000-0000-000000000c01");
    private static final UUID MEMBER_ID = UUID.fromString("00000000-0000-0000-0000-000000000b01");

    // ─── Factory helpers ──────────────────────────────────────────────────────

    private CareGroupMember makePendingMember(InviteChannel channel, String token) {
        CareGroupMember m = new CareGroupMember();
        m.setId(MEMBER_ID);
        m.setCareGroupId(GROUP_ID);
        m.setUserId(CALLER_ID);
        m.setMemberRole(GroupMemberRole.MEMBER);
        m.setInviteStatus(InviteStatus.PENDING);
        m.setInviteChannel(channel);
        m.setInviteToken(token);
        m.setInviteExpiresAt(Instant.now().plusSeconds(3600));
        m.setInvitedPhone(null);
        return m;
    }

    // ─── FAM83-TC-001: LINK channel happy path ────────────────────────────────

    @Test
    void acceptByToken_linkChannel_returnsAccepted() {
        CareGroupMember member = makePendingMember(InviteChannel.LINK, "tok-link-001");
        when(memberRepository.findByInviteToken("tok-link-001")).thenReturn(Optional.of(member));
        when(memberRepository.acceptIfPending(eq(MEMBER_ID), any(Instant.class))).thenReturn(1);
        when(deviceTokenRepository.findByUserIdAndActiveTrue(any())).thenReturn(List.of());

        AcceptInvitationByTokenResponse response = service.acceptInvitationByToken("tok-link-001", CALLER_ID);

        assertThat(response.getInviteStatus()).isEqualTo(InviteStatus.ACCEPTED.name());
        assertThat(response.getCareGroupId()).isEqualTo(GROUP_ID);
        assertThat(response.getJoinedAt()).isNotNull();
        verify(memberRepository).acceptIfPending(eq(MEMBER_ID), any(Instant.class));
        verify(authorizationPolicy, never()).isPhoneMatchForInvite(any(), any());
    }

    // ─── FAM83-TC-002: QR channel happy path (same as LINK — no phone check) ──

    @Test
    void acceptByToken_qrChannel_returnsAcceptedWithoutPhoneCheck() {
        CareGroupMember member = makePendingMember(InviteChannel.QR, "tok-qr-001");
        when(memberRepository.findByInviteToken("tok-qr-001")).thenReturn(Optional.of(member));
        when(memberRepository.acceptIfPending(eq(MEMBER_ID), any(Instant.class))).thenReturn(1);
        when(deviceTokenRepository.findByUserIdAndActiveTrue(any())).thenReturn(List.of());

        AcceptInvitationByTokenResponse response = service.acceptInvitationByToken("tok-qr-001", CALLER_ID);

        assertThat(response.getInviteStatus()).isEqualTo(InviteStatus.ACCEPTED.name());
        verify(authorizationPolicy, never()).isPhoneMatchForInvite(any(), any());
    }

    // ─── FAM83-TC-003: PHONE channel, phone matches ───────────────────────────

    @Test
    void acceptByToken_phoneChannel_phoneMatches_returnsAccepted() {
        CareGroupMember member = makePendingMember(InviteChannel.PHONE, "tok-phone-001");
        member.setInvitedPhone("+84900000001");
        when(memberRepository.findByInviteToken("tok-phone-001")).thenReturn(Optional.of(member));
        when(authorizationPolicy.isPhoneMatchForInvite(member, CALLER_ID)).thenReturn(true);
        when(memberRepository.acceptIfPending(eq(MEMBER_ID), any(Instant.class))).thenReturn(1);
        when(deviceTokenRepository.findByUserIdAndActiveTrue(any())).thenReturn(List.of());

        AcceptInvitationByTokenResponse response = service.acceptInvitationByToken("tok-phone-001", CALLER_ID);

        assertThat(response.getInviteStatus()).isEqualTo(InviteStatus.ACCEPTED.name());
        verify(authorizationPolicy).isPhoneMatchForInvite(member, CALLER_ID);
    }

    // ─── FAM83-TC-004: Token not found → FAM-040 404 ─────────────────────────

    @Test
    void acceptByToken_tokenNotFound_throwsFam040() {
        when(memberRepository.findByInviteToken("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.acceptInvitationByToken("nonexistent", CALLER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo("FAM-040");
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });
    }

    // ─── FAM83-TC-005: Lazy expiry → marks EXPIRED + FAM-041 (410 Gone) ──────

    @Test
    void acceptByToken_pendingButExpired_marksExpiredAndThrowsFam041() {
        CareGroupMember member = makePendingMember(InviteChannel.LINK, "tok-expired-001");
        member.setInviteExpiresAt(Instant.now().minusSeconds(3600));
        when(memberRepository.findByInviteToken("tok-expired-001")).thenReturn(Optional.of(member));
        when(memberRepository.markExpiredIfPending(eq(MEMBER_ID), any(Instant.class))).thenReturn(1);

        assertThatThrownBy(() -> service.acceptInvitationByToken("tok-expired-001", CALLER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo("FAM-041");
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.GONE);
                });
        // ADR-FAM-006: lazy transition MUST have been called
        verify(memberRepository).markExpiredIfPending(eq(MEMBER_ID), any(Instant.class));
    }

    // ─── FAM83-TC-006: Already ACCEPTED → FAM-042 409 ───────────────────────

    @Test
    void acceptByToken_alreadyAccepted_throwsFam042() {
        CareGroupMember member = makePendingMember(InviteChannel.LINK, "tok-accepted-001");
        member.setInviteStatus(InviteStatus.ACCEPTED);
        when(memberRepository.findByInviteToken("tok-accepted-001")).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> service.acceptInvitationByToken("tok-accepted-001", CALLER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("FAM-042"));
        verify(memberRepository, never()).acceptIfPending(any(), any());
    }

    // ─── FAM83-TC-007/TC-008: REVOKED / REJECTED → FAM-042 ──────────────────

    @Test
    void acceptByToken_revoked_throwsFam042() {
        CareGroupMember member = makePendingMember(InviteChannel.LINK, "tok-revoked-001");
        member.setInviteStatus(InviteStatus.REVOKED);
        when(memberRepository.findByInviteToken("tok-revoked-001")).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> service.acceptInvitationByToken("tok-revoked-001", CALLER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("FAM-042"));
    }

    // ─── FAM83-TC-009: Pre-existing EXPIRED (not PENDING) → FAM-042, NOT FAM-041

    @Test
    void acceptByToken_preExpiredStatus_throwsFam042NotFam041() {
        CareGroupMember member = makePendingMember(InviteChannel.LINK, "tok-preexpired-001");
        member.setInviteStatus(InviteStatus.EXPIRED);
        // even if expiresAt < now, since status is already EXPIRED (not PENDING), lazy expiry branch skipped
        member.setInviteExpiresAt(Instant.now().minusSeconds(3600));
        when(memberRepository.findByInviteToken("tok-preexpired-001")).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> service.acceptInvitationByToken("tok-preexpired-001", CALLER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo("FAM-042");  // NOT FAM-041
                });
        // Lazy expiry MUST NOT be called (status is already EXPIRED, not PENDING)
        verify(memberRepository, never()).markExpiredIfPending(any(), any());
    }

    // ─── FAM83-TC-010: PHONE channel, phone mismatch → FAM-043 403 ───────────

    @Test
    void acceptByToken_phoneChannel_phoneMismatch_throwsFam043() {
        CareGroupMember member = makePendingMember(InviteChannel.PHONE, "tok-phone-001");
        member.setInvitedPhone("+84900000001");
        when(memberRepository.findByInviteToken("tok-phone-001")).thenReturn(Optional.of(member));
        when(authorizationPolicy.isPhoneMatchForInvite(member, CALLER_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.acceptInvitationByToken("tok-phone-001", CALLER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo("FAM-043");
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                });
        verify(memberRepository, never()).acceptIfPending(any(), any());
    }

    // ─── FAM83-TC-013: joinedAt is set exactly once and returned in response ──

    @Test
    void acceptByToken_successful_joinedAtSetInResponse() {
        CareGroupMember member = makePendingMember(InviteChannel.LINK, "tok-link-001");
        when(memberRepository.findByInviteToken("tok-link-001")).thenReturn(Optional.of(member));
        ArgumentCaptor<Instant> joinedAtCaptor = ArgumentCaptor.forClass(Instant.class);
        when(memberRepository.acceptIfPending(eq(MEMBER_ID), joinedAtCaptor.capture())).thenReturn(1);
        when(deviceTokenRepository.findByUserIdAndActiveTrue(any())).thenReturn(List.of());

        Instant before = Instant.now();
        AcceptInvitationByTokenResponse response = service.acceptInvitationByToken("tok-link-001", CALLER_ID);
        Instant after = Instant.now();

        Instant joinedAt = joinedAtCaptor.getValue();
        assertThat(joinedAt).isBetween(before, after);
        assertThat(response.getJoinedAt()).isNotNull();
    }

    // ─── FAM83-TC-014: Audit log emitted on success ───────────────────────────

    @Test
    void acceptByToken_successful_emitsAuditLog() {
        CareGroupMember member = makePendingMember(InviteChannel.LINK, "tok-link-001");
        when(memberRepository.findByInviteToken("tok-link-001")).thenReturn(Optional.of(member));
        when(memberRepository.acceptIfPending(eq(MEMBER_ID), any(Instant.class))).thenReturn(1);
        when(deviceTokenRepository.findByUserIdAndActiveTrue(any())).thenReturn(List.of());

        service.acceptInvitationByToken("tok-link-001", CALLER_ID);

        verify(auditService).log(
                eq(AuditAction.CARE_GROUP_INVITATION_ACCEPTED),
                eq(CALLER_ID),
                eq("CareGroupMember"),
                eq(MEMBER_ID.toString()),
                anyString()
        );
    }

    // ─── FAM83-TC-015: FCM failure does not fail the transaction ─────────────

    @Test
    void acceptByToken_fcmThrows_acceptStillSucceeds() {
        CareGroupMember member = makePendingMember(InviteChannel.LINK, "tok-link-001");
        when(memberRepository.findByInviteToken("tok-link-001")).thenReturn(Optional.of(member));
        when(memberRepository.acceptIfPending(eq(MEMBER_ID), any(Instant.class))).thenReturn(1);
        // Simulate FCM outage by throwing during deviceToken lookup
        when(deviceTokenRepository.findByUserIdAndActiveTrue(any()))
                .thenThrow(new RuntimeException("FCM service unavailable"));

        // Must NOT throw — FCM is best-effort (SRS E3)
        assertThatCode(() -> service.acceptInvitationByToken("tok-link-001", CALLER_ID))
                .doesNotThrowAnyException();
    }
}
