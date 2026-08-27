package com.carebridge.backend.directchat.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.common.exception.ResourceNotFoundException;
import com.carebridge.backend.directchat.entity.ConversationCall;
import com.carebridge.backend.directchat.repository.ConversationCallRepository;
import com.carebridge.backend.directchat.repository.DirectConversationRepository;
import com.carebridge.backend.expert.repository.ExpertProfileRepository;
import com.carebridge.backend.file.entity.UploadedFile;
import com.carebridge.backend.file.repository.UploadedFileRepository;
import com.carebridge.backend.file.service.IFileService;
import com.carebridge.backend.file.service.IStorageService;
import com.carebridge.backend.file.service.StorageServiceResolver;
import com.carebridge.backend.security.repository.UserRepository;
import jakarta.persistence.LockModeType;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(MockitoExtension.class)
class AdminConsultationCallServiceImplTest {

    private static final UUID CALL_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID FILE_ID = UUID.fromString("00000000-0000-0000-0000-000000000102");
    private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000103");

    @Mock private ConversationCallRepository callRepository;
    @Mock private DirectConversationRepository conversationRepository;
    @Mock private UserRepository userRepository;
    @Mock private ExpertProfileRepository expertProfileRepository;
    @Mock private IFileService fileService;
    @Mock private UploadedFileRepository uploadedFileRepository;
    @Mock private StorageServiceResolver storageServiceResolver;
    @Mock private AuditService auditService;
    @Mock private IStorageService storageService;

    private AdminConsultationCallServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminConsultationCallServiceImpl(
                callRepository,
                conversationRepository,
                userRepository,
                expertProfileRepository,
                fileService,
                uploadedFileRepository,
                storageServiceResolver,
                auditService);
    }

    @Test
    void deleteRecording_deletesStorageBeforeDatabaseMetadataAndAudits() {
        ConversationCall call = recordedCall();
        UploadedFile file = recordingFile();
        when(callRepository.findByIdForUpdate(CALL_ID)).thenReturn(Optional.of(call));
        when(uploadedFileRepository.findById(FILE_ID)).thenReturn(Optional.of(file));
        when(storageServiceResolver.resolve("r2")).thenReturn(storageService);

        service.deleteRecording(CALL_ID, ADMIN_ID);

        InOrder order = inOrder(storageService, uploadedFileRepository, auditService);
        order.verify(storageService).delete("consultation/recordings/call.webm");
        order.verify(uploadedFileRepository).delete(file);
        order.verify(auditService).log(
                eq(AuditAction.DIRECT_CALL_STATE_CHANGED),
                eq(ADMIN_ID),
                eq("ConversationCall"),
                eq(CALL_ID.toString()),
                any(),
                eq("RECORDING_DELETED"),
                any(UUID.class));
        assertThat(call.getRecordingFileId()).isNull();
        assertThat(call.getRecordingStatus()).isEqualTo("NONE");
        assertThat(call.getRecordedDurationSeconds()).isNull();
    }

    @Test
    void deleteRecording_storageFailureLeavesDatabaseStateUntouched() {
        ConversationCall call = recordedCall();
        UploadedFile file = recordingFile();
        when(callRepository.findByIdForUpdate(CALL_ID)).thenReturn(Optional.of(call));
        when(uploadedFileRepository.findById(FILE_ID)).thenReturn(Optional.of(file));
        when(storageServiceResolver.resolve("r2")).thenReturn(storageService);
        org.mockito.Mockito.doThrow(new RuntimeException("provider unavailable"))
                .when(storageService).delete(file.getStorageKey());

        assertThatThrownBy(() -> service.deleteRecording(CALL_ID, ADMIN_ID))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(exception.getCode()).isEqualTo("DCC-013");
                });

        assertThat(call.getRecordingFileId()).isEqualTo(FILE_ID);
        assertThat(call.getRecordingStatus()).isEqualTo("READY");
        assertThat(call.getRecordedDurationSeconds()).isEqualTo(120);
        verify(uploadedFileRepository, never()).delete(any());
        verifyNoInteractions(auditService);
    }

    @Test
    void deleteRecording_missingAttachmentMetadataFailsClosed() {
        ConversationCall call = recordedCall();
        when(callRepository.findByIdForUpdate(CALL_ID)).thenReturn(Optional.of(call));
        when(uploadedFileRepository.findById(FILE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteRecording(CALL_ID, ADMIN_ID))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getCode()).isEqualTo("DCC-014");
                });

        assertThat(call.getRecordingFileId()).isEqualTo(FILE_ID);
        assertThat(call.getRecordingStatus()).isEqualTo("READY");
        assertThat(call.getRecordedDurationSeconds()).isEqualTo(120);
        verifyNoInteractions(storageServiceResolver, auditService);
        verify(uploadedFileRepository, never()).delete(any());
    }

    @Test
    void deleteRecording_withoutRecordingIsIdempotent() {
        ConversationCall call = ConversationCall.builder()
                .id(CALL_ID)
                .recordingFileId(null)
                .recordingStatus("NONE")
                .build();
        when(callRepository.findByIdForUpdate(CALL_ID)).thenReturn(Optional.of(call));

        service.deleteRecording(CALL_ID, ADMIN_ID);

        verifyNoInteractions(uploadedFileRepository, storageServiceResolver, auditService);
    }

    @Test
    void deleteRecording_unknownCallReturnsNotFound() {
        when(callRepository.findByIdForUpdate(CALL_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteRecording(CALL_ID, ADMIN_ID))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(uploadedFileRepository, storageServiceResolver, auditService);
    }

    @Test
    void deleteRecording_usesTransactionAndPessimisticWriteLock() throws Exception {
        Method serviceMethod = AdminConsultationCallServiceImpl.class
                .getDeclaredMethod("deleteRecording", UUID.class, UUID.class);
        Transactional transactional = serviceMethod.getAnnotation(Transactional.class);
        assertThat(transactional).isNotNull();
        assertThat(transactional.readOnly()).isFalse();

        Method repositoryMethod = ConversationCallRepository.class
                .getDeclaredMethod("findByIdForUpdate", UUID.class);
        assertThat(repositoryMethod.getAnnotation(Lock.class).value())
                .isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    }

    private ConversationCall recordedCall() {
        return ConversationCall.builder()
                .id(CALL_ID)
                .recordingFileId(FILE_ID)
                .recordingStatus("READY")
                .recordedDurationSeconds(120)
                .build();
    }

    private UploadedFile recordingFile() {
        return UploadedFile.builder()
                .id(FILE_ID)
                .ownerUserId(ADMIN_ID)
                .storageProvider("r2")
                .storageKey("consultation/recordings/call.webm")
                .originalName("call.webm")
                .mimeType("video/webm")
                .fileSizeBytes(1024)
                .build();
    }
}
