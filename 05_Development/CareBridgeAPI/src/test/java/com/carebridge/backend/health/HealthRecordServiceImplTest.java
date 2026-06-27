package com.carebridge.backend.health;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.file.entity.FileStatus;
import com.carebridge.backend.file.entity.UploadedFile;
import com.carebridge.backend.file.repository.UploadedFileRepository;
import com.carebridge.backend.file.service.IStorageService;
import com.carebridge.backend.health.dto.AddHealthRecordRequest;
import com.carebridge.backend.health.dto.AddHealthRecordResponse;
import com.carebridge.backend.health.dto.HealthRecordDetailResponse;
import com.carebridge.backend.health.entity.HealthRecord;
import com.carebridge.backend.health.entity.HealthRecordFile;
import com.carebridge.backend.health.entity.HealthRecordStatus;
import com.carebridge.backend.health.entity.RecordType;
import com.carebridge.backend.health.repository.HealthRecordFileRepository;
import com.carebridge.backend.health.repository.HealthRecordRepository;
import com.carebridge.backend.health.service.impl.HealthRecordServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

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
    @Mock private IStorageService storageService;
    @Mock private AuditService auditService;
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

    // HEALTH-TC-001: Happy path — no files
    @Test
    void addHealthRecord_noFiles_returns201() {
        when(recordRepository.save(any())).thenReturn(savedRecord(RECORD_ID));

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

        verify(recordRepository, never()).save(any());
    }

    // HEALTH-TC-003: C5 — no diagnosis in response
    @Test
    void addHealthRecord_noMedicalDiagnosisInResponse() {
        when(recordRepository.save(any())).thenReturn(savedRecord(RECORD_ID));

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
