package com.carebridge.backend.file;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.ResourceNotFoundException;
import com.carebridge.backend.file.dto.ViewFileResponse;
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
class FileServiceViewTest {

    @Mock private UploadedFileRepository fileRepository;
    @Mock private IStorageService storageService;
    @Mock private AuditService auditService;
    @Mock private FileAccessPolicy fileAccessPolicy;
    @Mock private FileDeletePolicy fileDeletePolicy;
    @InjectMocks private FileServiceImpl fileService;

    static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID FILE_ID  = UUID.fromString("00000000-0000-0000-0000-000000000010");

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

    // FILE-VIEW-TC-001: Owner views own active file (happy path)
    @Test
    void viewFile_ownerViewsOwnActiveFile_returnsPopulatedResponse() {
        when(fileRepository.findByIdAndStatus(FILE_ID, FileStatus.ACTIVE))
                .thenReturn(Optional.of(makeActiveFile()));
        doNothing().when(fileAccessPolicy).assertViewable(any(), eq(OWNER_ID), any());
        when(storageService.generatePresignedUrl(anyString(), eq(15)))
                .thenReturn("https://storage.example.com/presigned");

        ViewFileResponse resp = fileService.viewFile(FILE_ID, OWNER_ID);

        assertThat(resp.getFileId()).isEqualTo(FILE_ID);
        assertThat(resp.getOriginalName()).isEqualTo("ultrasound.jpg");
        assertThat(resp.getMimeType()).isEqualTo("image/jpeg");
        assertThat(resp.getPresignedUrl()).isNotBlank();
    }

    // FILE-VIEW-TC-006: Soft-deleted file → 404 (no existence leak)
    @Test
    void viewFile_softDeletedFile_throwsResourceNotFoundException() {
        when(fileRepository.findByIdAndStatus(FILE_ID, FileStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> fileService.viewFile(FILE_ID, OWNER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageNotContaining("deleted");
    }

    // FILE-VIEW-TC-007: Non-existent fileId → 404
    @Test
    void viewFile_nonExistentFileId_throwsResourceNotFoundException() {
        UUID randomId = UUID.randomUUID();
        when(fileRepository.findByIdAndStatus(randomId, FileStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> fileService.viewFile(randomId, OWNER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // FILE-VIEW-TC-008: Presigned URL TTL always 15 minutes (ADR-FILE-006)
    @Test
    void viewFile_presignedUrlTtlIsExactly15Minutes() {
        when(fileRepository.findByIdAndStatus(FILE_ID, FileStatus.ACTIVE))
                .thenReturn(Optional.of(makeActiveFile()));
        doNothing().when(fileAccessPolicy).assertViewable(any(), eq(OWNER_ID), any());
        when(storageService.generatePresignedUrl(anyString(), eq(15)))
                .thenReturn("https://storage.example.com/presigned");

        fileService.viewFile(FILE_ID, OWNER_ID);

        verify(storageService).generatePresignedUrl(
                eq("files/" + FILE_ID + ".jpg"), eq(15));
    }

    // FILE-VIEW-TC-009: Successful view emits FILE_VIEWED audit exactly once
    @Test
    void viewFile_success_emitsFileViewedAuditOnce() {
        when(fileRepository.findByIdAndStatus(FILE_ID, FileStatus.ACTIVE))
                .thenReturn(Optional.of(makeActiveFile()));
        doNothing().when(fileAccessPolicy).assertViewable(any(), eq(OWNER_ID), any());
        when(storageService.generatePresignedUrl(anyString(), eq(15)))
                .thenReturn("https://storage.example.com/presigned");

        fileService.viewFile(FILE_ID, OWNER_ID);

        verify(auditService, times(1)).log(
                eq(AuditAction.FILE_VIEWED), eq(OWNER_ID),
                eq("UploadedFile"), eq(FILE_ID.toString()), any());
    }
}
