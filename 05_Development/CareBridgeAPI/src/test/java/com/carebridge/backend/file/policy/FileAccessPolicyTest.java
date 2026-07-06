package com.carebridge.backend.file.policy;

import com.carebridge.backend.common.exception.AccessDeniedBusinessException;
import com.carebridge.backend.family.entity.CareGroup;
import com.carebridge.backend.family.entity.CareGroupMember;
import com.carebridge.backend.family.entity.InviteStatus;
import com.carebridge.backend.family.repository.CareGroupMemberRepository;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.file.entity.FileStatus;
import com.carebridge.backend.file.entity.UploadedFile;
import com.carebridge.backend.health.entity.HealthRecord;
import com.carebridge.backend.health.entity.HealthRecordFile;
import com.carebridge.backend.health.repository.HealthRecordFileRepository;
import com.carebridge.backend.health.repository.HealthRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileAccessPolicyTest {

    @Mock private HealthRecordFileRepository healthRecordFileRepository;
    @Mock private HealthRecordRepository healthRecordRepository;
    @Mock private CareGroupRepository careGroupRepository;
    @Mock private CareGroupMemberRepository careGroupMemberRepository;
    @InjectMocks private FileAccessPolicyImpl fileAccessPolicy;

    static final UUID OWNER_ID    = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID FAMILY_ID   = UUID.fromString("00000000-0000-0000-0000-000000000002");
    static final UUID STRANGER_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    static final UUID ADMIN_ID    = UUID.fromString("00000000-0000-0000-0000-000000000004");
    static final UUID FILE_ID     = UUID.fromString("00000000-0000-0000-0000-000000000010");
    static final UUID HR_ID       = UUID.fromString("00000000-0000-0000-0000-000000000020");
    static final UUID BABY_ID     = UUID.fromString("00000000-0000-0000-0000-000000000030");
    static final UUID CARE_GROUP_ID = UUID.fromString("00000000-0000-0000-0000-000000000040");

    static UploadedFile makeActiveFile() {
        return UploadedFile.builder()
                .id(FILE_ID)
                .ownerUserId(OWNER_ID)
                .storageKey("files/" + FILE_ID + ".jpg")
                .originalName("ultrasound.jpg")
                .mimeType("image/jpeg")
                .fileSizeBytes(2048L)
                .status(FileStatus.ACTIVE)
                .build();
    }

    // FILE-VIEW-TC-002: Non-owner with no sharing chain → 403
    @Test
    void assertViewable_nonOwnerNoSharingChain_throwsAccessDenied() {
        when(healthRecordFileRepository.findByFileId(FILE_ID)).thenReturn(List.of());

        assertThatThrownBy(() ->
                fileAccessPolicy.assertViewable(makeActiveFile(), STRANGER_ID, List.of("ROLE_MOTHER")))
                .isInstanceOf(AccessDeniedBusinessException.class);
    }

    // FILE-VIEW-TC-003: Family member in ACCEPTED care-group sharing linked baby → allowed
    @Test
    void assertViewable_familyInAcceptedCareGroup_noExceptionThrown() {
        HealthRecordFile link = HealthRecordFile.builder()
                .id(UUID.randomUUID()).healthRecordId(HR_ID).fileId(FILE_ID).build();
        HealthRecord hr = new HealthRecord();
        hr.setBabyId(BABY_ID);

        CareGroup cg = CareGroup.builder().id(CARE_GROUP_ID).linkedBabyProfileId(BABY_ID).build();

        when(healthRecordFileRepository.findByFileId(FILE_ID)).thenReturn(List.of(link));
        when(healthRecordRepository.findById(HR_ID)).thenReturn(Optional.of(hr));
        when(careGroupRepository.findByLinkedBabyProfileId(BABY_ID)).thenReturn(List.of(cg));
        when(careGroupMemberRepository.existsByCareGroupIdAndUserIdAndInviteStatus(
                CARE_GROUP_ID, FAMILY_ID, InviteStatus.ACCEPTED)).thenReturn(true);

        assertThatNoException().isThrownBy(() ->
                fileAccessPolicy.assertViewable(makeActiveFile(), FAMILY_ID, List.of("ROLE_FAMILY")));
    }

    // FILE-VIEW-TC-004: Family member with PENDING invitation → 403 (boundary)
    @Test
    void assertViewable_familyWithPendingInvite_throwsAccessDenied() {
        HealthRecordFile link = HealthRecordFile.builder()
                .id(UUID.randomUUID()).healthRecordId(HR_ID).fileId(FILE_ID).build();
        HealthRecord hr = new HealthRecord();
        hr.setBabyId(BABY_ID);

        CareGroup cg = CareGroup.builder().id(CARE_GROUP_ID).linkedBabyProfileId(BABY_ID).build();

        when(healthRecordFileRepository.findByFileId(FILE_ID)).thenReturn(List.of(link));
        when(healthRecordRepository.findById(HR_ID)).thenReturn(Optional.of(hr));
        when(careGroupRepository.findByLinkedBabyProfileId(BABY_ID)).thenReturn(List.of(cg));
        when(careGroupMemberRepository.existsByCareGroupIdAndUserIdAndInviteStatus(
                CARE_GROUP_ID, FAMILY_ID, InviteStatus.ACCEPTED)).thenReturn(false);

        assertThatThrownBy(() ->
                fileAccessPolicy.assertViewable(makeActiveFile(), FAMILY_ID, List.of("ROLE_FAMILY")))
                .isInstanceOf(AccessDeniedBusinessException.class);
    }

    // FILE-VIEW-TC-005: Admin role bypasses scope check (parametrized)
    @Test
    void assertViewable_systemAdminRole_noExceptionThrown() {
        assertThatNoException().isThrownBy(() ->
                fileAccessPolicy.assertViewable(makeActiveFile(), ADMIN_ID, List.of("ROLE_SYSTEM_ADMIN")));
    }

    @Test
    void assertViewable_moderatorRole_noExceptionThrown() {
        assertThatNoException().isThrownBy(() ->
                fileAccessPolicy.assertViewable(makeActiveFile(), ADMIN_ID, List.of("ROLE_MODERATOR")));
    }

    @Test
    void assertViewable_contentAdminRole_noExceptionThrown() {
        assertThatNoException().isThrownBy(() ->
                fileAccessPolicy.assertViewable(makeActiveFile(), ADMIN_ID, List.of("ROLE_CONTENT_ADMIN")));
    }
}
