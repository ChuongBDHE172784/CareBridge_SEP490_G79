package com.carebridge.backend.aimoderation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.aimoderation.entity.AiContentScanJob;
import com.carebridge.backend.aimoderation.exception.AiModerationException;
import com.carebridge.backend.aimoderation.policy.AiContentHasher;
import com.carebridge.backend.aimoderation.repository.AiContentScanJobRepository;
import com.carebridge.backend.aimoderation.service.AiScanEnqueueService;
import com.carebridge.backend.aimoderation.service.AiScanTargetResolver;
import com.carebridge.backend.content.entity.ReportTargetType;
import com.carebridge.backend.systemconfiguration.entity.SystemConfiguration;
import com.carebridge.backend.systemconfiguration.repository.SystemConfigurationRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiScanEnqueueServiceTest {

    @Mock
    private AiContentScanJobRepository jobRepository;

    @Mock
    private SystemConfigurationRepository systemConfigurationRepository;

    @Mock
    private AiScanTargetResolver targetResolver;

    @InjectMocks
    private AiScanEnqueueService enqueueService;

    private static final UUID TARGET_ID = UUID.randomUUID();
    private static final String TEXT = "Câu hỏi về dinh dưỡng thai kỳ";

    private void givenBusinessToggle(boolean enabled) {
        SystemConfiguration config = SystemConfiguration.builder().aiModerationEnabled(enabled).build();
        when(systemConfigurationRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(config));
    }

    // Scenario A: enqueue is a same-transaction insert with the content hash
    @Test
    void enqueue_createsQueuedJobWithContentHash() {
        givenBusinessToggle(true);
        when(jobRepository.existsByTargetTypeAndTargetIdAndContentHashAndStatusIn(any(), any(), anyString(), any()))
                .thenReturn(false);
        when(jobRepository.save(any(AiContentScanJob.class))).thenAnswer(inv -> inv.getArgument(0));

        enqueueService.enqueueScan(ReportTargetType.QUESTION, TARGET_ID, TEXT);

        ArgumentCaptor<AiContentScanJob> captor = ArgumentCaptor.forClass(AiContentScanJob.class);
        verify(jobRepository).save(captor.capture());
        assertThat(captor.getValue().getContentHash()).isEqualTo(AiContentHasher.sha256Hex(TEXT));
    }

    // Business toggle off → silent no-op, content creation is never blocked
    @Test
    void businessToggleOff_skipsEnqueueSilently() {
        givenBusinessToggle(false);
        enqueueService.enqueueScan(ReportTargetType.QUESTION, TARGET_ID, TEXT);
        verify(jobRepository, never()).save(any());
    }

    // Scenario E: duplicate active job for the same content version collapses
    @Test
    void duplicateActiveJob_isCollapsed() {
        givenBusinessToggle(true);
        when(jobRepository.existsByTargetTypeAndTargetIdAndContentHashAndStatusIn(any(), any(), anyString(), any()))
                .thenReturn(true);
        enqueueService.enqueueScan(ReportTargetType.QUESTION, TARGET_ID, TEXT);
        verify(jobRepository, never()).save(any());
    }

    // Scenario 15: changed content produces a different hash → a new job is created
    @Test
    void changedContent_createsNewJob() {
        givenBusinessToggle(true);
        String editedText = TEXT + " (đã chỉnh sửa)";
        when(jobRepository.existsByTargetTypeAndTargetIdAndContentHashAndStatusIn(
                eq(ReportTargetType.QUESTION), eq(TARGET_ID),
                eq(AiContentHasher.sha256Hex(editedText)), any()))
                .thenReturn(false);
        when(jobRepository.save(any(AiContentScanJob.class))).thenAnswer(inv -> inv.getArgument(0));

        enqueueService.enqueueScan(ReportTargetType.QUESTION, TARGET_ID, editedText);

        verify(jobRepository).save(any(AiContentScanJob.class));
    }

    @Test
    void blankText_isNeverEnqueued() {
        enqueueService.enqueueScan(ReportTargetType.QUESTION, TARGET_ID, "   ");
        verify(jobRepository, never()).save(any());
    }

    // Scope guard: only QUESTION/ANSWER/CONTENT are scannable
    @Test
    void unsupportedTarget_isNeverEnqueued() {
        enqueueService.enqueueScan(ReportTargetType.USER, TARGET_ID, TEXT);
        verify(jobRepository, never()).save(any());
    }

    @Test
    void rescan_unsupportedTarget_throwsAim010() {
        assertThatThrownBy(() -> enqueueService.enqueueRescan(ReportTargetType.EXPERT, TARGET_ID))
                .isInstanceOf(AiModerationException.class)
                .extracting(ex -> ((AiModerationException) ex).getCode())
                .isEqualTo("AIM-010");
    }

    @Test
    void rescan_forceFlagBypassesIdempotencySkip() {
        when(targetResolver.resolve(ReportTargetType.QUESTION, TARGET_ID))
                .thenReturn(AiScanTargetResolver.TargetContent.of(TEXT));
        when(jobRepository.existsByTargetTypeAndTargetIdAndContentHashAndStatusIn(any(), any(), anyString(), any()))
                .thenReturn(false);
        when(jobRepository.save(any(AiContentScanJob.class))).thenAnswer(inv -> {
            AiContentScanJob job = inv.getArgument(0);
            job.setId(UUID.randomUUID());
            return job;
        });

        UUID jobId = enqueueService.enqueueRescan(ReportTargetType.QUESTION, TARGET_ID);

        assertThat(jobId).isNotNull();
        ArgumentCaptor<AiContentScanJob> captor = ArgumentCaptor.forClass(AiContentScanJob.class);
        verify(jobRepository).save(captor.capture());
        assertThat(captor.getValue().isForceRescan()).isTrue();
    }
}
