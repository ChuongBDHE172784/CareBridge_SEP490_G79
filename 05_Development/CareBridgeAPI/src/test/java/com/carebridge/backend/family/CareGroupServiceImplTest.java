package com.carebridge.backend.family;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.family.dto.CareGroupMembersResponse;
import com.carebridge.backend.family.dto.CreateCareGroupRequest;
import com.carebridge.backend.family.dto.CreateCareGroupResponse;
import com.carebridge.backend.family.entity.CareGroup;
import com.carebridge.backend.family.entity.CareGroupMember;
import com.carebridge.backend.family.entity.CareGroupStatus;
import com.carebridge.backend.family.entity.GroupMemberRole;
import com.carebridge.backend.family.entity.InviteStatus;
import com.carebridge.backend.family.repository.CareGroupMemberRepository;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.family.service.impl.CareGroupServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
    @Mock private AuditService auditService;
    @InjectMocks private CareGroupServiceImpl careGroupService;

    private static final UUID CALLER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID GROUP_ID  = UUID.fromString("00000000-0000-0000-0000-000000000002");

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
}
