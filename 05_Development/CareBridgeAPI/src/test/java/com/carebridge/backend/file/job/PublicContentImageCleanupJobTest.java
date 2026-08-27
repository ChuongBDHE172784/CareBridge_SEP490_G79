package com.carebridge.backend.file.job;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.content.repository.ContentRepository;
import com.carebridge.backend.file.entity.FileStatus;
import com.carebridge.backend.file.entity.UploadedFile;
import com.carebridge.backend.file.enums.FileAccessMode;
import com.carebridge.backend.file.enums.FileKind;
import com.carebridge.backend.file.enums.FilePurpose;
import com.carebridge.backend.file.repository.UploadedFileRepository;
import com.carebridge.backend.file.service.impl.CloudinaryStorageService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

// TC-CLEAN-001..009 — see ContentImageOrphanCleanup_Test-Spec.md §4.
@ExtendWith(MockitoExtension.class)
class PublicContentImageCleanupJobTest {

    @Mock private UploadedFileRepository fileRepository;
    @Mock private ContentRepository contentRepository;
    @Mock private CloudinaryStorageService cloudinaryStorageService;
    @Mock private AuditService auditService;

    private final Instant now = Instant.parse("2026-07-23T03:00:00Z");
    private PublicContentImageCleanupJob job;

    @BeforeEach
    void setUp() {
        job = new PublicContentImageCleanupJob(
                fileRepository, contentRepository, cloudinaryStorageService, auditService,
                Clock.fixed(now, ZoneOffset.UTC));
        ReflectionTestUtils.setField(job, "enabled", true);
        ReflectionTestUtils.setField(job, "dryRun", false);
        ReflectionTestUtils.setField(job, "gracePeriodHours", 24L);
    }

    private UploadedFile candidate(String storageKey, Instant createdAt) {
        return UploadedFile.builder()
                .id(UUID.randomUUID())
                .ownerUserId(UUID.randomUUID())
                .storageKey(storageKey)
                .storageProvider("cloudinary")
                .kind(FileKind.IMAGE)
                .purpose(FilePurpose.PUBLIC_CONTENT_IMAGE)
                .accessMode(FileAccessMode.PUBLIC)
                .originalName("photo.jpg")
                .mimeType("image/jpeg")
                .fileSizeBytes(1000L)
                .status(FileStatus.ACTIVE)
                .createdAt(createdAt)
                .build();
    }

    // TC-CLEAN-001: ảnh mồ côi thật sự (quá grace period, không ai tham chiếu) bị xoá đúng
    @Test
    void cleanupOrphanedImages_orphanPastGracePeriod_getsPurged() {
        UploadedFile file = candidate("carebridge/abc|image|PUBLIC", now.minus(Duration.ofHours(48)));
        when(fileRepository.findAllByPurposeAndAccessModeAndStatusAndCreatedAtBefore(
                eq(FilePurpose.PUBLIC_CONTENT_IMAGE), eq(FileAccessMode.PUBLIC), eq(FileStatus.ACTIVE), any()))
                .thenReturn(List.of(file));
        when(contentRepository.existsByBodyContaining("carebridge/abc")).thenReturn(false);

        job.cleanupOrphanedImages();

        verify(cloudinaryStorageService).delete("carebridge/abc|image|PUBLIC");
        verify(fileRepository).delete(file);
    }

    // TC-CLEAN-002: ảnh được tham chiếu bởi content DRAFT -> không xoá
    // TC-CLEAN-003 (regression guard cho ARCHIVED) được verify gián tiếp: existsByBodyContaining()
    // không lọc theo status trong JPQL (xem ContentRepository), nên hành vi "true = giữ" ở đây
    // đúng cho mọi status, không chỉ DRAFT.
    @Test
    void cleanupOrphanedImages_stillReferenced_isNotPurged() {
        UploadedFile file = candidate("carebridge/abc|image|PUBLIC", now.minus(Duration.ofHours(48)));
        when(fileRepository.findAllByPurposeAndAccessModeAndStatusAndCreatedAtBefore(
                any(), any(), any(), any()))
                .thenReturn(List.of(file));
        when(contentRepository.existsByBodyContaining("carebridge/abc")).thenReturn(true);

        job.cleanupOrphanedImages();

        verify(cloudinaryStorageService, never()).delete(any());
        verify(fileRepository, never()).delete(any());
    }

    // TC-CLEAN-004: grace period đúng tham số cutoff truyền cho repository
    @Test
    void cleanupOrphanedImages_usesConfiguredGracePeriodAsCutoff() {
        when(fileRepository.findAllByPurposeAndAccessModeAndStatusAndCreatedAtBefore(
                any(), any(), any(), any()))
                .thenReturn(List.of());

        job.cleanupOrphanedImages();

        verify(fileRepository).findAllByPurposeAndAccessModeAndStatusAndCreatedAtBefore(
                eq(FilePurpose.PUBLIC_CONTENT_IMAGE), eq(FileAccessMode.PUBLIC), eq(FileStatus.ACTIVE),
                eq(now.minus(Duration.ofHours(24))));
    }

    // TC-CLEAN-005: filter PHẢI luôn dùng cả purpose VÀ accessMode=PUBLIC — regression guard cho
    // bug đã tìm thấy trước khi trình user (xem TDS ADR-CLEAN-001 phương án A0 bị loại).
    @Test
    void cleanupOrphanedImages_alwaysFiltersByPurposeAndPublicAccessMode() {
        when(fileRepository.findAllByPurposeAndAccessModeAndStatusAndCreatedAtBefore(
                any(), any(), any(), any()))
                .thenReturn(List.of());

        job.cleanupOrphanedImages();

        verify(fileRepository).findAllByPurposeAndAccessModeAndStatusAndCreatedAtBefore(
                eq(FilePurpose.PUBLIC_CONTENT_IMAGE), eq(FileAccessMode.PUBLIC), any(), any());
    }

    // TC-CLEAN-006: dry-run=true chỉ log, không xoá gì thật
    @Test
    void cleanupOrphanedImages_dryRun_detectsButDoesNotDelete() {
        ReflectionTestUtils.setField(job, "dryRun", true);
        UploadedFile file = candidate("carebridge/abc|image|PUBLIC", now.minus(Duration.ofHours(48)));
        when(fileRepository.findAllByPurposeAndAccessModeAndStatusAndCreatedAtBefore(
                any(), any(), any(), any()))
                .thenReturn(List.of(file));
        when(contentRepository.existsByBodyContaining("carebridge/abc")).thenReturn(false);

        job.cleanupOrphanedImages();

        verify(cloudinaryStorageService, never()).delete(any());
        verify(fileRepository, never()).delete(any());
    }

    // TC-CLEAN-007: enabled=false -> job không query gì cả
    @Test
    void cleanupOrphanedImages_disabled_doesNothing() {
        ReflectionTestUtils.setField(job, "enabled", false);

        job.cleanupOrphanedImages();

        verify(fileRepository, never()).findAllByPurposeAndAccessModeAndStatusAndCreatedAtBefore(
                any(), any(), any(), any());
    }

    // TC-CLEAN-008: lỗi giữa batch không làm crash job, phần tử còn lại vẫn xử lý
    @Test
    void cleanupOrphanedImages_errorOnOneFile_stillProcessesTheRest() {
        UploadedFile failing = candidate("carebridge/fail|image|PUBLIC", now.minus(Duration.ofHours(48)));
        UploadedFile ok = candidate("carebridge/ok|image|PUBLIC", now.minus(Duration.ofHours(48)));
        when(fileRepository.findAllByPurposeAndAccessModeAndStatusAndCreatedAtBefore(
                any(), any(), any(), any()))
                .thenReturn(List.of(failing, ok));
        when(contentRepository.existsByBodyContaining(any())).thenReturn(false);
        org.mockito.Mockito.doThrow(new RuntimeException("Cloudinary down"))
                .when(cloudinaryStorageService).delete("carebridge/fail|image|PUBLIC");

        job.cleanupOrphanedImages();

        verify(fileRepository, never()).delete(failing);
        verify(cloudinaryStorageService).delete("carebridge/ok|image|PUBLIC");
        verify(fileRepository).delete(ok);
    }

    // TC-CLEAN-009: audit log ghi đúng khi xoá thành công
    @Test
    void cleanupOrphanedImages_successfulPurge_logsAudit() {
        UploadedFile file = candidate("carebridge/abc|image|PUBLIC", now.minus(Duration.ofHours(48)));
        when(fileRepository.findAllByPurposeAndAccessModeAndStatusAndCreatedAtBefore(
                any(), any(), any(), any()))
                .thenReturn(List.of(file));
        when(contentRepository.existsByBodyContaining("carebridge/abc")).thenReturn(false);

        job.cleanupOrphanedImages();

        verify(auditService).log(eq(AuditAction.FILE_ORPHAN_PURGED), isNull(),
                eq("UploadedFile"), eq(file.getId().toString()), any());
    }
}
