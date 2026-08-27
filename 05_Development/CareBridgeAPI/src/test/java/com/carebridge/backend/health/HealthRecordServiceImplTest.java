package com.carebridge.backend.health;


import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.file.entity.FileStatus;
import com.carebridge.backend.file.entity.UploadedFile;
import com.carebridge.backend.file.repository.UploadedFileRepository;
import com.carebridge.backend.file.service.IFileService;
import com.carebridge.backend.health.dto.AddHealthRecordRequest;
import com.carebridge.backend.health.dto.AddHealthRecordResponse;
import com.carebridge.backend.health.dto.HealthRecordDetailResponse;
import com.carebridge.backend.health.entity.HealthRecord;
import com.carebridge.backend.health.entity.HealthRecordStatus;
import com.carebridge.backend.health.entity.RecordType;
import com.carebridge.backend.health.repository.HealthRecordFileRepository;
import com.carebridge.backend.health.repository.HealthRecordRepository;
import com.carebridge.backend.health.service.impl.HealthRecordServiceImpl;
import com.carebridge.backend.family.repository.CareGroupMemberRepository;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.family.policy.CareGroupAuthorizationPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HealthRecordServiceImplTest {

    @Mock private HealthRecordRepository recordRepository;
    @Mock private HealthRecordFileRepository recordFileRepository;
    @Mock private UploadedFileRepository uploadedFileRepository;
    @Mock private IFileService fileService;
    @Mock private AuditService auditService;
    @Mock private CareGroupRepository careGroupRepository;
    @Mock private CareGroupMemberRepository careGroupMemberRepository;
    @Mock private CareGroupAuthorizationPolicy careGroupAuthorizationPolicy;
    @InjectMocks private HealthRecordServiceImpl healthRecordService;

    private static final UUID CALLER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID RECORD_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID FILE_ID   = UUID.fromString("00000000-0000-0000-0000-000000000003");

    private AddHealthRecordRequest makeRequest(List<UUID> fileIds) {
        AddHealthRecordRequest req = new AddHealthRecordRequest();
        req.setRecordType(RecordType.ULTRASOUND);
        req.setTitle("Week 20 Ultrasound");
        req.setRecordDate(LocalDate.now());
        req.setFileIds(fileIds);
        return req;
    }

    private HealthRecord savedRecord(UUID id) {
        return HealthRecord.builder()
                .id(id)
                .ownerUserId(CALLER_ID)
                .recordType(RecordType.ULTRASOUND)
                .title("Week 20 Ultrasound")
                .status(HealthRecordStatus.ACTIVE)
                .build();
    }

    private UploadedFile ownedFile() {
        return UploadedFile.builder()
                .id(FILE_ID)
                .ownerUserId(CALLER_ID)
                .storageKey("key/" + FILE_ID)
                .originalName("scan.jpg")
                .mimeType("image/jpeg")
                .fileSizeBytes(100_000L)
                .status(FileStatus.ACTIVE)
                .build();
    }

    private UploadedFile ownedDocxFile() {
        return UploadedFile.builder()
                .id(FILE_ID)
                .ownerUserId(CALLER_ID)
                .storageKey("key/" + FILE_ID)
                .originalName("report.docx")
                .mimeType("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                .fileSizeBytes(100_000L)
                .status(FileStatus.ACTIVE)
                .build();
    }

    // HEALTH-TC-001: Happy path — no files
    @Test
    void addHealthRecord_noFiles_returns201() {
        when(recordRepository.saveAndFlush(any())).thenReturn(savedRecord(RECORD_ID));

        AddHealthRecordResponse resp = healthRecordService.addHealthRecord(makeRequest(null), CALLER_ID);

        assertThat(resp.getId()).isEqualTo(RECORD_ID);
        assertThat(resp.getStatus()).isEqualTo("ACTIVE");
    }

    // HEALTH-TC-002: C1 — file ownership validated before save
    @Test
    void addHealthRecord_fileNotOwnedByCaller_throwsBusinessException403() {
        UUID strangerFileId = UUID.randomUUID();
        when(uploadedFileRepository.findAllByIdInAndOwnerUserIdAndStatus(
                List.of(strangerFileId), CALLER_ID, FileStatus.ACTIVE))
                .thenReturn(List.of()); // empty = ownership check fails

        assertThatThrownBy(() -> healthRecordService.addHealthRecord(makeRequest(List.of(strangerFileId)), CALLER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getHttpStatus())
                        .isEqualTo(HttpStatus.FORBIDDEN));

        verify(recordRepository, never()).saveAndFlush(any());
    }

    // HEALTH-TC-003: health record attachments accept only images and PDF.
    @Test
    void addHealthRecord_docxAttachment_throwsBusinessException415() {
        when(uploadedFileRepository.findAllByIdInAndOwnerUserIdAndStatus(
                List.of(FILE_ID), CALLER_ID, FileStatus.ACTIVE))
                .thenReturn(List.of(ownedDocxFile()));

        assertThatThrownBy(() -> healthRecordService.addHealthRecord(makeRequest(List.of(FILE_ID)), CALLER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo("FILE-001");
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
                });

        verify(recordRepository, never()).saveAndFlush(any());
    }

    // HEALTH-TC-004: C5 - no diagnosis in response
    @Test
    void addHealthRecord_noMedicalDiagnosisInResponse() {
        when(recordRepository.saveAndFlush(any())).thenReturn(savedRecord(RECORD_ID));

        AddHealthRecordResponse resp = healthRecordService.addHealthRecord(makeRequest(null), CALLER_ID);

        assertThat(resp.toString()).doesNotContainIgnoringCase("diagnosis");
        assertThat(resp.toString()).doesNotContainIgnoringCase("prescription");
    }

    // HEALTH-TC-004: View — owner can view active record
    @Test
    void getHealthRecord_ownerActive_returnsDetail() {
        HealthRecord record = savedRecord(RECORD_ID);
        when(recordRepository.findByIdAndStatus(RECORD_ID, HealthRecordStatus.ACTIVE))
                .thenReturn(Optional.of(record));
        when(recordFileRepository.findByHealthRecordIdOrderByDisplayOrderAsc(RECORD_ID))
                .thenReturn(List.of());

        HealthRecordDetailResponse resp = healthRecordService.getHealthRecord(RECORD_ID, CALLER_ID);

        assertThat(resp.getId()).isEqualTo(RECORD_ID);
    }

    // HEALTH-TC-005: View — ARCHIVED record → 404 (HEALTH-008)
    @Test
    void getHealthRecord_archivedRecord_throwsBusinessException404() {
        when(recordRepository.findByIdAndStatus(RECORD_ID, HealthRecordStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> healthRecordService.getHealthRecord(RECORD_ID, CALLER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getHttpStatus())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    // HEALTH-TC-004: View — presigned URL TTL must be exactly 15 minutes (ADR-FILE-004)
    @Test
    void getHealthRecord_withAttachedFiles_presignedUrlTtlIs15Minutes() {
        HealthRecord record = savedRecord(RECORD_ID);
        UUID fileId = UUID.fromString("00000000-0000-0000-0000-000000000004");
        Instant uploadedAt = Instant.parse("2026-07-29T02:30:00Z");
        com.carebridge.backend.health.entity.HealthRecordFile link =
                com.carebridge.backend.health.entity.HealthRecordFile.builder()
                        .healthRecordId(RECORD_ID)
                        .fileId(fileId)
                        .displayOrder(0)
                        .build();
        com.carebridge.backend.file.entity.UploadedFile uploadedFile =
                com.carebridge.backend.file.entity.UploadedFile.builder()
                        .id(fileId).ownerUserId(CALLER_ID)
                        .storageKey("files/" + fileId + ".jpg")
                        .originalName("scan.jpg").mimeType("image/jpeg")
                        .fileSizeBytes(100_000L)
                        .createdAt(uploadedAt)
                        .status(com.carebridge.backend.file.entity.FileStatus.ACTIVE)
                        .build();
        when(recordRepository.findByIdAndStatus(RECORD_ID, HealthRecordStatus.ACTIVE))
                .thenReturn(Optional.of(record));
        when(recordFileRepository.findByHealthRecordIdOrderByDisplayOrderAsc(RECORD_ID))
                .thenReturn(List.of(link));
        when(uploadedFileRepository.findByIdAndStatus(fileId, FileStatus.ACTIVE))
                .thenReturn(Optional.of(uploadedFile));
        when(fileService.generatePresignedUrl(eq(fileId), eq(CALLER_ID), eq(15))).thenReturn("https://presigned/url");

        HealthRecordDetailResponse response = healthRecordService.getHealthRecord(RECORD_ID, CALLER_ID);

        verify(fileService).generatePresignedUrl(eq(fileId), eq(CALLER_ID), eq(15));
        assertThat(response.getAttachments()).singleElement()
                .satisfies(attachment -> assertThat(attachment.getCreatedAt()).isEqualTo(uploadedAt));
    }

    // HEALTH-TC-005: View — response must NOT contain medical diagnosis (BR-SAFETY-001)
    @Test
    void getHealthRecord_noDiagnosisOrMedicalAdviceInResponse() throws Exception {
        HealthRecord record = savedRecord(RECORD_ID);
        when(recordRepository.findByIdAndStatus(RECORD_ID, HealthRecordStatus.ACTIVE))
                .thenReturn(Optional.of(record));
        when(recordFileRepository.findByHealthRecordIdOrderByDisplayOrderAsc(RECORD_ID))
                .thenReturn(List.of());

        com.carebridge.backend.health.dto.HealthRecordDetailResponse resp =
                healthRecordService.getHealthRecord(RECORD_ID, CALLER_ID);

        String json = new com.fasterxml.jackson.databind.ObjectMapper()
                .findAndRegisterModules().writeValueAsString(resp);
        assertThat(json).doesNotContainIgnoringCase("diagnosis");
        assertThat(json).doesNotContainIgnoringCase("medicalAdvice");
        assertThat(json).doesNotContainIgnoringCase("prescription");
    }

    // HEALTH-TC-006: View — non-owner → 403
    @Test
    void getHealthRecord_notOwner_throwsBusinessException403() {
        UUID otherOwner = UUID.randomUUID();
        HealthRecord record = HealthRecord.builder()
                .id(RECORD_ID).ownerUserId(otherOwner)
                .status(HealthRecordStatus.ACTIVE).build();
        when(recordRepository.findByIdAndStatus(RECORD_ID, HealthRecordStatus.ACTIVE))
                .thenReturn(Optional.of(record));

        assertThatThrownBy(() -> healthRecordService.getHealthRecord(RECORD_ID, CALLER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getHttpStatus())
                        .isEqualTo(HttpStatus.FORBIDDEN));
    }
}
