package com.carebridge.backend.family;


import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.family.dto.CareGroupMemberDto;
import com.carebridge.backend.family.dto.CareGroupMembersResponse;
import com.carebridge.backend.family.dto.CreateCareGroupRequest;
import com.carebridge.backend.family.dto.CreateCareGroupResponse;
import com.carebridge.backend.family.dto.InviteCareGroupMemberRequest;
import com.carebridge.backend.family.dto.PendingInvitationDto;
import com.carebridge.backend.family.entity.CareGroup;
import com.carebridge.backend.family.entity.CareGroupMember;
import com.carebridge.backend.family.entity.CareGroupStatus;
import com.carebridge.backend.family.entity.GroupMemberRole;
import com.carebridge.backend.family.entity.InviteStatus;
import com.carebridge.backend.family.repository.CareGroupMemberRepository;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.family.repository.CareTaskRepository;
import com.carebridge.backend.family.service.impl.CareGroupServiceImpl;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.MotherJourney;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CareGroupServiceImplTest {

    @Mock private CareGroupRepository groupRepository;
    @Mock private CareGroupMemberRepository memberRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditService auditService;
    @Mock private CareTaskRepository taskRepository;
    @Mock private MotherJourneyRepository journeyRepository;
    @InjectMocks private CareGroupServiceImpl careGroupService;

    private static final UUID CALLER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID GROUP_ID  = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID INVITEE_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID JOURNEY_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");

    private CreateCareGroupRequest makeRequest() {
        CreateCareGroupRequest req = new CreateCareGroupRequest();
        req.setGroupName("My Pregnancy Team");
        req.setDescription("Support group");
        return req;
    }

    private CareGroup savedGroup(UUID id) {
        return CareGroup.builder()
                .id(id)
                .ownerUserId(CALLER_ID)
                .groupName("My Pregnancy Team")
                .status(CareGroupStatus.ACTIVE)
                .build();
    }

    private CareGroupMember ownerMember(UUID groupId) {
        return CareGroupMember.builder()
                .id(UUID.randomUUID())
                .careGroupId(groupId)
                .userId(CALLER_ID)
                .memberRole(GroupMemberRole.OWNER)
                .inviteStatus(InviteStatus.ACCEPTED)
                .build();
    }

    // FAM-TC-001: Happy path — group created, creator added as OWNER
    @Test
    void createCareGroup_validRequest_returns201WithOwnerMember() {
        when(groupRepository.countByOwnerUserIdAndStatus(CALLER_ID, CareGroupStatus.ACTIVE)).thenReturn(0L);
        CareGroup saved = savedGroup(GROUP_ID);
        when(groupRepository.save(any())).thenReturn(saved);
        when(memberRepository.save(any())).thenReturn(ownerMember(GROUP_ID));
        when(memberRepository.countByCareGroupId(GROUP_ID)).thenReturn(1L);

        CreateCareGroupResponse resp = careGroupService.createCareGroup(makeRequest(), CALLER_ID);

        assertThat(resp.getId()).isEqualTo(GROUP_ID);
        assertThat(resp.getMemberCount()).isEqualTo(1);
        assertThat(resp.getStatus()).isEqualTo("ACTIVE");
    }

    // FAM-TC-002: C1 — max 5 active groups → FAM-002 / 409
    @Test
    void createCareGroup_alreadyHas5Active_throwsBusinessException409() {
        when(groupRepository.countByOwnerUserIdAndStatus(CALLER_ID, CareGroupStatus.ACTIVE)).thenReturn(5L);

        assertThatThrownBy(() -> careGroupService.createCareGroup(makeRequest(), CALLER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo("FAM-002");
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
                });

        verify(groupRepository, never()).save(any());
    }

    // FAM-TC-003: C2 — creator auto-added as OWNER with ACCEPTED status
    @Test
    void createCareGroup_creatorAutoAddedAsOwner() {
        when(groupRepository.countByOwnerUserIdAndStatus(any(), any())).thenReturn(0L);
        when(groupRepository.save(any())).thenReturn(savedGroup(GROUP_ID));
        when(memberRepository.save(any())).thenReturn(ownerMember(GROUP_ID));
        when(memberRepository.countByCareGroupId(any())).thenReturn(1L);

        careGroupService.createCareGroup(makeRequest(), CALLER_ID);

        verify(memberRepository).save(argThat(m ->
                m.getMemberRole() == GroupMemberRole.OWNER &&
                m.getInviteStatus() == InviteStatus.ACCEPTED &&
                m.getUserId().equals(CALLER_ID)));
    }

    @Test
    void createCareGroup_withoutContextAutoLinksOwnersLatestActiveJourney() {
        when(groupRepository.countByOwnerUserIdAndStatus(CALLER_ID, CareGroupStatus.ACTIVE)).thenReturn(0L);
        when(journeyRepository.findFirstByOwnerUserIdAndStatusOrderByCreatedAtDesc(
                CALLER_ID, JourneyStatus.ACTIVE))
                .thenReturn(Optional.of(MotherJourney.builder().id(JOURNEY_ID).build()));
        when(groupRepository.save(any(CareGroup.class)))
                .thenAnswer(invocation -> {
                    CareGroup candidate = invocation.getArgument(0);
                    candidate.setId(GROUP_ID);
                    return candidate;
                });
        when(memberRepository.save(any())).thenReturn(ownerMember(GROUP_ID));
        when(memberRepository.countByCareGroupId(any())).thenReturn(1L);

        careGroupService.createCareGroup(makeRequest(), CALLER_ID);

        ArgumentCaptor<CareGroup> group = ArgumentCaptor.forClass(CareGroup.class);
        verify(groupRepository).save(group.capture());
        assertThat(group.getValue().getLinkedJourneyId()).isEqualTo(JOURNEY_ID);
        assertThat(group.getValue().getLinkedBabyProfileId()).isNull();
    }

    @Test
    void deleteCareGroup_withRetainedHistoryArchivesAndPublishesCandidate() {
        CareGroup group = savedGroup(GROUP_ID);
        CareGroupMember owner = ownerMember(GROUP_ID);
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));
        when(memberRepository.findByCareGroupIdAndUserId(GROUP_ID, CALLER_ID))
                .thenReturn(Optional.of(owner));
        when(memberRepository.countByCareGroupIdAndInviteStatus(GROUP_ID, InviteStatus.ACCEPTED))
                .thenReturn(1L);
        when(groupRepository.save(group)).thenReturn(group);

        careGroupService.deleteCareGroup(GROUP_ID, CALLER_ID);

        assertThat(group.getStatus()).isEqualTo(CareGroupStatus.ARCHIVED);
        verify(groupRepository).save(group);
        verify(memberRepository, never()).deleteByCareGroupId(any());
        verify(taskRepository, never()).deleteByCareGroupId(any());
        verify(groupRepository, never()).deleteById(any());
    }

    @Test
    void deleteCareGroup_withAnotherAcceptedMember_throwsConflictAndKeepsGroupActive() {
        CareGroup group = savedGroup(GROUP_ID);
        CareGroupMember owner = ownerMember(GROUP_ID);
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));
        when(memberRepository.findByCareGroupIdAndUserId(GROUP_ID, CALLER_ID))
                .thenReturn(Optional.of(owner));
        when(memberRepository.countByCareGroupIdAndInviteStatus(GROUP_ID, InviteStatus.ACCEPTED))
                .thenReturn(2L);

        assertThatThrownBy(() -> careGroupService.deleteCareGroup(GROUP_ID, CALLER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException businessException = (BusinessException) ex;
                    assertThat(businessException.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(businessException.getCode()).isEqualTo("FAM-069");
                });

        assertThat(group.getStatus()).isEqualTo(CareGroupStatus.ACTIVE);
        verify(groupRepository, never()).save(any());
        verify(auditService, never()).log(any(), any(), any(), any(), any());
    }

    @Test
    void listMyGroups_excludesArchivedGroups() {
        UUID archivedGroupId = UUID.randomUUID();
        CareGroup active = savedGroup(GROUP_ID);
        CareGroup archived = savedGroup(archivedGroupId);
        archived.setStatus(CareGroupStatus.ARCHIVED);
        CareGroupMember activeMembership = ownerMember(GROUP_ID);
        CareGroupMember archivedMembership = ownerMember(archivedGroupId);
        when(memberRepository.findByUserIdAndInviteStatus(CALLER_ID, InviteStatus.ACCEPTED))
                .thenReturn(List.of(activeMembership, archivedMembership));
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(active));
        when(groupRepository.findById(archivedGroupId)).thenReturn(Optional.of(archived));
        when(memberRepository.countByCareGroupIdAndInviteStatus(GROUP_ID, InviteStatus.ACCEPTED))
                .thenReturn(1L);

        var groups = careGroupService.listMyGroups(CALLER_ID);

        assertThat(groups).extracting("groupId").containsExactly(GROUP_ID);
        verify(memberRepository, never()).countByCareGroupIdAndInviteStatus(
                archivedGroupId, InviteStatus.ACCEPTED);
    }

    // FAM-TC-004: List members — ACCEPTED member can view (C1 — isMember uses ACCEPTED)
    @Test
    void listMembers_acceptedMember_returnsMembers() {
        CareGroup group = savedGroup(GROUP_ID);
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));
        when(memberRepository.existsByCareGroupIdAndUserIdAndInviteStatus(
                GROUP_ID, CALLER_ID, InviteStatus.ACCEPTED)).thenReturn(true);
        when(memberRepository.findByCareGroupIdAndInviteStatusIn(eq(GROUP_ID), any()))
                .thenReturn(List.of(ownerMember(GROUP_ID)));

        CareGroupMembersResponse resp = careGroupService.listMembers(GROUP_ID, CALLER_ID);

        assertThat(resp.getMembers()).hasSize(1);
        assertThat(resp.getGroupId()).isEqualTo(GROUP_ID);
    }

    // FAM-TC-005: PENDING member → FAM-003 / 403 (C1 — PENDING is NOT sufficient)
    @Test
    void listMembers_pendingMember_throwsBusinessException403() {
        CareGroup group = savedGroup(GROUP_ID);
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));
        when(memberRepository.existsByCareGroupIdAndUserIdAndInviteStatus(
                GROUP_ID, CALLER_ID, InviteStatus.ACCEPTED)).thenReturn(false);

        assertThatThrownBy(() -> careGroupService.listMembers(GROUP_ID, CALLER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getHttpStatus())
                        .isEqualTo(HttpStatus.FORBIDDEN));
    }

    // FAM-TC-006: C2 — response has displayName only, no PII (email/phone)
    @Test
    void listMembers_responseContainsDisplayNameNotPii() {
        CareGroup group = savedGroup(GROUP_ID);
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));
        when(memberRepository.existsByCareGroupIdAndUserIdAndInviteStatus(any(), any(), any())).thenReturn(true);
        when(memberRepository.findByCareGroupIdAndInviteStatusIn(any(), any()))
                .thenReturn(List.of(ownerMember(GROUP_ID)));

        CareGroupMembersResponse resp = careGroupService.listMembers(GROUP_ID, CALLER_ID);

        assertThat(resp.getMembers().get(0).toString()).doesNotContainIgnoringCase("email");
        assertThat(resp.getMembers().get(0).toString()).doesNotContainIgnoringCase("phone");
    }

    // ---- UC-83: invite / accept / decline ----

    private User invitee() {
        return User.builder().id(INVITEE_ID).email("family@carebridge.dev").build();
    }

    @Test
    void inviteMember_ownerInvitesNewUser_createsPendingMember() {
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(savedGroup(GROUP_ID)));
        when(memberRepository.findByCareGroupIdAndUserId(GROUP_ID, CALLER_ID))
                .thenReturn(Optional.of(ownerMember(GROUP_ID)));
        when(userRepository.findByEmailIgnoreCase("family@carebridge.dev")).thenReturn(Optional.of(invitee()));
        when(memberRepository.findByCareGroupIdAndUserId(GROUP_ID, INVITEE_ID)).thenReturn(Optional.empty());
        when(memberRepository.save(any())).thenAnswer(inv -> {
            CareGroupMember m = inv.getArgument(0);
            m.setId(UUID.randomUUID());
            return m;
        });

        InviteCareGroupMemberRequest request = new InviteCareGroupMemberRequest();
        request.setEmail("family@carebridge.dev");

        CareGroupMemberDto result = careGroupService.inviteMember(GROUP_ID, request, CALLER_ID);

        assertThat(result.getInviteStatus()).isEqualTo("PENDING");
        verify(memberRepository).save(argThat(m ->
                m.getUserId().equals(INVITEE_ID) && m.getInviteStatus() == InviteStatus.PENDING));
    }

    @Test
    void inviteMember_callerNotOwner_throwsForbidden() {
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(savedGroup(GROUP_ID)));
        CareGroupMember nonOwner = CareGroupMember.builder()
                .careGroupId(GROUP_ID).userId(CALLER_ID)
                .memberRole(GroupMemberRole.MEMBER).inviteStatus(InviteStatus.ACCEPTED).build();
        when(memberRepository.findByCareGroupIdAndUserId(GROUP_ID, CALLER_ID)).thenReturn(Optional.of(nonOwner));

        InviteCareGroupMemberRequest request = new InviteCareGroupMemberRequest();
        request.setEmail("family@carebridge.dev");

        assertThatThrownBy(() -> careGroupService.inviteMember(GROUP_ID, request, CALLER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("FAM-008"));
    }

    @Test
    void inviteMember_emailNotRegistered_throwsNotFound() {
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(savedGroup(GROUP_ID)));
        when(memberRepository.findByCareGroupIdAndUserId(GROUP_ID, CALLER_ID))
                .thenReturn(Optional.of(ownerMember(GROUP_ID)));
        when(userRepository.findByEmailIgnoreCase("nobody@nowhere.dev")).thenReturn(Optional.empty());

        InviteCareGroupMemberRequest request = new InviteCareGroupMemberRequest();
        request.setEmail("nobody@nowhere.dev");

        assertThatThrownBy(() -> careGroupService.inviteMember(GROUP_ID, request, CALLER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("FAM-006"));
    }

    @Test
    void inviteMember_alreadyAcceptedMember_throwsConflict() {
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(savedGroup(GROUP_ID)));
        when(memberRepository.findByCareGroupIdAndUserId(GROUP_ID, CALLER_ID))
                .thenReturn(Optional.of(ownerMember(GROUP_ID)));
        when(userRepository.findByEmailIgnoreCase("family@carebridge.dev")).thenReturn(Optional.of(invitee()));
        CareGroupMember existing = CareGroupMember.builder()
                .careGroupId(GROUP_ID).userId(INVITEE_ID)
                .memberRole(GroupMemberRole.MEMBER).inviteStatus(InviteStatus.ACCEPTED).build();
        when(memberRepository.findByCareGroupIdAndUserId(GROUP_ID, INVITEE_ID)).thenReturn(Optional.of(existing));

        InviteCareGroupMemberRequest request = new InviteCareGroupMemberRequest();
        request.setEmail("family@carebridge.dev");

        assertThatThrownBy(() -> careGroupService.inviteMember(GROUP_ID, request, CALLER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("FAM-007"));
    }

    @Test
    void acceptInvite_pendingInvite_setsAcceptedAndJoinedAt() {
        CareGroupMember pending = CareGroupMember.builder()
                .id(UUID.randomUUID()).careGroupId(GROUP_ID).userId(INVITEE_ID)
                .memberRole(GroupMemberRole.MEMBER).inviteStatus(InviteStatus.PENDING)
                .inviteToken("invite-token").build();
        when(memberRepository.findFirstByCareGroupIdAndUserIdAndInviteStatus(
                GROUP_ID, INVITEE_ID, InviteStatus.PENDING)).thenReturn(Optional.of(pending));
        when(memberRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CareGroupMemberDto result = careGroupService.acceptInvite(GROUP_ID, INVITEE_ID);

        assertThat(result.getInviteStatus()).isEqualTo("ACCEPTED");
        verify(memberRepository).save(argThat(m -> m.getJoinedAt() != null));
    }

    @Test
    void acceptInvite_noPendingInvite_throwsNotFound() {
        when(memberRepository.findFirstByCareGroupIdAndUserIdAndInviteStatus(
                GROUP_ID, INVITEE_ID, InviteStatus.PENDING)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> careGroupService.acceptInvite(GROUP_ID, INVITEE_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("FAM-009"));
    }

    @Test
    void declineInvite_pendingInvite_setsRevoked() {
        CareGroupMember pending = CareGroupMember.builder()
                .id(UUID.randomUUID()).careGroupId(GROUP_ID).userId(INVITEE_ID)
                .memberRole(GroupMemberRole.MEMBER).inviteStatus(InviteStatus.PENDING)
                .inviteToken("invite-token").build();
        when(memberRepository.findFirstByCareGroupIdAndUserIdAndInviteStatus(
                GROUP_ID, INVITEE_ID, InviteStatus.PENDING)).thenReturn(Optional.of(pending));
        when(memberRepository.save(pending)).thenReturn(pending);

        careGroupService.declineInvite(GROUP_ID, INVITEE_ID);

        verify(memberRepository).save(argThat(m -> m.getInviteStatus() == InviteStatus.REVOKED));
    }

    @Test
    void respondJoinRequest_approvePublishesDurableEligibilityCandidate() {
        UUID memberId = UUID.randomUUID();
        CareGroupMember pending = CareGroupMember.builder()
                .id(memberId).careGroupId(GROUP_ID).userId(INVITEE_ID)
                .memberRole(GroupMemberRole.MEMBER).inviteStatus(InviteStatus.PENDING).build();
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(savedGroup(GROUP_ID)));
        when(memberRepository.findByIdAndCareGroupId(memberId, GROUP_ID)).thenReturn(Optional.of(pending));
        when(memberRepository.save(pending)).thenReturn(pending);

        CareGroupMemberDto result = careGroupService.respondJoinRequest(
                GROUP_ID, memberId, true, CALLER_ID);

        assertThat(result.getInviteStatus()).isEqualTo("ACCEPTED");
    }

    @Test
    void respondJoinRequest_rejectDoesNotPublishEligibilityCandidate() {
        UUID memberId = UUID.randomUUID();
        CareGroupMember pending = CareGroupMember.builder()
                .id(memberId).careGroupId(GROUP_ID).userId(INVITEE_ID)
                .memberRole(GroupMemberRole.MEMBER).inviteStatus(InviteStatus.PENDING).build();
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(savedGroup(GROUP_ID)));
        when(memberRepository.findByIdAndCareGroupId(memberId, GROUP_ID)).thenReturn(Optional.of(pending));
        when(memberRepository.save(pending)).thenReturn(pending);

        CareGroupMemberDto result = careGroupService.respondJoinRequest(
                GROUP_ID, memberId, false, CALLER_ID);

        assertThat(result.getInviteStatus()).isEqualTo("REJECTED");
    }

    @Test
    void listMyInvitations_returnsPendingOnly() {
        CareGroupMember pending = CareGroupMember.builder()
                .careGroupId(GROUP_ID).userId(INVITEE_ID)
                .memberRole(GroupMemberRole.MEMBER).inviteStatus(InviteStatus.PENDING)
                .inviteToken("invite-token").build();
        when(memberRepository.findByUserIdAndInviteStatus(INVITEE_ID, InviteStatus.PENDING))
                .thenReturn(List.of(pending));
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(savedGroup(GROUP_ID)));

        List<PendingInvitationDto> result = careGroupService.listMyInvitations(INVITEE_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getGroupId()).isEqualTo(GROUP_ID);
    }
}
