package com.carebridge.backend.family.service;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.family.CareGroupTestFactory;
import com.carebridge.backend.family.dto.InviteFamilyMemberRequest;
import com.carebridge.backend.family.dto.InviteFamilyMemberResponse;
import com.carebridge.backend.family.entity.CareGroup;
import com.carebridge.backend.family.entity.CareGroupMember;
import com.carebridge.backend.family.entity.CareGroupStatus;
import com.carebridge.backend.family.entity.GroupMemberRole;
import com.carebridge.backend.family.entity.InviteChannel;
import com.carebridge.backend.family.entity.InviteStatus;
import com.carebridge.backend.family.event.FamilyMemberInvited;
import com.carebridge.backend.family.policy.CareGroupAuthorizationPolicy;
import com.carebridge.backend.family.repository.CareGroupMemberRepository;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.family.service.impl.CareGroupServiceImpl;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CareGroupServiceImplInviteTest {

    @Mock private CareGroupRepository groupRepository;
    @Mock private CareGroupMemberRepository memberRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditService auditService;
    @Mock private CareGroupAuthorizationPolicy authorizationPolicy;
    @Mock private InviteTokenGenerator tokenGenerator;
    @Mock private ApplicationEventPublisher eventPublisher;
    @InjectMocks private CareGroupServiceImpl service;

    private static final UUID GROUP_ID   = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID OWNER_ID   = UUID.fromString("00000000-0000-0000-0000-000000000011");
    private static final UUID INVITEE_ID = UUID.fromString("00000000-0000-0000-0000-000000000012");
    private static final String PHONE    = "+84912345678";
    private static final String TOKEN    = "testToken123";

    private void stubOwnerCheck(boolean isOwner) {
        when(authorizationPolicy.isOwner(GROUP_ID, OWNER_ID)).thenReturn(isOwner);
    }

    private void stubActiveGroup() {
        CareGroup group = CareGroupTestFactory.makeCareGroup(g -> {
            g.setId(GROUP_ID);
            g.setStatus(CareGroupStatus.ACTIVE);
        });
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));
    }

    private void stubPendingCount(long count) {
        when(memberRepository.countByCareGroupIdAndInviteStatus(GROUP_ID, InviteStatus.PENDING)).thenReturn(count);
    }

    private void stubInviteePhone(boolean found) {
        if (found) {
            User user = new User();
            user.setId(INVITEE_ID);
            user.setPhone(PHONE);
            when(userRepository.findByPhone(PHONE)).thenReturn(Optional.of(user));
        } else {
            when(userRepository.findByPhone(PHONE)).thenReturn(Optional.empty());
        }
    }

    private void stubNoDuplicatePending() {
        when(memberRepository.existsByCareGroupIdAndUserIdAndInviteStatus(
                GROUP_ID, INVITEE_ID, InviteStatus.PENDING)).thenReturn(false);
        when(memberRepository.findByCareGroupIdAndUserId(GROUP_ID, INVITEE_ID))
                .thenReturn(Optional.empty());
    }

    private CareGroupMember captureAndReturnSaved() {
        when(memberRepository.save(any(CareGroupMember.class))).thenAnswer(inv -> {
            CareGroupMember m = inv.getArgument(0);
            m.setId(UUID.randomUUID());
            return m;
        });
        return null;
    }

    // ── TC-001: Happy path PHONE ─────────────────────────────────────────────

    @Test
    void inviteFamilyMember_phoneChannel_createsPendingRow() {
        stubActiveGroup();
        stubOwnerCheck(true);
        stubPendingCount(0);
        stubInviteePhone(true);
        stubNoDuplicatePending();
        captureAndReturnSaved();
        when(tokenGenerator.generate()).thenReturn(TOKEN);

        InviteFamilyMemberRequest request = new InviteFamilyMemberRequest(InviteChannel.PHONE, PHONE);
        InviteFamilyMemberResponse response = service.inviteFamilyMember(GROUP_ID, request, OWNER_ID);

        ArgumentCaptor<CareGroupMember> captor = ArgumentCaptor.forClass(CareGroupMember.class);
        verify(memberRepository).save(captor.capture());
        CareGroupMember saved = captor.getValue();

        assertThat(saved.getInviteStatus()).isEqualTo(InviteStatus.PENDING);
        assertThat(saved.getInviteChannel()).isEqualTo(InviteChannel.PHONE);
        assertThat(saved.getInvitedPhone()).isEqualTo(PHONE);
        assertThat(saved.getInviteToken()).isEqualTo(TOKEN);
        assertThat(saved.getInviteExpiresAt()).isNotNull();

        assertThat(response.getCareGroupMemberId()).isNotNull();
        assertThat(response.getChannel()).isEqualTo(InviteChannel.PHONE);
        assertThat(response.getInviteToken()).isEqualTo(TOKEN);
        Instant expectedExpiry = Instant.now().plus(7, ChronoUnit.DAYS);
        assertThat(response.getInviteExpiresAt()).isBetween(
                expectedExpiry.minus(60, ChronoUnit.SECONDS),
                expectedExpiry.plus(60, ChronoUnit.SECONDS));
    }

    // ── TC-002: Happy path LINK ──────────────────────────────────────────────

    @Test
    void inviteFamilyMember_linkChannel_returnsTokenWithoutRow() {
        stubActiveGroup();
        stubOwnerCheck(true);
        stubPendingCount(0);
        when(tokenGenerator.generate()).thenReturn(TOKEN);

        InviteFamilyMemberRequest request = new InviteFamilyMemberRequest(InviteChannel.LINK, null);
        InviteFamilyMemberResponse response = service.inviteFamilyMember(GROUP_ID, request, OWNER_ID);

        verify(userRepository, never()).findByPhone(any());
        verify(memberRepository, never()).save(any());

        assertThat(response.getCareGroupMemberId()).isNull();
        assertThat(response.getChannel()).isEqualTo(InviteChannel.LINK);
        assertThat(response.getInviteToken()).isEqualTo(TOKEN);
        assertThat(response.getInvitedPhone()).isNull();
    }

    // ── TC-003: Happy path QR ────────────────────────────────────────────────

    @Test
    void inviteFamilyMember_qrChannel_returnsTokenWithoutRow() {
        stubActiveGroup();
        stubOwnerCheck(true);
        stubPendingCount(0);
        when(tokenGenerator.generate()).thenReturn(TOKEN);

        InviteFamilyMemberRequest request = new InviteFamilyMemberRequest(InviteChannel.QR, null);
        InviteFamilyMemberResponse response = service.inviteFamilyMember(GROUP_ID, request, OWNER_ID);

        verify(memberRepository, never()).save(any());
        assertThat(response.getCareGroupMemberId()).isNull();
        assertThat(response.getChannel()).isEqualTo(InviteChannel.QR);
        assertThat(response.getInviteToken()).isEqualTo(TOKEN);
    }

    // ── TC-004: Group not found ──────────────────────────────────────────────

    @Test
    void inviteFamilyMember_groupNotFound_throws404() {
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.inviteFamilyMember(GROUP_ID, new InviteFamilyMemberRequest(InviteChannel.LINK, null), OWNER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(be.getCode()).isEqualTo("FAM-005");
                });
        verify(memberRepository, never()).save(any());
        verify(tokenGenerator, never()).generate();
    }

    // ── TC-005: Non-member caller ────────────────────────────────────────────

    @Test
    void inviteFamilyMember_nonMemberCaller_throws403() {
        stubActiveGroup();
        when(authorizationPolicy.isOwner(GROUP_ID, OWNER_ID)).thenReturn(false);

        assertThatThrownBy(() ->
                service.inviteFamilyMember(GROUP_ID, new InviteFamilyMemberRequest(InviteChannel.LINK, null), OWNER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(be.getCode()).isEqualTo("FAM-012");
                });
        verify(tokenGenerator, never()).generate();
    }

    // ── TC-006: ACCEPTED MEMBER role (not OWNER) ─────────────────────────────

    @Test
    void inviteFamilyMember_acceptedMemberNotOwner_throws403() {
        stubActiveGroup();
        when(authorizationPolicy.isOwner(GROUP_ID, OWNER_ID)).thenReturn(false);

        assertThatThrownBy(() ->
                service.inviteFamilyMember(GROUP_ID, new InviteFamilyMemberRequest(InviteChannel.LINK, null), OWNER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(be.getCode()).isEqualTo("FAM-012");
                });
    }

    // ── TC-007: VIEWER role ──────────────────────────────────────────────────

    @Test
    void inviteFamilyMember_viewerRole_throws403() {
        stubActiveGroup();
        when(authorizationPolicy.isOwner(GROUP_ID, OWNER_ID)).thenReturn(false);

        assertThatThrownBy(() ->
                service.inviteFamilyMember(GROUP_ID, new InviteFamilyMemberRequest(InviteChannel.LINK, null), OWNER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("FAM-012"));
    }

    // ── TC-009: PHONE channel with null phone ────────────────────────────────

    @Test
    void inviteFamilyMember_phoneChannelMissingPhone_throws400() {
        stubActiveGroup();
        stubOwnerCheck(true);
        stubPendingCount(0);

        assertThatThrownBy(() ->
                service.inviteFamilyMember(GROUP_ID, new InviteFamilyMemberRequest(InviteChannel.PHONE, null), OWNER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(be.getCode()).isEqualTo("FAM-010");
                });
        verify(userRepository, never()).findByPhone(any());
    }

    // ── TC-011: Phone not registered ─────────────────────────────────────────

    @Test
    void inviteFamilyMember_phoneNotRegistered_throws404() {
        stubActiveGroup();
        stubOwnerCheck(true);
        stubPendingCount(0);
        when(userRepository.findByPhone("+84900000000")).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.inviteFamilyMember(GROUP_ID, new InviteFamilyMemberRequest(InviteChannel.PHONE, "+84900000000"), OWNER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(be.getCode()).isEqualTo("FAM-014");
                });
        verify(memberRepository, never()).save(any());
    }

    // ── TC-012: Duplicate PENDING invite ─────────────────────────────────────

    @Test
    void inviteFamilyMember_duplicatePending_throws409() {
        stubActiveGroup();
        stubOwnerCheck(true);
        stubPendingCount(0);
        stubInviteePhone(true);
        when(memberRepository.existsByCareGroupIdAndUserIdAndInviteStatus(
                GROUP_ID, INVITEE_ID, InviteStatus.PENDING)).thenReturn(true);

        assertThatThrownBy(() ->
                service.inviteFamilyMember(GROUP_ID, new InviteFamilyMemberRequest(InviteChannel.PHONE, PHONE), OWNER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(be.getCode()).isEqualTo("FAM-011");
                });
    }

    // ── TC-013: Invitee already ACCEPTED ─────────────────────────────────────

    @Test
    void inviteFamilyMember_alreadyAcceptedMember_throws409() {
        stubActiveGroup();
        stubOwnerCheck(true);
        stubPendingCount(0);
        stubInviteePhone(true);
        when(memberRepository.existsByCareGroupIdAndUserIdAndInviteStatus(
                GROUP_ID, INVITEE_ID, InviteStatus.PENDING)).thenReturn(false);
        CareGroupMember existing = CareGroupTestFactory.makeCareGroupMember(m -> {
            m.setCareGroupId(GROUP_ID);
            m.setUserId(INVITEE_ID);
            m.setInviteStatus(InviteStatus.ACCEPTED);
        });
        when(memberRepository.findByCareGroupIdAndUserId(GROUP_ID, INVITEE_ID))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() ->
                service.inviteFamilyMember(GROUP_ID, new InviteFamilyMemberRequest(InviteChannel.PHONE, PHONE), OWNER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(be.getCode()).isEqualTo("FAM-011");
                });
    }

    // ── TC-016: 20th pending invite (boundary — succeeds) ────────────────────

    @Test
    void inviteFamilyMember_exactlyTwentiethPending_succeeds() {
        stubActiveGroup();
        stubOwnerCheck(true);
        stubPendingCount(19);
        stubInviteePhone(true);
        stubNoDuplicatePending();
        captureAndReturnSaved();
        when(tokenGenerator.generate()).thenReturn(TOKEN);

        InviteFamilyMemberResponse response = service.inviteFamilyMember(
                GROUP_ID, new InviteFamilyMemberRequest(InviteChannel.PHONE, PHONE), OWNER_ID);

        assertThat(response).isNotNull();
        verify(memberRepository).save(any(CareGroupMember.class));
    }

    // ── TC-017: 21st pending invite (boundary — rejected) ────────────────────

    @Test
    void inviteFamilyMember_twentyFirstPending_throws409() {
        stubActiveGroup();
        stubOwnerCheck(true);
        stubPendingCount(20);

        assertThatThrownBy(() ->
                service.inviteFamilyMember(GROUP_ID, new InviteFamilyMemberRequest(InviteChannel.PHONE, PHONE), OWNER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(be.getCode()).isEqualTo("FAM-013");
                });
        verify(memberRepository, never()).save(any());
    }

    // ── TC-018: FamilyMemberInvited event published with hashed token ─────────

    @Test
    void inviteFamilyMember_publishesEventWithHashedToken() {
        stubActiveGroup();
        stubOwnerCheck(true);
        stubPendingCount(0);
        stubInviteePhone(true);
        stubNoDuplicatePending();
        captureAndReturnSaved();
        when(tokenGenerator.generate()).thenReturn(TOKEN);

        service.inviteFamilyMember(GROUP_ID, new InviteFamilyMemberRequest(InviteChannel.PHONE, PHONE), OWNER_ID);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        Object event = eventCaptor.getValue();
        assertThat(event).isInstanceOf(FamilyMemberInvited.class);
        FamilyMemberInvited familyEvent = (FamilyMemberInvited) event;

        String tokenHash = familyEvent.payload().inviteTokenHash();
        assertThat(tokenHash).matches("^[a-f0-9]{64}$");
        assertThat(familyEvent.payload().inviteTokenHash()).doesNotContain(TOKEN);
    }

    // ── TC-019: Audit log created ─────────────────────────────────────────────

    @Test
    void inviteFamilyMember_logsAuditEntry() {
        stubActiveGroup();
        stubOwnerCheck(true);
        stubPendingCount(0);
        stubInviteePhone(true);
        stubNoDuplicatePending();
        captureAndReturnSaved();
        when(tokenGenerator.generate()).thenReturn(TOKEN);

        service.inviteFamilyMember(GROUP_ID, new InviteFamilyMemberRequest(InviteChannel.PHONE, PHONE), OWNER_ID);

        verify(auditService, times(1)).log(
                eq(com.carebridge.backend.audit.entity.AuditAction.CARE_GROUP_MEMBER_INVITED),
                eq(OWNER_ID),
                anyString(),
                anyString(),
                anyString());
    }
}
