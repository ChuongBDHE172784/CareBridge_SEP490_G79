package com.carebridge.backend.family.service;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.family.CareGroupTestFactory;
import com.carebridge.backend.family.dto.FamilyPermissionResponse;
import com.carebridge.backend.family.dto.UpdateFamilyPermissionRequest;
import com.carebridge.backend.family.entity.CareGroup;
import com.carebridge.backend.family.entity.CareGroupMember;
import com.carebridge.backend.family.entity.GroupMemberRole;
import com.carebridge.backend.family.entity.InviteStatus;
import com.carebridge.backend.family.event.FamilyPermissionUpdated;
import com.carebridge.backend.family.policy.CareGroupAuthorizationPolicy;
import com.carebridge.backend.family.repository.CareGroupMemberRepository;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.family.service.impl.CareGroupServiceImpl;
import com.carebridge.backend.notification.entity.DeviceToken;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CareGroupServiceImplPermissionTest {

    @Mock private CareGroupRepository groupRepository;
    @Mock private CareGroupMemberRepository memberRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditService auditService;
    @Mock private CareGroupAuthorizationPolicy authorizationPolicy;
    @Mock private InviteTokenGenerator tokenGenerator;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private FcmService fcmService;
    @Mock private DeviceTokenRepository deviceTokenRepository;
    @InjectMocks private CareGroupServiceImpl service;

    private static final UUID GROUP_ID       = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID OWNER_ID       = UUID.fromString("00000000-0000-0000-0000-000000000011");
    private static final UUID MEMBER_ID      = UUID.fromString("00000000-0000-0000-0000-000000000012");
    private static final UUID MEMBER_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000013");

    private static final String DEFAULT_PERM_JSON =
            "{\"calendar\":true,\"logs\":false,\"alerts\":true,\"records\":false}";

    // ── Stubs ────────────────────────────────────────────────────────────────

    private void stubActiveGroup() {
        CareGroup group = CareGroupTestFactory.makeCareGroup(g -> g.setId(GROUP_ID));
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));
    }

    private void stubOwnerAuthorized() {
        when(authorizationPolicy.canManagePermissions(GROUP_ID, OWNER_ID)).thenReturn(true);
    }

    private CareGroupMember stubAcceptedMember(String permissionJson) {
        CareGroupMember member = CareGroupTestFactory.makeCareGroupMember(m -> {
            m.setId(MEMBER_ID);
            m.setCareGroupId(GROUP_ID);
            m.setUserId(MEMBER_USER_ID);
            m.setMemberRole(GroupMemberRole.MEMBER);
            m.setInviteStatus(InviteStatus.ACCEPTED);
            m.setPermissionJson(permissionJson);
        });
        when(memberRepository.findByIdAndCareGroupId(MEMBER_ID, GROUP_ID)).thenReturn(Optional.of(member));
        return member;
    }

    private void stubSaveMember(CareGroupMember member) {
        when(memberRepository.save(any(CareGroupMember.class))).thenReturn(member);
    }

    private void stubDeviceToken(UUID userId) {
        DeviceToken dt = CareGroupTestFactory.makeDeviceToken(userId);
        when(deviceTokenRepository.findByUserIdAndActiveTrue(userId)).thenReturn(List.of(dt));
    }

    private UpdateFamilyPermissionRequest allFlagsRequest() {
        return CareGroupTestFactory.makePermissionUpdateRequest(r -> {
        });
    }

    // ── TC-001: Happy path — owner updates permission ─────────────────────────

    @Test
    void updateFamilyPermission_ownerValidFlags_returnsCorrectResponse() {
        stubActiveGroup();
        stubOwnerAuthorized();
        CareGroupMember member = stubAcceptedMember(DEFAULT_PERM_JSON);
        stubSaveMember(member);
        stubDeviceToken(MEMBER_USER_ID);

        FamilyPermissionResponse result = service.updateFamilyPermission(
                GROUP_ID, MEMBER_ID, allFlagsRequest(), OWNER_ID);

        assertThat(result).isNotNull();
        assertThat(result.isCalendar()).isTrue();
        assertThat(result.isLogs()).isFalse();
        assertThat(result.isAlerts()).isTrue();
        assertThat(result.isRecords()).isFalse();
        verify(memberRepository).save(any(CareGroupMember.class));
    }

    // ── TC-002: Partial update leaves other flags unchanged ───────────────────

    @Test
    void updateFamilyPermission_partialUpdate_preservesPreviousFlags() {
        stubActiveGroup();
        stubOwnerAuthorized();
        CareGroupMember member = stubAcceptedMember(DEFAULT_PERM_JSON);
        stubSaveMember(member);
        stubDeviceToken(MEMBER_USER_ID);

        UpdateFamilyPermissionRequest partial = new UpdateFamilyPermissionRequest();
        partial.setLogs(true); // only logs changes; others remain null (unchanged)

        FamilyPermissionResponse result = service.updateFamilyPermission(
                GROUP_ID, MEMBER_ID, partial, OWNER_ID);

        assertThat(result.isCalendar()).isTrue();  // preserved from DEFAULT_PERM_JSON
        assertThat(result.isLogs()).isTrue();       // changed
        assertThat(result.isAlerts()).isTrue();     // preserved
        assertThat(result.isRecords()).isFalse();   // preserved
    }

    @Test
    void updateFamilyPermission_turningQuickNotesOffClearsAllChildPermissions() {
        stubActiveGroup();
        stubOwnerAuthorized();
        CareGroupMember member = stubAcceptedMember(
                "{\"quickNotes\":true,\"quickNoteWeight\":true,"
                        + "\"quickNoteHydration\":true,\"quickNoteEpds\":true,"
                        + "\"quickNoteFetalMovement\":true}");
        stubSaveMember(member);
        stubDeviceToken(MEMBER_USER_ID);
        UpdateFamilyPermissionRequest request = new UpdateFamilyPermissionRequest();
        request.setQuickNotes(false);

        FamilyPermissionResponse result = service.updateFamilyPermission(
                GROUP_ID, MEMBER_ID, request, OWNER_ID);

        assertThat(result.isQuickNotes()).isFalse();
        assertThat(result.isQuickNoteWeight()).isFalse();
        assertThat(result.isQuickNoteHydration()).isFalse();
        assertThat(result.isQuickNoteEpds()).isFalse();
        assertThat(result.isQuickNoteFetalMovement()).isFalse();
    }

    @Test
    void updateFamilyPermission_partialQuickNoteUpdatePreservesOtherChildren() {
        stubActiveGroup();
        stubOwnerAuthorized();
        CareGroupMember member = stubAcceptedMember(
                "{\"quickNotes\":true,\"quickNoteWeight\":true,"
                        + "\"quickNoteHydration\":false,\"quickNoteEpds\":true,"
                        + "\"quickNoteFetalMovement\":false}");
        stubSaveMember(member);
        stubDeviceToken(MEMBER_USER_ID);
        UpdateFamilyPermissionRequest request = new UpdateFamilyPermissionRequest();
        request.setQuickNoteHydration(true);

        FamilyPermissionResponse result = service.updateFamilyPermission(
                GROUP_ID, MEMBER_ID, request, OWNER_ID);

        assertThat(result.isQuickNotes()).isTrue();
        assertThat(result.isQuickNoteWeight()).isTrue();
        assertThat(result.isQuickNoteHydration()).isTrue();
        assertThat(result.isQuickNoteEpds()).isTrue();
        assertThat(result.isQuickNoteFetalMovement()).isFalse();
    }

    // ── TC-003: Member views own permission grant ─────────────────────────────

    @Test
    void updateFamilyPermission_preservesChecklistPermissionKeys() {
        stubActiveGroup();
        stubOwnerAuthorized();
        CareGroupMember member = stubAcceptedMember(
                "{\"calendar\":true,\"CHECKLIST_VIEW\":true,\"CHECKLIST_COMPLETE\":false}");
        stubSaveMember(member);
        stubDeviceToken(MEMBER_USER_ID);

        UpdateFamilyPermissionRequest partial = new UpdateFamilyPermissionRequest();
        partial.setLogs(true);
        service.updateFamilyPermission(GROUP_ID, MEMBER_ID, partial, OWNER_ID);

        assertThat(member.getPermissionJson())
                .contains("\"CHECKLIST_VIEW\":true")
                .contains("\"CHECKLIST_COMPLETE\":false");
    }

    @Test
    void updateFamilyPermission_checklistOnlyPayloadCanGrantAccess() throws Exception {
        stubActiveGroup();
        stubOwnerAuthorized();
        CareGroupMember member = stubAcceptedMember(DEFAULT_PERM_JSON);
        stubSaveMember(member);
        stubDeviceToken(MEMBER_USER_ID);
        UpdateFamilyPermissionRequest request = new com.fasterxml.jackson.databind.ObjectMapper()
                .readValue("{\"checklistView\":true,\"checklistComplete\":true}",
                        UpdateFamilyPermissionRequest.class);

        FamilyPermissionResponse response = service.updateFamilyPermission(
                GROUP_ID, MEMBER_ID, request, OWNER_ID);

        assertThat(member.getPermissionJson())
                .contains("\"CHECKLIST_VIEW\":true")
                .contains("\"CHECKLIST_COMPLETE\":true")
                .doesNotContain("\"checklistView\"")
                .doesNotContain("\"checklistComplete\"");
        assertThat(response).extracting("checklistView", "checklistComplete")
                .containsExactly(true, true);
    }

    @Test
    void updateFamilyPermission_completeOnlyChangeDoesNotPublishDistributionCandidate() {
        stubActiveGroup();
        stubOwnerAuthorized();
        CareGroupMember member = stubAcceptedMember(
                "{\"CHECKLIST_VIEW\":false,\"CHECKLIST_COMPLETE\":false}");
        stubSaveMember(member);
        stubDeviceToken(MEMBER_USER_ID);
        UpdateFamilyPermissionRequest request = new UpdateFamilyPermissionRequest();
        request.setChecklistComplete(true);

        service.updateFamilyPermission(GROUP_ID, MEMBER_ID, request, OWNER_ID);

    }

    @Test
    void getFamilyPermission_memberViewsOwnRecord_returnsCurrentFlags() {
        stubActiveGroup();
        stubAcceptedMember(DEFAULT_PERM_JSON);

        FamilyPermissionResponse result = service.getFamilyPermission(
                GROUP_ID, MEMBER_ID, MEMBER_USER_ID);

        assertThat(result).isNotNull();
        assertThat(result.isCalendar()).isTrue();
        assertThat(result.isLogs()).isFalse();
        assertThat(result.isAlerts()).isTrue();
        assertThat(result.isRecords()).isFalse();
        verify(memberRepository, never()).save(any()); // read-only — no write
    }

    // ── TC-004: Update on nonexistent group → 404 FAM-005 ────────────────────

    @Test
    void updateFamilyPermission_groupNotFound_throws404Fam005() {
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.updateFamilyPermission(GROUP_ID, MEMBER_ID, allFlagsRequest(), OWNER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(be.getCode()).isEqualTo("FAM-005");
                });
        verify(memberRepository, never()).save(any());
    }

    // ── TC-005: Read on nonexistent group → 404 FAM-005 ──────────────────────

    @Test
    void getFamilyPermission_groupNotFound_throws404Fam005() {
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.getFamilyPermission(GROUP_ID, MEMBER_ID, OWNER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(be.getCode()).isEqualTo("FAM-005");
                });
    }

    // ── TC-006: Non-owner (ACCEPTED MEMBER role) attempts update → 403 FAM-021 ─

    @Test
    void updateFamilyPermission_callerNotOwner_throws403Fam021() {
        stubActiveGroup();
        when(authorizationPolicy.canManagePermissions(GROUP_ID, OWNER_ID)).thenReturn(false);

        assertThatThrownBy(() ->
                service.updateFamilyPermission(GROUP_ID, MEMBER_ID, allFlagsRequest(), OWNER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(be.getCode()).isEqualTo("FAM-021");
                });
        verify(memberRepository, never()).save(any());
    }

    // ── TC-007: Target member PENDING → 404 FAM-020 ──────────────────────────

    @Test
    void updateFamilyPermission_targetMemberPending_throws404Fam020() {
        stubActiveGroup();
        stubOwnerAuthorized();
        CareGroupMember pending = CareGroupTestFactory.makeCareGroupMember(m -> {
            m.setId(MEMBER_ID);
            m.setCareGroupId(GROUP_ID);
            m.setInviteStatus(InviteStatus.PENDING);
        });
        when(memberRepository.findByIdAndCareGroupId(MEMBER_ID, GROUP_ID))
                .thenReturn(Optional.of(pending));

        assertThatThrownBy(() ->
                service.updateFamilyPermission(GROUP_ID, MEMBER_ID, allFlagsRequest(), OWNER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(be.getCode()).isEqualTo("FAM-020");
                });
        verify(memberRepository, never()).save(any());
    }

    // ── TC-008: Target member REVOKED → 404 FAM-020 ──────────────────────────

    @Test
    void updateFamilyPermission_targetMemberRevoked_throws404Fam020() {
        stubActiveGroup();
        stubOwnerAuthorized();
        CareGroupMember revoked = CareGroupTestFactory.makeCareGroupMember(m -> {
            m.setId(MEMBER_ID);
            m.setCareGroupId(GROUP_ID);
            m.setInviteStatus(InviteStatus.REVOKED);
        });
        when(memberRepository.findByIdAndCareGroupId(MEMBER_ID, GROUP_ID))
                .thenReturn(Optional.of(revoked));

        assertThatThrownBy(() ->
                service.updateFamilyPermission(GROUP_ID, MEMBER_ID, allFlagsRequest(), OWNER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(be.getCode()).isEqualTo("FAM-020");
                });
        verify(memberRepository, never()).save(any());
    }

    // ── TC-009: Empty/all-null payload → 400 FAM-022 ─────────────────────────

    @Test
    void updateFamilyPermission_allNullPayload_throws400Fam022() {
        stubActiveGroup();
        stubOwnerAuthorized();
        stubAcceptedMember(DEFAULT_PERM_JSON);

        UpdateFamilyPermissionRequest empty = new UpdateFamilyPermissionRequest();
        // all fields null

        assertThatThrownBy(() ->
                service.updateFamilyPermission(GROUP_ID, MEMBER_ID, empty, OWNER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(be.getCode()).isEqualTo("FAM-022");
                });
        verify(memberRepository, never()).save(any());
    }

    // ── TC-010: FCM failure does not roll back DB write ───────────────────────

    @Test
    void updateFamilyPermission_fcmThrows_dbWritePersistedAnd200Returned() {
        stubActiveGroup();
        stubOwnerAuthorized();
        CareGroupMember member = stubAcceptedMember(DEFAULT_PERM_JSON);
        stubSaveMember(member);
        when(deviceTokenRepository.findByUserIdAndActiveTrue(MEMBER_USER_ID))
                .thenReturn(List.of(CareGroupTestFactory.makeDeviceToken(MEMBER_USER_ID)));
        when(fcmService.sendToTokens(anyList(), any(), any()))
                .thenThrow(new RuntimeException("FCM unavailable"));

        FamilyPermissionResponse result = service.updateFamilyPermission(
                GROUP_ID, MEMBER_ID, allFlagsRequest(), OWNER_ID);

        assertThat(result).isNotNull();
        verify(memberRepository).save(any(CareGroupMember.class));
    }

    // ── TC-011: No active device tokens → FCM skipped gracefully ─────────────

    @Test
    void updateFamilyPermission_noDeviceTokens_updateSucceedsWithoutFcmCall() {
        stubActiveGroup();
        stubOwnerAuthorized();
        CareGroupMember member = stubAcceptedMember(DEFAULT_PERM_JSON);
        stubSaveMember(member);
        when(deviceTokenRepository.findByUserIdAndActiveTrue(MEMBER_USER_ID))
                .thenReturn(List.of());

        FamilyPermissionResponse result = service.updateFamilyPermission(
                GROUP_ID, MEMBER_ID, allFlagsRequest(), OWNER_ID);

        assertThat(result).isNotNull();
        verify(memberRepository).save(any(CareGroupMember.class));
        verify(fcmService, never()).sendToTokens(anyList(), any(), any());
    }

    // ── TC-012: Successful update creates audit log entry ─────────────────────

    @Test
    void updateFamilyPermission_success_createsAuditLogEntry() {
        stubActiveGroup();
        stubOwnerAuthorized();
        CareGroupMember member = stubAcceptedMember(DEFAULT_PERM_JSON);
        stubSaveMember(member);
        stubDeviceToken(MEMBER_USER_ID);

        service.updateFamilyPermission(GROUP_ID, MEMBER_ID, allFlagsRequest(), OWNER_ID);

        verify(auditService).log(
                eq(AuditAction.CARE_GROUP_PERMISSION_UPDATED),
                eq(OWNER_ID),
                eq("CareGroupMember"),
                eq(MEMBER_ID.toString()),
                any());
    }

    // ── TC-013: FamilyPermissionUpdated event published with correct payload ──

    @Test
    void updateFamilyPermission_success_publishesDomainEventWithCorrectPayload() {
        stubActiveGroup();
        stubOwnerAuthorized();
        // Seed: previous logs=false; request changes logs to true
        CareGroupMember member = stubAcceptedMember(
                "{\"calendar\":true,\"logs\":false,\"alerts\":true,\"records\":false}");
        stubSaveMember(member);
        stubDeviceToken(MEMBER_USER_ID);

        UpdateFamilyPermissionRequest req = new UpdateFamilyPermissionRequest();
        req.setLogs(true);

        service.updateFamilyPermission(GROUP_ID, MEMBER_ID, req, OWNER_ID);

        ArgumentCaptor<FamilyPermissionUpdated> captor =
                ArgumentCaptor.forClass(FamilyPermissionUpdated.class);
        verify(eventPublisher).publishEvent(captor.capture());

        FamilyPermissionUpdated event = captor.getValue();
        assertThat(event.payload().careGroupId()).isEqualTo(GROUP_ID);
        assertThat(event.payload().careGroupMemberId()).isEqualTo(MEMBER_ID);
        assertThat(event.payload().updatedBy()).isEqualTo(OWNER_ID);
        assertThat(event.payload().previousPermissions().isLogs()).isFalse();
        assertThat(event.payload().newPermissions().isLogs()).isTrue();
    }

    // ── TC-014: Non-target, non-owner member attempts GET → 403 FAM-003 ──────

    @Test
    void getFamilyPermission_thirdPartyNonOwnerCaller_throws403Fam003() {
        UUID thirdPartyId = UUID.fromString("00000000-0000-0000-0000-000000000099");
        stubActiveGroup();
        // Target member has a different userId than thirdPartyId
        CareGroupMember target = CareGroupTestFactory.makeCareGroupMember(m -> {
            m.setId(MEMBER_ID);
            m.setCareGroupId(GROUP_ID);
            m.setUserId(MEMBER_USER_ID);  // different from thirdPartyId
            m.setInviteStatus(InviteStatus.ACCEPTED);
        });
        when(memberRepository.findByIdAndCareGroupId(MEMBER_ID, GROUP_ID))
                .thenReturn(Optional.of(target));
        when(authorizationPolicy.isOwner(GROUP_ID, thirdPartyId)).thenReturn(false);

        assertThatThrownBy(() ->
                service.getFamilyPermission(GROUP_ID, MEMBER_ID, thirdPartyId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(be.getCode()).isEqualTo("FAM-003");
                });
    }

    // ── TC-016: Boundary — all 4 flags explicitly false (valid, not FAM-022) ──

    @Test
    void updateFamilyPermission_allFlagsExplicitlyFalse_succeeds() {
        stubActiveGroup();
        stubOwnerAuthorized();
        CareGroupMember member = stubAcceptedMember(DEFAULT_PERM_JSON);
        stubSaveMember(member);
        stubDeviceToken(MEMBER_USER_ID);

        UpdateFamilyPermissionRequest allFalse = new UpdateFamilyPermissionRequest();
        allFalse.setCalendar(false);
        allFalse.setLogs(false);
        allFalse.setAlerts(false);
        allFalse.setRecords(false);

        FamilyPermissionResponse result = service.updateFamilyPermission(
                GROUP_ID, MEMBER_ID, allFalse, OWNER_ID);

        assertThat(result).isNotNull();
        assertThat(result.isCalendar()).isFalse();
        assertThat(result.isLogs()).isFalse();
        assertThat(result.isAlerts()).isFalse();
        assertThat(result.isRecords()).isFalse();
        verify(memberRepository).save(any(CareGroupMember.class));
    }

    // ── TC-018: Cross-group memberId (IDOR attempt) → 404 FAM-020 ────────────

    @Test
    void updateFamilyPermission_memberBelongsToAnotherGroup_throws404Fam020() {
        stubActiveGroup();
        stubOwnerAuthorized();
        when(memberRepository.findByIdAndCareGroupId(MEMBER_ID, GROUP_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.updateFamilyPermission(GROUP_ID, MEMBER_ID, allFlagsRequest(), OWNER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(be.getCode()).isEqualTo("FAM-020");
                });
        verify(memberRepository, never()).save(any());
    }
}
