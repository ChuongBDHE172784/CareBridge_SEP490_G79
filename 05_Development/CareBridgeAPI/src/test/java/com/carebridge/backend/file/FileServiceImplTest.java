package com.carebridge.backend.file;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.common.exception.AccessDeniedBusinessException;
import com.carebridge.backend.file.dto.UploadFileResponse;
import com.carebridge.backend.file.entity.FileStatus;
import com.carebridge.backend.file.entity.UploadedFile;
import com.carebridge.backend.file.enums.FilePurpose;
import com.carebridge.backend.file.enums.FileAccessMode;
import com.carebridge.backend.file.enums.FileKind;
import com.carebridge.backend.file.repository.UploadedFileRepository;
import com.carebridge.backend.file.service.impl.CloudinaryStorageService;
import com.carebridge.backend.file.service.impl.R2StorageService;
import com.carebridge.backend.file.service.IStorageService;
import com.carebridge.backend.file.service.impl.FileServiceImpl;
import com.carebridge.backend.file.policy.FileAccessPolicy;
import com.carebridge.backend.file.policy.FileDeletePolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceImplTest {

    @Mock private UploadedFileRepository fileRepository;
    @Mock private CloudinaryStorageService cloudinaryStorageService;
    @Mock private R2StorageService r2StorageService;
    @Mock private ObjectProvider<R2StorageService> r2StorageServiceProvider;
    @Mock private AuditService auditService;
    @Mock private FileAccessPolicy fileAccessPolicy;
    @Mock private FileDeletePolicy fileDeletePolicy;

    private FileServiceImpl fileService;

    private static final UUID CALLER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID FILE_ID   = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @BeforeEach
    void setUp() {
        lenient().when(r2StorageServiceProvider.getIfAvailable()).thenReturn(r2StorageService);
        fileService = new FileServiceImpl(
                fileRepository,
                cloudinaryStorageService,
                r2StorageServiceProvider,
                auditService,
                fileAccessPolicy,
                fileDeletePolicy,
                "cloudinary" // default provider
        );
    }

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

    private void saveReturnsPersistedArgument() {
        when(fileRepository.save(any())).thenAnswer(invocation -> {
            UploadedFile file = invocation.getArgument(0);
            file.setId(FILE_ID);
            return file;
        });
    }

    // FILE-TC-001: Happy path JPEG upload
    @Test
    void uploadFile_validJpeg_returnsPresignedUrl() {
        when(fileRepository.countByOwnerUserIdAndStatus(CALLER_ID, FileStatus.ACTIVE)).thenReturn(0L);
        when(fileRepository.save(any())).thenReturn(savedFile(FILE_ID));
        when(cloudinaryStorageService.generatePresignedUrl(any(), eq(15))).thenReturn("https://presigned.url/file");

        UploadFileResponse resp = fileService.uploadFile(makeFile("image/jpeg", 1000), CALLER_ID);

        assertThat(resp.getFileId()).isEqualTo(FILE_ID);
        assertThat(resp.getPresignedUrl()).isNotBlank();
    }

    // FILE-TC-002: C3 — presigned URL TTL must be exactly 15 minutes
    @Test
    void uploadFile_presignedUrlTtlIs15Minutes() {
        when(fileRepository.countByOwnerUserIdAndStatus(any(), any())).thenReturn(0L);
        when(fileRepository.save(any())).thenReturn(savedFile(FILE_ID));
        when(cloudinaryStorageService.generatePresignedUrl(any(), eq(15))).thenReturn("https://presigned.url/file");

        fileService.uploadFile(makeFile("image/jpeg", 1000), CALLER_ID);

        verify(cloudinaryStorageService).generatePresignedUrl(any(), eq(15));
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

        verify(cloudinaryStorageService, never()).store(any(), any(), any());
    }

    @Test
    void uploadWithPurpose_callRecordingAllowsMoreThanTwentyMegabytes() {
        byte[] content = new byte[20 * 1024 * 1024 + 1];
        MultipartFile recording = new MockMultipartFile(
                "file", "recording.mp3", "audio/mpeg", content);
        when(fileRepository.countByOwnerUserIdAndStatus(CALLER_ID, FileStatus.ACTIVE))
                .thenReturn(0L);
        saveReturnsPersistedArgument();
        when(r2StorageService.persistedKey(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(r2StorageService.generatePresignedUrl(any(), eq(15))).thenReturn("https://presigned.url/file");

        assertThatCode(() -> fileService.uploadWithPurpose(
                recording,
                CALLER_ID,
                FileKind.DOCUMENT,
                FilePurpose.CONSULTATION_CALL_RECORDING,
                FileAccessMode.PRIVATE))
                .doesNotThrowAnyException();
    }

    // FILE-TC-004: C4 — storage quota check before write
    @Test
    void uploadFile_quotaExceeded_throwsBusinessException409() {
        when(fileRepository.countByOwnerUserIdAndStatus(CALLER_ID, FileStatus.ACTIVE)).thenReturn(500L);

        assertThatThrownBy(() -> fileService.uploadFile(makeFile("image/jpeg", 1000), CALLER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getHttpStatus())
                        .isEqualTo(HttpStatus.CONFLICT));

        verify(cloudinaryStorageService, never()).store(any(), any(), any());
    }

    // FILE-TC-002: Valid PDF upload (magic bytes %PDF) — routes to R2, no fallback to Cloudinary
    @Test
    void uploadFile_validPdf_routesToR2() {
        byte[] pdfBytes = new byte[100];
        pdfBytes[0] = 0x25; pdfBytes[1] = 0x50; pdfBytes[2] = 0x44; pdfBytes[3] = 0x46; // %PDF
        MockMultipartFile pdfFile = new MockMultipartFile("file", "report.pdf", "application/pdf", pdfBytes);
        when(fileRepository.countByOwnerUserIdAndStatus(CALLER_ID, FileStatus.ACTIVE)).thenReturn(0L);
        when(fileRepository.save(any())).thenReturn(savedFile(FILE_ID));
        when(r2StorageService.generatePresignedUrl(any(), eq(15))).thenReturn("https://presigned.url/report.pdf");

        UploadFileResponse resp = fileService.uploadFile(pdfFile, CALLER_ID);

        // PDF magic bytes detected — upload succeeds (no exception, fileId returned)
        assertThat(resp.getFileId()).isEqualTo(FILE_ID);
        assertThat(resp.getPresignedUrl()).isNotBlank();
        verify(r2StorageService).store(anyString(), any(), eq("application/pdf"));
        verify(cloudinaryStorageService, never()).store(any(), any(), any());
    }

    // FILE-TC-002b: uploadPublicFile() rejects non-image files (images only)
    @Test
    void uploadPublicFile_rejectsPdfWithFile001() {
        byte[] pdfBytes = new byte[100];
        pdfBytes[0] = 0x25; pdfBytes[1] = 0x50; pdfBytes[2] = 0x44; pdfBytes[3] = 0x46; // %PDF
        MockMultipartFile pdfFile = new MockMultipartFile("file", "report.pdf", "application/pdf", pdfBytes);

        assertThatThrownBy(() -> fileService.uploadPublicFile(pdfFile, CALLER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo("FILE-001");
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
                });

        verify(cloudinaryStorageService, never()).store(any(), any(), any());
        verify(cloudinaryStorageService, never()).storePublic(any(), any(), any());
        verify(r2StorageService, never()).store(any(), any(), any());
    }

    // FILE-TC-002c: uploadPublicFile() accepts images and routes to storePublic() — a dedicated
    // path separate from the shared store() used by expert identity / contribution / generic
    // uploads (ADR-RTE-007, decoupled design — ContentRichTextEditor_TDS.md). Regression guard:
    // PUBLIC images must never fall back to the shared store(), which cannot express PUBLIC intent.
    @Test
    void uploadPublicFile_acceptsJpeg() {
        when(fileRepository.countByOwnerUserIdAndStatus(CALLER_ID, FileStatus.ACTIVE)).thenReturn(0L);
        when(fileRepository.save(any())).thenReturn(savedFile(FILE_ID));
        when(cloudinaryStorageService.generatePresignedUrl(any(), eq(15))).thenReturn("https://presigned.url/image.jpg");

        UploadFileResponse resp = fileService.uploadPublicFile(makeFile("image/jpeg", 1000), CALLER_ID);

        assertThat(resp.getFileId()).isEqualTo(FILE_ID);
        assertThat(resp.getPresignedUrl()).isNotBlank();
        verify(cloudinaryStorageService).storePublic(anyString(), any(), eq("image/jpeg"));
        verify(cloudinaryStorageService, never()).store(any(), any(), any());
        verify(r2StorageService, never()).store(any(), any(), any());
    }

    // Health record upload accepts only images and PDF.
    @Test
    void uploadHealthRecordFile_acceptsJpegAsPrivateMedicalImage() {
        when(fileRepository.countByOwnerUserIdAndStatus(CALLER_ID, FileStatus.ACTIVE)).thenReturn(0L);
        saveReturnsPersistedArgument();
        when(cloudinaryStorageService.generatePresignedUrl(any(), eq(15))).thenReturn("https://presigned.url/image.jpg");

        UploadFileResponse resp = fileService.uploadHealthRecordFile(makeFile("image/jpeg", 1000), CALLER_ID);

        assertThat(resp.getFileId()).isEqualTo(FILE_ID);
        verify(cloudinaryStorageService).store(anyString(), any(), eq("image/jpeg"));
        verify(cloudinaryStorageService, never()).storePublic(any(), any(), any());
        verify(fileRepository).save(argThat(file -> file.getPurpose() == FilePurpose.MEDICAL_CONTRIBUTION_IMAGE));
        verify(r2StorageService, never()).store(any(), any(), any());
    }

    @Test
    void uploadHealthRecordFile_acceptsPdfAsPrivateMedicalDocument() {
        byte[] pdfBytes = new byte[100];
        pdfBytes[0] = 0x25; pdfBytes[1] = 0x50; pdfBytes[2] = 0x44; pdfBytes[3] = 0x46; // %PDF
        MockMultipartFile pdfFile = new MockMultipartFile("file", "report.pdf", "application/pdf", pdfBytes);
        when(fileRepository.countByOwnerUserIdAndStatus(CALLER_ID, FileStatus.ACTIVE)).thenReturn(0L);
        saveReturnsPersistedArgument();
        when(r2StorageService.generatePresignedUrl(any(), eq(15))).thenReturn("https://presigned.url/report.pdf");

        UploadFileResponse resp = fileService.uploadHealthRecordFile(pdfFile, CALLER_ID);

        assertThat(resp.getFileId()).isEqualTo(FILE_ID);
        verify(r2StorageService).store(anyString(), any(), eq("application/pdf"));
        verify(fileRepository).save(argThat(file -> file.getPurpose() == FilePurpose.MEDICAL_CONTRIBUTION_DOCUMENT));
        verify(cloudinaryStorageService, never()).store(any(), any(), any());
    }

    @Test
    void uploadHealthRecordFile_rejectsDocxWithFile001() {
        byte[] docxBytes = new byte[]{0x50, 0x4B, 0x03, 0x04};
        MockMultipartFile docxFile = new MockMultipartFile("file", "report.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", docxBytes);

        assertThatThrownBy(() -> fileService.uploadHealthRecordFile(docxFile, CALLER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo("FILE-001");
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
                });

        verify(fileRepository, never()).countByOwnerUserIdAndStatus(any(), any());
        verify(cloudinaryStorageService, never()).store(any(), any(), any());
        verify(r2StorageService, never()).store(any(), any(), any());
    }

    // FILE-TC-004: C1 - invalid MIME type -> FILE-001 / 415
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

        verify(cloudinaryStorageService, never()).store(any(), any(), any());
    }

    // FILE-TC-005: C2 — storageKey must be UUID-based (not originalName)
    @Test
    void uploadFile_storageKeyIsUuidBased() {
        when(fileRepository.countByOwnerUserIdAndStatus(any(), any())).thenReturn(0L);
        when(fileRepository.save(any())).thenReturn(savedFile(FILE_ID));
        when(cloudinaryStorageService.generatePresignedUrl(any(), anyInt())).thenReturn("https://url");

        fileService.uploadFile(makeFile("image/jpeg", 1000), CALLER_ID);

        verify(cloudinaryStorageService).store(argThat(key ->
                !key.contains("photo.jpg") && key.length() > 10), any(), any());
    }

    @Test
    void uploadCommunityQuestionImage_persistsPermanentCloudinaryUrl() {
        String publicUrl = "https://res.cloudinary.com/demo/image/upload/v1/carebridge/question.jpg";
        when(fileRepository.countByOwnerUserIdAndStatus(CALLER_ID, FileStatus.ACTIVE)).thenReturn(0L);
        saveReturnsPersistedArgument();
        when(cloudinaryStorageService.persistedKey(anyString()))
                .thenReturn("carebridge/question|image|PUBLIC");
        when(cloudinaryStorageService.generatePresignedUrl("carebridge/question|image|PUBLIC", 15))
                .thenReturn(publicUrl);

        UploadFileResponse response = fileService.uploadWithPurpose(
                makeFile("image/jpeg", 1000), CALLER_ID,
                com.carebridge.backend.file.enums.FileKind.IMAGE,
                FilePurpose.COMMUNITY_QUESTION_IMAGE,
                com.carebridge.backend.file.enums.FileAccessMode.PUBLIC);

        assertThat(response.getPresignedUrl()).isEqualTo(publicUrl);
        verify(fileRepository, times(2)).save(argThat(file -> publicUrl.equals(file.getFileUrl())));
    }

    @Test
    void purgeCommunityImages_trackedImageDeletesAssetAndMarksAttachmentDeleted() {
        String url = "https://res.cloudinary.com/demo/image/upload/v1/carebridge/question.jpg";
        UploadedFile tracked = savedFile(FILE_ID);
        tracked.setStorageKey("carebridge/question|image|PUBLIC");
        tracked.setFileUrl(url);
        when(fileRepository.findAllByOwnerUserIdAndFileUrlInAndStatus(
                eq(CALLER_ID), anyCollection(), eq(FileStatus.ACTIVE)))
                .thenReturn(List.of(tracked));

        fileService.purgeCommunityImages(List.of(url), CALLER_ID);

        verify(cloudinaryStorageService).deleteRequired("carebridge/question|image|PUBLIC");
        verify(fileRepository).saveAll(argThat(files ->
                files.iterator().next().getStatus() == FileStatus.DELETED));
    }

    @Test
    void purgeCommunityImages_untrackedUrlIsRejectedInsteadOfDeletingUnownedAsset() {
        String url = "https://res.cloudinary.com/demo/image/upload/v1/carebridge/legacy.jpg";
        when(fileRepository.findAllByOwnerUserIdAndFileUrlInAndStatus(
                eq(CALLER_ID), anyCollection(), eq(FileStatus.ACTIVE)))
                .thenReturn(List.of());

        assertThatThrownBy(() -> fileService.purgeCommunityImages(List.of(url), CALLER_ID))
                .isInstanceOf(AccessDeniedBusinessException.class);

        verifyNoInteractions(cloudinaryStorageService);
        verify(fileRepository, never()).saveAll(any());
    }
}
