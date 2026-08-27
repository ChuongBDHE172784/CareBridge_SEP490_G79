package com.carebridge.backend.aimoderation;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.aimoderation.job.AiContentScanWorker;
import com.carebridge.backend.aimoderation.service.AiScanProcessingService;
import com.carebridge.backend.integration.gemini.client.GeminiModerationClient;
import com.carebridge.backend.systemconfiguration.repository.SystemConfigurationRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiContentScanWorkerTest {

    @Mock private AiScanProcessingService processingService;
    @Mock private GeminiModerationClient geminiModerationClient;
    @Mock private SystemConfigurationRepository configurationRepository;

    @Test
    void unavailableProvider_routesAlreadyQueuedJobsToHumanReview() {
        UUID jobId = UUID.randomUUID();
        when(geminiModerationClient.configState())
                .thenReturn(GeminiModerationClient.ConfigState.DISABLED);
        when(processingService.claimDueCommunityJobs(anyString(), eq(3))).thenReturn(List.of(jobId));
        AiContentScanWorker worker = new AiContentScanWorker(
                processingService, geminiModerationClient, configurationRepository, 3);

        worker.poll();

        verify(processingService).routeClaimedJobToHumanAsync(jobId, "AI_NOT_READY");
        verify(processingService, never()).claimDueJobs(anyString(), eq(3));
        verify(processingService, never()).processJobAsync(jobId);
    }
}
