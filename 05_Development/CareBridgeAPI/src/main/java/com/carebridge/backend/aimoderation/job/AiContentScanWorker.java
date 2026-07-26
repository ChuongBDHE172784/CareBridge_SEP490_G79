package com.carebridge.backend.aimoderation.job;

import com.carebridge.backend.aimoderation.service.AiScanProcessingService;
import com.carebridge.backend.integration.gemini.client.GeminiModerationClient;
import com.carebridge.backend.systemconfiguration.entity.SystemConfiguration;
import com.carebridge.backend.systemconfiguration.repository.SystemConfigurationRepository;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Polls the durable ai_content_scan_jobs queue (same thin-wrapper convention as the
 * notification outbox jobs). Claims happen on the scheduler thread (fast DB ops only);
 * each claimed job is dispatched via @Async so the shared single-threaded scheduler —
 * which also runs the 1s safety countdown job — is never blocked on a Gemini HTTP call.
 * Gated on both the infra flag (GEMINI_ENABLED) and the business toggle
 * (system_configurations.ai_moderation_enabled); while gated, jobs stay QUEUED and are
 * processed once scanning is re-enabled.
 */
@Component
@Slf4j
public class AiContentScanWorker {

    private final AiScanProcessingService processingService;
    private final GeminiModerationClient geminiModerationClient;
    private final SystemConfigurationRepository systemConfigurationRepository;
    private final int batchSize;
    private final String workerId;

    public AiContentScanWorker(AiScanProcessingService processingService,
            GeminiModerationClient geminiModerationClient,
            SystemConfigurationRepository systemConfigurationRepository,
            @Value("${carebridge.gemini.moderation.batch-size:3}") int batchSize) {
        this.processingService = processingService;
        this.geminiModerationClient = geminiModerationClient;
        this.systemConfigurationRepository = systemConfigurationRepository;
        this.batchSize = batchSize;
        this.workerId = "ai-scan-" + UUID.randomUUID();
    }

    @Scheduled(fixedDelayString = "${carebridge.gemini.moderation.worker-delay-ms:15000}")
    public void poll() {
        if (geminiModerationClient.configState() != GeminiModerationClient.ConfigState.READY) {
            return;
        }
        if (!businessToggleEnabled()) {
            return;
        }
        for (UUID jobId : processingService.claimDueJobs(workerId, batchSize)) {
            processingService.processJobAsync(jobId);
        }
    }

    private boolean businessToggleEnabled() {
        return systemConfigurationRepository.findFirstByOrderByCreatedAtAsc()
                .map(SystemConfiguration::isAiModerationEnabled)
                .orElse(true);
    }
}
