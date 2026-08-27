package com.carebridge.backend.health.service;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.file.repository.UploadedFileRepository;
import com.carebridge.backend.file.service.IStorageService;
import com.carebridge.backend.health.HealthRecord41TestFactory;
import com.carebridge.backend.health.dto.ArchiveHealthRecordResponse;
import com.carebridge.backend.health.repository.HealthRecordFileRepository;
import com.carebridge.backend.health.repository.HealthRecordRepository;
import com.carebridge.backend.health.service.impl.HealthRecordServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

import static com.carebridge.backend.health.HealthRecord41TestFactory.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HealthRecordServiceArchiveTest {

    @Mock private HealthRecordRepository recordRepository;
    @Mock private HealthRecordFileRepository recordFileRepository;
    @Mock private UploadedFileRepository uploadedFileRepository;
    @Mock private IStorageService storageService;
    @Mock private AuditService auditService;
    @InjectMocks private HealthRecordServiceImpl service;

    // HEALTH41-TC-001: Archive ACTIVE record -> status=ARCHIVED
    @Test
    void archiveRecord_activeRecord_setsStatusArchived() {
        when(recordRepository.findById(HR_001)).thenReturn(Optional.of(makeActiveRecord()));
        when(recordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ArchiveHealthRecordResponse response = service.archiveRecord(HR_001, ACC_001);

        assertThat(response.getStatus()).isEqualTo("ARCHIVED");
        assertThat(response.getHealthRecordId()).isEqualTo(HR_001);
        verify(recordRepository, times(1)).save(any());
        verify(auditService, times(1)).log(any(), eq(ACC_001), any(), any(), any());
    }

    // HEALTH41-TC-002: Idempotent — archive ARCHIVED record -> 200, no save, no audit
    @Test
    void archiveRecord_alreadyArchived_idempotentReturn() {
        when(recordRepository.findById(HR_002)).thenReturn(Optional.of(makeArchivedRecord()));

        ArchiveHealthRecordResponse response = service.archiveRecord(HR_002, ACC_001);

        assertThat(response.getStatus()).isEqualTo("ARCHIVED");
        verify(recordRepository, never()).save(any());
        verify(auditService, never()).log(any(), any(), any(), any(), any());
    }

    // HEALTH41-TC-003: Archive another user's record -> 403
    @Test
    void archiveRecord_notOwner_throwsForbidden() {
        when(recordRepository.findById(HR_003)).thenReturn(Optional.of(makeOtherUserActiveRecord()));

        assertThatThrownBy(() -> service.archiveRecord(HR_003, ACC_001))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(be.getCode()).isEqualTo("HEALTH-004");
                });

        verify(recordRepository, never()).save(any());
        verify(auditService, never()).log(any(), any(), any(), any(), any());
    }

    // HEALTH41-TC-004: Record not found -> 404
    @Test
    void archiveRecord_notFound_throwsNotFound() {
        UUID randomId = UUID.randomUUID();
        when(recordRepository.findById(randomId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.archiveRecord(randomId, ACC_001))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(be.getCode()).isEqualTo("HEALTH-007");
                });

        verify(recordRepository, never()).save(any());
    }
}
