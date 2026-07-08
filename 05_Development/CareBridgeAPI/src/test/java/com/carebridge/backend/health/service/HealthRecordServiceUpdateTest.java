package com.carebridge.backend.health.service;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.file.repository.UploadedFileRepository;
import com.carebridge.backend.file.service.IStorageService;
import com.carebridge.backend.health.HealthRecord40TestFactory;
import com.carebridge.backend.health.dto.UpdateHealthRecordRequest;
import com.carebridge.backend.health.dto.UpdateHealthRecordResponse;
import com.carebridge.backend.health.entity.HealthRecord;
import com.carebridge.backend.health.entity.HealthRecordStatus;
import com.carebridge.backend.health.repository.HealthRecordFileRepository;
import com.carebridge.backend.health.repository.HealthRecordRepository;
import com.carebridge.backend.health.service.impl.HealthRecordServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

import static com.carebridge.backend.health.HealthRecord40TestFactory.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HealthRecordServiceUpdateTest {

    @Mock private HealthRecordRepository recordRepository;
    @Mock private HealthRecordFileRepository recordFileRepository;
    @Mock private UploadedFileRepository uploadedFileRepository;
    @Mock private IStorageService storageService;
    @Mock private AuditService auditService;
    @InjectMocks private HealthRecordServiceImpl service;

    // HEALTH40-TC-001: Happy path — valid PATCH on ACTIVE record
    @Test
    void updateHealthRecord_activeRecord_returnsUpdatedResponse() {
        HealthRecord existing = makeActiveRecord();
        HealthRecord saved = makeActiveRecord();
        saved.setTitle("Updated Title");

        when(recordRepository.findById(HR_001)).thenReturn(Optional.of(existing));
        when(recordRepository.save(any(HealthRecord.class))).thenReturn(saved);

        UpdateHealthRecordResponse response =
                service.updateHealthRecord(HR_001, makeValidRequest(), ACC_001);

        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Updated Title");
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        verify(recordRepository, times(1)).save(any(HealthRecord.class));
        verify(auditService, times(1)).log(any(), eq(ACC_001), any(), any(), any());
    }

    // HEALTH40-TC-002: ARCHIVED record -> 409
    @Test
    void updateHealthRecord_archivedRecord_throwsConflict() {
        when(recordRepository.findById(HR_002)).thenReturn(Optional.of(makeArchivedRecord()));

        assertThatThrownBy(() -> service.updateHealthRecord(HR_002, makeValidRequest(), ACC_001))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(be.getCode()).isEqualTo("HEALTH-006");
                });

        verify(recordRepository, never()).save(any());
        verify(auditService, never()).log(any(), any(), any(), any(), any());
    }

    // HEALTH40-TC-003: Another user's record -> 403
    @Test
    void updateHealthRecord_notOwner_throwsForbidden() {
        when(recordRepository.findById(HR_003)).thenReturn(Optional.of(makeOtherUserRecord()));

        assertThatThrownBy(() -> service.updateHealthRecord(HR_003, makeValidRequest(), ACC_001))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(be.getCode()).isEqualTo("HEALTH-004");
                });

        verify(recordRepository, never()).save(any());
    }

    // HEALTH40-TC-004: Record not found -> 404
    @Test
    void updateHealthRecord_notFound_throwsNotFound() {
        UUID randomId = UUID.randomUUID();
        when(recordRepository.findById(randomId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateHealthRecord(randomId, makeValidRequest(), ACC_001))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(be.getCode()).isEqualTo("HEALTH-007");
                });
    }

    // HEALTH40-TC-007: All-null request (no-op) — original fields unchanged
    @Test
    void updateHealthRecord_allNullRequest_originalFieldsUnchanged() {
        HealthRecord existing = makeActiveRecord();
        String originalTitle = existing.getTitle();

        when(recordRepository.findById(HR_001)).thenReturn(Optional.of(existing));
        when(recordRepository.save(any(HealthRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        service.updateHealthRecord(HR_001, makeAllNullRequest(), ACC_001);

        ArgumentCaptor<HealthRecord> captor = ArgumentCaptor.forClass(HealthRecord.class);
        verify(recordRepository).save(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo(originalTitle);
        assertThat(captor.getValue().getStatus()).isEqualTo(HealthRecordStatus.ACTIVE);
    }
}
