package com.carebridge.backend.file;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.file.dto.UploadFileResponse;
import com.carebridge.backend.file.entity.FileStatus;
import com.carebridge.backend.file.entity.UploadedFile;
import com.carebridge.backend.file.repository.UploadedFileRepository;
import com.carebridge.backend.file.service.IStorageService;
import com.carebridge.backend.file.service.impl.FileServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceImplTest {

    @Mock private UploadedFileRepository fileRepository;
    @Mock private IStorageService storageService;
    @Mock private AuditService auditService;
    @InjectMocks private FileServiceImpl fileService;

    private static final UUID CALLER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID FILE_ID   = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private MultipartFile makeFile(String contentType, int sizeBytes) {
        byte[] content = new byte[sizeBytes];
        if (contentType.equals("image/jpeg")) {
            content[0] = (byte) 0xFF;
            content[1] = (byte) 0xD8;
        }
        return new MockMultipartFile("file", "photo.jpg", contentType, content);
    }

    private UploadedFile savedFile(UUID id) {
        return UploadedFile.builder()
                .id(id)
                .ownerUserId(CALLER_ID)
                .storageKey("files/" + id + ".jpg")
                .originalName("photo.jpg")
                .mimeType("image/jpeg")
                .fileSizeBytes(1000L)
                .status(FileStatus.ACTIVE)
                .build();
    }

    // FILE-TC-001: Happy path JPEG upload
    @Test
    void uploadFile_validJpeg_returnsPresignedUrl() {
        when(fileRepository.countByOwnerUserIdAndStatus(CALLER_ID, FileStatus.ACTIVE)).thenReturn(0L);
        when(fileRepository.save(any())).thenReturn(savedFile(FILE_ID));
        when(storageService.generatePresignedUrl(any(), eq(15))).thenReturn("https://presigned.url/file");

        UploadFileResponse resp = fileService.uploadFile(makeFile("image/jpeg", 1000), CALLER_ID);

        assertThat(resp.getFileId()).isEqualTo(FILE_ID);
        assertThat(resp.getPresignedUrl()).isNotBlank();
    }

    // FILE-TC-002: C3 — presigned URL TTL must be exactly 15 minutes
    @Test
    void uploadFile_presignedUrlTtlIs15Minutes() {
        when(fileRepository.countByOwnerUserIdAndStatus(any(), any())).thenReturn(0L);
        when(fileRepository.save(any())).thenReturn(savedFile(FILE_ID));
        when(storageService.generatePresignedUrl(any(), eq(15))).thenReturn("https://presigned.url/file");

        fileService.uploadFile(makeFile("image/jpeg", 1000), CALLER_ID);

        verify(storageService).generatePresignedUrl(any(), eq(15));
    }

    // FILE-TC-003: File > 20MB → FILE-002 / 413
    @Test
    void uploadFile_overSizeLimit_throwsBusinessException413() {
        int overSize = 20 * 1024 * 1024 + 1;
        MultipartFile bigFile = new MockMultipartFile("file", "big.jpg", "image/jpeg", new byte[overSize]);

        assertThatThrownBy(() -> fileService.uploadFile(bigFile, CALLER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo("FILE-002");
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.CONTENT_TOO_LARGE);
                });

        verify(storageService, never()).store(any(), any(), any());
    }

    // FILE-TC-004: C4 — storage quota check before write
    @Test
    void uploadFile_quotaExceeded_throwsBusinessException409() {
        when(fileRepository.countByOwnerUserIdAndStatus(CALLER_ID, FileStatus.ACTIVE)).thenReturn(500L);

        assertThatThrownBy(() -> fileService.uploadFile(makeFile("image/jpeg", 1000), CALLER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getHttpStatus())
                        .isEqualTo(HttpStatus.CONFLICT));

        verify(storageService, never()).store(any(), any(), any());
    }

    // FILE-TC-002: Valid PDF upload (magic bytes %PDF) — upload completes, no exception thrown
    @Test
    void uploadFile_validPdf_returnsPresignedUrl() {
        byte[] pdfBytes = new byte[100];
        pdfBytes[0] = 0x25; pdfBytes[1] = 0x50; pdfBytes[2] = 0x44; pdfBytes[3] = 0x46; // %PDF
        MockMultipartFile pdfFile = new MockMultipartFile("file", "report.pdf", "application/pdf", pdfBytes);
        when(fileRepository.countByOwnerUserIdAndStatus(CALLER_ID, FileStatus.ACTIVE)).thenReturn(0L);
        when(fileRepository.save(any())).thenReturn(savedFile(FILE_ID));
        when(storageService.generatePresignedUrl(any(), eq(15))).thenReturn("https://presigned.url/report.pdf");

        UploadFileResponse resp = fileService.uploadFile(pdfFile, CALLER_ID);

        // PDF magic bytes detected — upload succeeds (no exception, fileId returned)
        assertThat(resp.getFileId()).isEqualTo(FILE_ID);
        assertThat(resp.getPresignedUrl()).isNotBlank();
        verify(storageService).store(anyString(), any(), eq("application/pdf"));
    }

    // FILE-TC-004: C1 — invalid MIME type → FILE-001 / 415
    @Test
    void uploadFile_invalidMimeType_throwsBusinessException415() {
        byte[] exeBytes = new byte[]{0x4D, 0x5A, 0x00, 0x00}; // MZ header (Windows PE)
        MockMultipartFile exeFile = new MockMultipartFile("file", "virus.exe",
                "application/x-msdownload", exeBytes);

        assertThatThrownBy(() -> fileService.uploadFile(exeFile, CALLER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo("FILE-001");
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
                });

        verify(storageService, never()).store(any(), any(), any());
    }

    // FILE-TC-005: C2 — storageKey must be UUID-based (not originalName)
    @Test
    void uploadFile_storageKeyIsUuidBased() {
        when(fileRepository.countByOwnerUserIdAndStatus(any(), any())).thenReturn(0L);
        when(fileRepository.save(any())).thenReturn(savedFile(FILE_ID));
        when(storageService.generatePresignedUrl(any(), anyInt())).thenReturn("https://url");

        fileService.uploadFile(makeFile("image/jpeg", 1000), CALLER_ID);

        verify(storageService).store(argThat(key ->
                !key.contains("photo.jpg") && key.length() > 10), any(), any());
    }
}
