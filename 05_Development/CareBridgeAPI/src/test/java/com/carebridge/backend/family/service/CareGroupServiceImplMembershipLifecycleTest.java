package com.carebridge.backend.family.service;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.family.dto.LeaveCareGroupResponse;
import com.carebridge.backend.family.dto.RemoveMemberResponse;
import com.carebridge.backend.family.dto.RevokeInvitationResponse;
import com.carebridge.backend.family.entity.CareGroup;
import com.carebridge.backend.family.entity.CareGroupMember;
import com.carebridge.backend.family.entity.GroupMemberRole;
import com.carebridge.backend.family.entity.InviteStatus;
import com.carebridge.backend.family.event.CareGroupMemberLeft;
import com.carebridge.backend.family.event.CareTaskReassigned;
import com.carebridge.backend.family.policy.CareGroupAuthorizationPolicy;
import com.carebridge.backend.family.repository.CareGroupMemberRepository;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.family.repository.CareTaskRepository;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.carebridge.backend.family.CareGroupTestFactory.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CareGroupServiceImplMembershipLifecycleTest {

    private static final UUID INVITEE_ID = UUID.fromString("eeeeeeee-0000-0000-0000-000000000005");

    @Mock private CareGroupRepository groupRepository;
    @Mock private CareGroupMemberRepository memberRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditService auditService;
    @Mock private CareGroupAuthorizationPolicy authorizationPolicy;
    @Mock private InviteTokenGenerator tokenGenerator;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private FcmService fcmService;
    @Mock private DeviceTokenRepository deviceTokenRepository;
    @Mock private CareTaskRepository taskRepository;
    @InjectMocks private CareGroupServiceImpl service;

    @Test
    void revokeInvitation_ownerRevokesPendingInvite_setsRevokedAndAudits() {
        stubGroup();
        CareGroupMember owner = member(OWNER_ID, GroupMemberRole.OWNER, InviteStatus.ACCEPTED);
        CareGroupMember invitee = member(INVITEE_ID, GroupMemberRole.MEMBER, InviteStatus.PENDING);
        when(memberRepository.findByCareGroupIdAndUserId(GROUP_ID, OWNER_ID)).thenReturn(Optional.of(owner));
        when(memberRepository.findFirstByCareGroupIdAndUserIdAndInviteStatus(
                GROUP_ID, INVITEE_ID, InviteStatus.PENDING)).thenReturn(Optional.of(invitee));
        when(memberRepository.save(invitee)).thenReturn(invitee);

        RevokeInvitationResponse response = service.revokeInvitation(GROUP_ID, INVITEE_ID, OWNER_ID);

        assertThat(invitee.getInviteStatus()).isEqualTo(InviteStatus.REVOKED);
        assertThat(response.getInviteStatus()).isEqualTo("REVOKED");
        verify(auditService).log(eq(AuditAction.CARE_GROUP_INVITE_REVOKED), eq(OWNER_ID),
                eq("CareGroup"), eq(GROUP_ID.toString()), contains(INVITEE_ID.toString()));
    }

    @Test
    void revokeInvitation_nonOwnerAcceptedMember_throwsFam050AndDoesNotSave() {
        stubGroup();
        CareGroupMember nonOwner = member(MEMBER_ID, GroupMemberRole.MEMBER, InviteStatus.ACCEPTED);
        when(memberRepository.findByCareGroupIdAndUserId(GROUP_ID, MEMBER_ID)).thenReturn(Optional.of(nonOwner));

        assertBusinessException(
                () -> service.revokeInvitation(GROUP_ID, INVITEE_ID, MEMBER_ID),
                HttpStatus.FORBIDDEN,
                "FAM-050");

        verify(memberRepository, never()).save(any());
        verify(memberRepository, never()).findFirstByCareGroupIdAndUserIdAndInviteStatus(
                GROUP_ID, INVITEE_ID, InviteStatus.PENDING);
        verifyNoInteractions(auditService);
    }

    @Test
    void revokeInvitation_targetNotFound_throwsFam051() {
        stubGroup();
        CareGroupMember owner = member(OWNER_ID, GroupMemberRole.OWNER, InviteStatus.ACCEPTED);
        when(memberRepository.findByCareGroupIdAndUserId(GROUP_ID, OWNER_ID)).thenReturn(Optional.of(owner));
        when(memberRepository.findFirstByCareGroupIdAndUserIdAndInviteStatus(
                GROUP_ID, INVITEE_ID, InviteStatus.PENDING)).thenReturn(Optional.empty());

        assertBusinessException(
                () -> service.revokeInvitation(GROUP_ID, INVITEE_ID, OWNER_ID),
                HttpStatus.NOT_FOUND,
                "FAM-051");

        verify(memberRepository, never()).save(any());
        verifyNoInteractions(auditService);
    }

    @Test
    void revokeInvitation_targetAccepted_throwsFam052AndDoesNotSave() {
        assertNonPendingTargetRejected(InviteStatus.ACCEPTED);
    }

    @Test
    void revokeInvitation_targetAlreadyRevoked_throwsFam052AndDoesNotSave() {
        assertNonPendingTargetRejected(InviteStatus.REVOKED);
    }

    @Test
    void revokeInvitation_selfTarget_throwsFam053BeforeOwnerLookup() {
        stubGroup();

        assertBusinessException(
                () -> service.revokeInvitation(GROUP_ID, OWNER_ID, OWNER_ID),
                HttpStatus.BAD_REQUEST,
                "FAM-053");

        verify(memberRepository, never()).findByCareGroupIdAndUserId(any(), any());
        verify(memberRepository, never()).save(any());
        verifyNoInteractions(auditService);
    }

    @Test
    void revokeInvitation_groupNotFound_throwsFam005() {
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.empty());

        assertBusinessException(
                () -> service.revokeInvitation(GROUP_ID, INVITEE_ID, OWNER_ID),
                HttpStatus.NOT_FOUND,
                "FAM-005");

        verify(memberRepository, never()).findByCareGroupIdAndUserId(any(), any());
        verifyNoInteractions(auditService);
    }

    @Test
    void revokeInvitation_usesRevokedAuditActionNotDeclined() {
        stubGroup();
        CareGroupMember owner = member(OWNER_ID, GroupMemberRole.OWNER, InviteStatus.ACCEPTED);
        CareGroupMember invitee = member(INVITEE_ID, GroupMemberRole.MEMBER, InviteStatus.PENDING);
        when(memberRepository.findByCareGroupIdAndUserId(GROUP_ID, OWNER_ID)).thenReturn(Optional.of(owner));
        when(memberRepository.findFirstByCareGroupIdAndUserIdAndInviteStatus(
                GROUP_ID, INVITEE_ID, InviteStatus.PENDING)).thenReturn(Optional.of(invitee));
        when(memberRepository.save(invitee)).thenReturn(invitee);

        service.revokeInvitation(GROUP_ID, INVITEE_ID, OWNER_ID);

        verify(auditService).log(eq(AuditAction.CARE_GROUP_INVITE_REVOKED), any(), any(), any(), any());
        verify(auditService, never()).log(eq(AuditAction.CARE_GROUP_INVITE_DECLINED), any(), any(), any(), any());
    }

    @Test
    void revokeInvitation_responseContainsOnlyNonPiiContractFields() {
        Set<String> fields = Stream.of(RevokeInvitationResponse.class.getDeclaredFields())
                .map(java.lang.reflect.Field::getName)
                .collect(Collectors.toSet());

        assertThat(fields).containsExactlyInAnyOrder(
                "careGroupMemberId", "groupId", "targetUserId", "inviteStatus", "revokedAt");
        assertThat(fields).doesNotContain("email", "phone", "accountId", "account_id", "invite_status");
    }

    @Test
    void removeMember_ownerRemovesAcceptedNonOwner_deletesAndAudits() {
        stubGroup();
        CareGroupMember target = member(MEMBER_ID, GroupMemberRole.MEMBER, InviteStatus.ACCEPTED);
        when(memberRepository.findByCareGroupIdAndUserId(GROUP_ID, MEMBER_ID)).thenReturn(Optional.of(target));

        RemoveMemberResponse response = service.removeMember(GROUP_ID, MEMBER_ID, OWNER_ID);

        assertThat(response.getInviteStatus()).isEqualTo("REMOVED");
        verify(memberRepository).delete(target);
        verify(taskRepository).reassignIncompleteTasks(GROUP_ID, MEMBER_ID, OWNER_ID);
        verify(auditService).log(eq(AuditAction.CARE_GROUP_MEMBER_REMOVED), eq(OWNER_ID),
                eq("CareGroup"), eq(GROUP_ID.toString()), contains(MEMBER_ID.toString()));
    }

    @Test
    void removeMember_nonOwnerAcceptedMember_throwsFam058AndDoesNotSave() {
        stubGroup();
        CareGroupMember nonOwner = member(ASSIGNEE_ID, GroupMemberRole.MEMBER, InviteStatus.ACCEPTED);
        when(memberRepository.findByCareGroupIdAndUserId(GROUP_ID, ASSIGNEE_ID)).thenReturn(Optional.of(nonOwner));

        assertBusinessException(
                () -> service.removeMember(GROUP_ID, MEMBER_ID, ASSIGNEE_ID),
                HttpStatus.FORBIDDEN,
                "FAM-058");

        verify(memberRepository, never()).save(any());
        verify(memberRepository, never()).findByCareGroupIdAndUserId(GROUP_ID, MEMBER_ID);
        verifyNoInteractions(auditService);
        verifyNoInteractions(taskRepository);
    }

    @Test
    void removeMember_targetNotFound_throwsFam059() {
        stubGroup();
        when(memberRepository.findByCareGroupIdAndUserId(GROUP_ID, MEMBER_ID)).thenReturn(Optional.empty());

        assertBusinessException(
                () -> service.removeMember(GROUP_ID, MEMBER_ID, OWNER_ID),
                HttpStatus.NOT_FOUND,
                "FAM-059");

        verify(memberRepository, never()).save(any());
        verifyNoInteractions(auditService);
        verifyNoInteractions(taskRepository);
    }

    @Test
    void removeMember_targetOwner_throwsFam061AndDoesNotSave() {
        stubGroup();
        CareGroupMember owner = member(OWNER_ID, GroupMemberRole.OWNER, InviteStatus.ACCEPTED);
        when(memberRepository.findByCareGroupIdAndUserId(GROUP_ID, OWNER_ID)).thenReturn(Optional.of(owner));

        assertBusinessException(
                () -> service.removeMember(GROUP_ID, OWNER_ID, OWNER_ID),
                HttpStatus.CONFLICT,
                "FAM-061");

        verify(memberRepository, never()).save(any());
        verifyNoInteractions(auditService);
        verifyNoInteractions(taskRepository);
    }

    @Test
    void removeMember_targetPending_throwsFam060AndDoesNotSave() {
        assertRemoveMemberNonAcceptedTargetRejected(InviteStatus.PENDING);
    }

    @Test
    void removeMember_targetAlreadyRevoked_throwsFam060AndDoesNotSave() {
        assertRemoveMemberNonAcceptedTargetRejected(InviteStatus.REVOKED);
    }

    @Test
    void removeMember_groupNotFound_throwsFam005() {
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.empty());

        assertBusinessException(
                () -> service.removeMember(GROUP_ID, MEMBER_ID, OWNER_ID),
                HttpStatus.NOT_FOUND,
                "FAM-005");

        verify(memberRepository, never()).findByCareGroupIdAndUserId(any(), any());
        verifyNoInteractions(auditService);
        verifyNoInteractions(taskRepository);
    }

    @Test
    void removeMember_usesMemberRemovedAuditActionNotInviteOrLeaveActions() {
        stubGroup();
        CareGroupMember target = member(MEMBER_ID, GroupMemberRole.MEMBER, InviteStatus.ACCEPTED);
        when(memberRepository.findByCareGroupIdAndUserId(GROUP_ID, MEMBER_ID)).thenReturn(Optional.of(target));

        service.removeMember(GROUP_ID, MEMBER_ID, OWNER_ID);

        verify(auditService).log(eq(AuditAction.CARE_GROUP_MEMBER_REMOVED), any(), any(), any(), any());
        verify(auditService, never()).log(eq(AuditAction.CARE_GROUP_INVITE_REVOKED), any(), any(), any(), any());
        verify(auditService, never()).log(eq(AuditAction.CARE_GROUP_INVITE_DECLINED), any(), any(), any(), any());
        verify(auditService, never()).log(eq(AuditAction.CARE_GROUP_MEMBER_LEFT), any(), any(), any(), any());
    }

    @Test
    void removeMember_reassignsIncompleteTasksToOwner() {
        stubGroup();
        CareGroupMember target = member(MEMBER_ID, GroupMemberRole.MEMBER, InviteStatus.ACCEPTED);
        when(memberRepository.findByCareGroupIdAndUserId(GROUP_ID, MEMBER_ID)).thenReturn(Optional.of(target));

        service.removeMember(GROUP_ID, MEMBER_ID, OWNER_ID);

        verify(taskRepository).reassignIncompleteTasks(GROUP_ID, MEMBER_ID, OWNER_ID);
    }

    @Test
    void removeMember_responseContainsOnlyNonPiiContractFields() {
        Set<String> fields = Stream.of(RemoveMemberResponse.class.getDeclaredFields())
                .map(java.lang.reflect.Field::getName)
                .collect(Collectors.toSet());

        assertThat(fields).containsExactlyInAnyOrder(
                "careGroupMemberId", "groupId", "targetUserId", "inviteStatus", "removedAt");
        assertThat(fields).doesNotContain("email", "phone", "accountId", "account_id", "invite_status");
    }

    @Test
    void removeMember_targetOwnerWithNonAcceptedStatus_stillThrowsFam061BeforeStatusGuard() {
        stubGroup();
        CareGroupMember inconsistentOwnerTarget = member(MEMBER_ID, GroupMemberRole.OWNER, InviteStatus.PENDING);
        when(memberRepository.findByCareGroupIdAndUserId(GROUP_ID, MEMBER_ID)).thenReturn(Optional.of(inconsistentOwnerTarget));

        assertBusinessException(
                () -> service.removeMember(GROUP_ID, MEMBER_ID, OWNER_ID),
                HttpStatus.CONFLICT,
                "FAM-061");

        verify(memberRepository, never()).save(any());
        verifyNoInteractions(auditService);
    }

    @Test
    void leaveCareGroup_memberLeaves_reassignsIncompleteTasksToOwnerAndRevokesOwnRow() {
        CareGroup group = makeCareGroup(g -> {
            g.setId(GROUP_ID);
            g.setOwnerUserId(OWNER_ID);
        });
        CareGroupMember own = member(MEMBER_ID, GroupMemberRole.MEMBER, InviteStatus.ACCEPTED);
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));
        when(memberRepository.findByCareGroupIdAndUserId(GROUP_ID, MEMBER_ID)).thenReturn(Optional.of(own));
        when(taskRepository.reassignIncompleteTasks(GROUP_ID, MEMBER_ID, OWNER_ID)).thenReturn(2);
        when(memberRepository.save(own)).thenReturn(own);

        LeaveCareGroupResponse response = service.leaveCareGroup(GROUP_ID, MEMBER_ID);

        assertThat(own.getInviteStatus()).isEqualTo(InviteStatus.REVOKED);
        assertThat(response.getReassignedTaskCount()).isEqualTo(2);
        verify(auditService).log(eq(AuditAction.CARE_GROUP_MEMBER_LEFT), eq(MEMBER_ID),
                eq("CareGroup"), eq(GROUP_ID.toString()), contains("member left"));
    }

    @Test
    void leaveCareGroup_memberLeavesWithNoIncompleteTasks_returnsZeroAndDoesNotPublishTaskEvent() {
        stubGroup();
        CareGroupMember own = member(MEMBER_ID, GroupMemberRole.MEMBER, InviteStatus.ACCEPTED);
        when(memberRepository.findByCareGroupIdAndUserId(GROUP_ID, MEMBER_ID)).thenReturn(Optional.of(own));
        when(taskRepository.reassignIncompleteTasks(GROUP_ID, MEMBER_ID, OWNER_ID)).thenReturn(0);
        when(memberRepository.save(own)).thenReturn(own);

        LeaveCareGroupResponse response = service.leaveCareGroup(GROUP_ID, MEMBER_ID);

        assertThat(response.getReassignedTaskCount()).isZero();
        verify(eventPublisher).publishEvent(isA(CareGroupMemberLeft.class));
        verify(eventPublisher, never()).publishEvent(isA(CareTaskReassigned.class));
    }

    @Test
    void leaveCareGroup_groupNotFound_throwsFam005() {
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.empty());

        assertBusinessException(
                () -> service.leaveCareGroup(GROUP_ID, MEMBER_ID),
                HttpStatus.NOT_FOUND,
                "FAM-005");

        verify(memberRepository, never()).findByCareGroupIdAndUserId(any(), any());
        verifyNoInteractions(taskRepository);
        verifyNoInteractions(auditService);
    }

    @Test
    void leaveCareGroup_ownerCannotLeave_throwsFam063() {
        stubGroup();
        CareGroupMember owner = member(OWNER_ID, GroupMemberRole.OWNER, InviteStatus.ACCEPTED);
        when(memberRepository.findByCareGroupIdAndUserId(GROUP_ID, OWNER_ID)).thenReturn(Optional.of(owner));

        assertThatThrownBy(() -> service.leaveCareGroup(GROUP_ID, OWNER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(be.getCode()).isEqualTo("FAM-063");
                });
        verify(taskRepository, never()).reassignIncompleteTasks(any(), any(), any());
    }

    @Test
    void leaveCareGroup_missingMembership_throwsFam064() {
        stubGroup();
        when(memberRepository.findByCareGroupIdAndUserId(GROUP_ID, MEMBER_ID)).thenReturn(Optional.empty());

        assertBusinessException(
                () -> service.leaveCareGroup(GROUP_ID, MEMBER_ID),
                HttpStatus.CONFLICT,
                "FAM-064");

        verify(taskRepository, never()).reassignIncompleteTasks(any(), any(), any());
        verify(memberRepository, never()).save(any());
        verifyNoInteractions(auditService);
    }

    @Test
    void leaveCareGroup_pendingMembership_throwsFam064() {
        assertLeaveCareGroupNonAcceptedMembershipRejected(InviteStatus.PENDING);
    }

    @Test
    void leaveCareGroup_revokedMembership_throwsFam064AndDoesNotReassignAgain() {
        assertLeaveCareGroupNonAcceptedMembershipRejected(InviteStatus.REVOKED);
    }

    @Test
    void leaveCareGroup_membershipFlipIsAppendOnlySaveNotDelete() {
        stubGroup();
        CareGroupMember own = member(MEMBER_ID, GroupMemberRole.MEMBER, InviteStatus.ACCEPTED);
        when(memberRepository.findByCareGroupIdAndUserId(GROUP_ID, MEMBER_ID)).thenReturn(Optional.of(own));
        when(taskRepository.reassignIncompleteTasks(GROUP_ID, MEMBER_ID, OWNER_ID)).thenReturn(0);
        when(memberRepository.save(own)).thenReturn(own);

        service.leaveCareGroup(GROUP_ID, MEMBER_ID);

        assertThat(own.getInviteStatus()).isEqualTo(InviteStatus.REVOKED);
        verify(memberRepository).save(own);
        verify(memberRepository, never()).delete(any());
        verify(memberRepository, never()).deleteById(any());
    }

    @Test
    void leaveCareGroup_publishesMemberLeftAndTaskReassignedEvents() {
        stubGroup();
        CareGroupMember own = member(MEMBER_ID, GroupMemberRole.MEMBER, InviteStatus.ACCEPTED);
        when(memberRepository.findByCareGroupIdAndUserId(GROUP_ID, MEMBER_ID)).thenReturn(Optional.of(own));
        when(taskRepository.reassignIncompleteTasks(GROUP_ID, MEMBER_ID, OWNER_ID)).thenReturn(2);
        when(memberRepository.save(own)).thenReturn(own);

        service.leaveCareGroup(GROUP_ID, MEMBER_ID);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, times(2)).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getAllValues()).anySatisfy(event -> {
            assertThat(event).isInstanceOf(CareGroupMemberLeft.class);
            CareGroupMemberLeft memberLeft = (CareGroupMemberLeft) event;
            assertThat(memberLeft.payload().careGroupId()).isEqualTo(GROUP_ID);
            assertThat(memberLeft.payload().userId()).isEqualTo(MEMBER_ID);
            assertThat(memberLeft.payload().reassignedTaskCount()).isEqualTo(2);
        });
        assertThat(eventCaptor.getAllValues()).anySatisfy(event -> {
            assertThat(event).isInstanceOf(CareTaskReassigned.class);
            CareTaskReassigned reassigned = (CareTaskReassigned) event;
            assertThat(reassigned.payload().careGroupId()).isEqualTo(GROUP_ID);
            assertThat(reassigned.payload().fromUserId()).isEqualTo(MEMBER_ID);
            assertThat(reassigned.payload().toUserId()).isEqualTo(OWNER_ID);
            assertThat(reassigned.payload().reassignedTaskCount()).isEqualTo(2);
        });
    }

    @Test
    void leaveCareGroup_usesActualGroupOwnerAsReassignmentTarget() {
        UUID alternateOwnerId = UUID.fromString("99999999-0000-0000-0000-000000000999");
        CareGroup group = makeCareGroup(g -> {
            g.setId(GROUP_ID);
            g.setOwnerUserId(alternateOwnerId);
        });
        CareGroupMember own = member(MEMBER_ID, GroupMemberRole.MEMBER, InviteStatus.ACCEPTED);
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));
        when(memberRepository.findByCareGroupIdAndUserId(GROUP_ID, MEMBER_ID)).thenReturn(Optional.of(own));
        when(taskRepository.reassignIncompleteTasks(GROUP_ID, MEMBER_ID, alternateOwnerId)).thenReturn(1);
        when(memberRepository.save(own)).thenReturn(own);

        LeaveCareGroupResponse response = service.leaveCareGroup(GROUP_ID, MEMBER_ID);

        assertThat(response.getReassignedTaskCount()).isEqualTo(1);
        verify(taskRepository).reassignIncompleteTasks(GROUP_ID, MEMBER_ID, alternateOwnerId);
        verify(taskRepository, never()).reassignIncompleteTasks(eq(GROUP_ID), eq(MEMBER_ID), eq(OWNER_ID));
    }

    @Test
    void leaveCareGroup_reassignmentFailurePreventsMembershipFlip() {
        stubGroup();
        CareGroupMember own = member(MEMBER_ID, GroupMemberRole.MEMBER, InviteStatus.ACCEPTED);
        when(memberRepository.findByCareGroupIdAndUserId(GROUP_ID, MEMBER_ID)).thenReturn(Optional.of(own));
        when(taskRepository.reassignIncompleteTasks(GROUP_ID, MEMBER_ID, OWNER_ID))
                .thenThrow(new RuntimeException("bulk update failed"));

        assertThatThrownBy(() -> service.leaveCareGroup(GROUP_ID, MEMBER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("bulk update failed");

        assertThat(own.getInviteStatus()).isEqualTo(InviteStatus.ACCEPTED);
        verify(memberRepository, never()).save(any());
        verifyNoInteractions(auditService);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void leaveCareGroup_responseContainsOnlyContractFields() {
        Set<String> fields = Stream.of(LeaveCareGroupResponse.class.getDeclaredFields())
                .map(java.lang.reflect.Field::getName)
                .collect(Collectors.toSet());

        assertThat(fields).containsExactlyInAnyOrder("groupId", "leftAt", "reassignedTaskCount");
        assertThat(fields).doesNotContain(
                "email", "phone", "careGroupMemberId", "careTaskId", "assignedTo", "title", "description");
    }

    private void stubGroup() {
        CareGroup group = makeCareGroup(g -> {
            g.setId(GROUP_ID);
            g.setOwnerUserId(OWNER_ID);
        });
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));
    }

    /**
     * Revoke resolves its target with a status-scoped finder, so a membership that is no longer
     * PENDING is simply not found: absent / already accepted / already revoked all end in the
     * single FAM-051 404 outcome, and nothing is written.
     */
    private void assertNonPendingTargetRejected(InviteStatus targetStatus) {
        stubGroup();
        CareGroupMember owner = member(OWNER_ID, GroupMemberRole.OWNER, InviteStatus.ACCEPTED);
        when(memberRepository.findByCareGroupIdAndUserId(GROUP_ID, OWNER_ID)).thenReturn(Optional.of(owner));
        when(memberRepository.findFirstByCareGroupIdAndUserIdAndInviteStatus(
                GROUP_ID, INVITEE_ID, InviteStatus.PENDING)).thenReturn(Optional.empty());

        assertBusinessException(
                () -> service.revokeInvitation(GROUP_ID, INVITEE_ID, OWNER_ID),
                HttpStatus.NOT_FOUND,
                "FAM-051");

        verify(memberRepository, never()).save(any());
        verifyNoInteractions(auditService);
    }

    private void assertRemoveMemberNonAcceptedTargetRejected(InviteStatus targetStatus) {
        stubGroup();
        CareGroupMember target = member(MEMBER_ID, GroupMemberRole.MEMBER, targetStatus);
        when(memberRepository.findByCareGroupIdAndUserId(GROUP_ID, MEMBER_ID)).thenReturn(Optional.of(target));

        assertBusinessException(
                () -> service.removeMember(GROUP_ID, MEMBER_ID, OWNER_ID),
                HttpStatus.CONFLICT,
                "FAM-060");

        verify(memberRepository, never()).save(any());
        verifyNoInteractions(auditService);
        verifyNoInteractions(taskRepository);
    }

    private void assertLeaveCareGroupNonAcceptedMembershipRejected(InviteStatus status) {
        stubGroup();
        CareGroupMember own = member(MEMBER_ID, GroupMemberRole.MEMBER, status);
        when(memberRepository.findByCareGroupIdAndUserId(GROUP_ID, MEMBER_ID)).thenReturn(Optional.of(own));

        assertBusinessException(
                () -> service.leaveCareGroup(GROUP_ID, MEMBER_ID),
                HttpStatus.CONFLICT,
                "FAM-064");

        verify(taskRepository, never()).reassignIncompleteTasks(any(), any(), any());
        verify(memberRepository, never()).save(any());
        verifyNoInteractions(auditService);
    }

    private void assertBusinessException(Runnable action, HttpStatus status, String code) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getHttpStatus()).isEqualTo(status);
                    assertThat(be.getCode()).isEqualTo(code);
                });
    }

    private CareGroupMember member(UUID userId, GroupMemberRole role, InviteStatus status) {
        return makeCareGroupMember(m -> {
            m.setCareGroupId(GROUP_ID);
            m.setUserId(userId);
            m.setMemberRole(role);
            m.setInviteStatus(status);
        });
    }
}
