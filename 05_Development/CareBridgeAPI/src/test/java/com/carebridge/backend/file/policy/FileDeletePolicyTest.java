package com.carebridge.backend.file.policy;

import com.carebridge.backend.common.exception.AccessDeniedBusinessException;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.file.entity.FileStatus;
import com.carebridge.backend.file.entity.UploadedFile;
import com.carebridge.backend.health.entity.HealthRecordFile;
import com.carebridge.backend.health.repository.HealthRecordFileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileDeletePolicyTest {

    @Mock private HealthRecordFileRepository healthRecordFileRepository;
    @InjectMocks private FileDeletePolicyImpl fileDeletePolicy;

    static final UUID OWNER_ID    = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID FAMILY_ID   = UUID.fromString("00000000-0000-0000-0000-000000000002");
    static final UUID STRANGER_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    static final UUID ADMIN_ID    = UUID.fromString("00000000-0000-0000-0000-000000000004");
    static final UUID FILE_ID     = UUID.fromString("00000000-0000-0000-0000-000000000010");
    static final UUID HR_ID       = UUID.fromString("00000000-0000-0000-0000-000000000020");

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

    static HealthRecordFile makeLink() {
        return HealthRecordFile.builder()
                .id(UUID.randomUUID())
                .healthRecordId(HR_ID)
                .fileId(FILE_ID)
                .displayOrder(0)
                .build();
    }

    // FILE-DEL-TC-002: Non-owner attempts delete → 403 (strict owner-only)
    @Test
    void assertDeletable_nonOwner_throwsAccessDenied() {
        assertThatThrownBy(() -> fileDeletePolicy.assertDeletable(makeActiveFile(), STRANGER_ID))
                .isInstanceOf(AccessDeniedBusinessException.class);
    }

    // FILE-DEL-TC-003: File bound to a health record → 409
    @Test
    void assertDeletable_fileBoundToHealthRecord_throwsConflict() {
        when(healthRecordFileRepository.findByFileId(FILE_ID)).thenReturn(List.of(makeLink()));

        assertThatThrownBy(() -> fileDeletePolicy.assertDeletable(makeActiveFile(), OWNER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getHttpStatus())
                        .isEqualTo(HttpStatus.CONFLICT));
    }

    // FILE-DEL-TC-004: Boundary — zero bindings → allowed
    @Test
    void assertDeletable_zeroBoundLinks_noExceptionThrown() {
        when(healthRecordFileRepository.findByFileId(FILE_ID)).thenReturn(List.of());

        assertThatNoException().isThrownBy(() ->
                fileDeletePolicy.assertDeletable(makeActiveFile(), OWNER_ID));
    }

    // FILE-DEL-TC-005: Admin role does NOT bypass ownership check (unlike UC-168 view scope)
    @Test
    void assertDeletable_adminNotOwner_throwsAccessDenied() {
        assertThatThrownBy(() -> fileDeletePolicy.assertDeletable(makeActiveFile(), ADMIN_ID))
                .isInstanceOf(AccessDeniedBusinessException.class);
    }

    // FILE-DEL-TC-006: Family member (even in shared care-group) does NOT bypass ownership check
    @Test
    void assertDeletable_familyMemberNotOwner_throwsAccessDenied() {
        assertThatThrownBy(() -> fileDeletePolicy.assertDeletable(makeActiveFile(), FAMILY_ID))
                .isInstanceOf(AccessDeniedBusinessException.class);

        // FileDeletePolicy must never query care-group membership
        verifyNoInteractions(healthRecordFileRepository);
    }
}
