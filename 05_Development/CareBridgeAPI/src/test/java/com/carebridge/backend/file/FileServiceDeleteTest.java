package com.carebridge.backend.file;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.AccessDeniedBusinessException;
import com.carebridge.backend.common.exception.ResourceNotFoundException;
import com.carebridge.backend.file.entity.FileStatus;
import com.carebridge.backend.file.entity.UploadedFile;
import com.carebridge.backend.file.policy.FileAccessPolicy;
import com.carebridge.backend.file.policy.FileDeletePolicy;
import com.carebridge.backend.file.repository.UploadedFileRepository;
import com.carebridge.backend.file.service.IStorageService;
import com.carebridge.backend.file.service.impl.FileServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceDeleteTest {

    @Mock private UploadedFileRepository fileRepository;
    @Mock private IStorageService storageService;
    @Mock private AuditService auditService;
    @Mock private FileAccessPolicy fileAccessPolicy;
    @Mock private FileDeletePolicy fileDeletePolicy;
    @InjectMocks private FileServiceImpl fileService;

    static final UUID OWNER_ID  = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID FILE_ID   = UUID.fromString("00000000-0000-0000-0000-000000000010");

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

    // FILE-DEL-TC-001: Owner deletes own active, unbound file (happy path)
    @Test
    void deleteFile_ownerDeletesUnboundFile_statusSetToDeleted() {
        when(fileRepository.findByIdAndStatus(FILE_ID, FileStatus.ACTIVE))
                .thenReturn(Optional.of(makeActiveFile()));
        doNothing().when(fileDeletePolicy).assertDeletable(any(), eq(OWNER_ID));
        when(fileRepository.save(any())).thenReturn(makeActiveFile());

        assertThatNoException().isThrownBy(() -> fileService.deleteFile(FILE_ID, OWNER_ID));

        verify(fileRepository).save(argThat(f -> f.getStatus() == FileStatus.DELETED));
    }

    // FILE-DEL-TC-007: Already-deleted file → 404, no side effects
    @Test
    void deleteFile_alreadyDeletedFile_throwsResourceNotFoundNoSave() {
        when(fileRepository.findByIdAndStatus(FILE_ID, FileStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> fileService.deleteFile(FILE_ID, OWNER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageNotContaining("already deleted");

        verify(fileRepository, never()).save(any());
    }

    // FILE-DEL-TC-008: Non-existent fileId → 404
    @Test
    void deleteFile_nonExistentFile_throwsResourceNotFoundException() {
        UUID randomId = UUID.randomUUID();
        when(fileRepository.findByIdAndStatus(randomId, FileStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> fileService.deleteFile(randomId, OWNER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // FILE-DEL-TC-009: IStorageService.delete() is never invoked (soft-delete only — ADR-FILE-008)
    @Test
    void deleteFile_happyPath_storageDeleteNeverCalled() {
        when(fileRepository.findByIdAndStatus(FILE_ID, FileStatus.ACTIVE))
                .thenReturn(Optional.of(makeActiveFile()));
        doNothing().when(fileDeletePolicy).assertDeletable(any(), eq(OWNER_ID));
        when(fileRepository.save(any())).thenReturn(makeActiveFile());

        fileService.deleteFile(FILE_ID, OWNER_ID);

        verify(storageService, never()).delete(anyString());
    }

    // FILE-DEL-TC-010: Successful delete emits FILE_DELETED audit exactly once
    @Test
    void deleteFile_success_emitsFileDeletedAuditOnce() {
        when(fileRepository.findByIdAndStatus(FILE_ID, FileStatus.ACTIVE))
                .thenReturn(Optional.of(makeActiveFile()));
        doNothing().when(fileDeletePolicy).assertDeletable(any(), eq(OWNER_ID));
        when(fileRepository.save(any())).thenReturn(makeActiveFile());

        fileService.deleteFile(FILE_ID, OWNER_ID);

        verify(auditService, times(1)).log(
                eq(AuditAction.FILE_DELETED), eq(OWNER_ID),
                eq("UploadedFile"), eq(FILE_ID.toString()), any());
    }

    // FILE-DEL-TC-011: Denied/blocked paths never emit FILE_DELETED audit
    @Test
    void deleteFile_notFoundPath_noAuditEmitted() {
        when(fileRepository.findByIdAndStatus(FILE_ID, FileStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> fileService.deleteFile(FILE_ID, OWNER_ID))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(auditService, never()).log(eq(AuditAction.FILE_DELETED), any(), any(), any(), any());
    }

    @Test
    void deleteFile_ownershipDeniedPath_noAuditEmitted() {
        when(fileRepository.findByIdAndStatus(FILE_ID, FileStatus.ACTIVE))
                .thenReturn(Optional.of(makeActiveFile()));
        doThrow(new AccessDeniedBusinessException("Not the owner"))
                .when(fileDeletePolicy).assertDeletable(any(), any());

        assertThatThrownBy(() -> fileService.deleteFile(FILE_ID, OWNER_ID))
                .isInstanceOf(AccessDeniedBusinessException.class);

        verify(auditService, never()).log(eq(AuditAction.FILE_DELETED), any(), any(), any(), any());
    }
}
