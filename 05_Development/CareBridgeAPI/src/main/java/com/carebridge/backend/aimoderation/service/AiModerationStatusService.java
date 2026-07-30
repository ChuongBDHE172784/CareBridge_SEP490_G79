package com.carebridge.backend.aimoderation.service;

import com.carebridge.backend.aimoderation.dto.response.AiModerationStatusResponse;
import com.carebridge.backend.aimoderation.entity.AiAssessmentStatus;
import com.carebridge.backend.aimoderation.entity.AiContentAssessment;
import com.carebridge.backend.aimoderation.entity.AiScanJobStatus;
import com.carebridge.backend.aimoderation.repository.AiContentAssessmentRepository;
import com.carebridge.backend.aimoderation.repository.AiContentScanJobRepository;
import com.carebridge.backend.aimoderation.repository.AiModerationPolicyRepository;
import com.carebridge.backend.integration.gemini.client.GeminiModerationClient;
import com.carebridge.backend.systemconfiguration.entity.SystemConfiguration;
import com.carebridge.backend.systemconfiguration.repository.SystemConfigurationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

/**
 * Operational summary for the System Admin hub: configuration state (never the key itself),
 * queue depth and failure visibility so a Gemini outage is observable instead of silent.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiModerationStatusService {

    private final GeminiModerationClient geminiModerationClient;
    private final AiContentScanJobRepository jobRepository;
    private final AiContentAssessmentRepository assessmentRepository;
    private final AiModerationPolicyRepository policyRepository;
    private final AiPolicySetService policySetService;
    private final SystemConfigurationRepository systemConfigurationRepository;

    @Transactional(readOnly = true)
    public AiModerationStatusResponse status() {
        try {
            GeminiModerationClient.ConfigState state = geminiModerationClient.configState();
            boolean businessToggle = systemConfigurationRepository.findFirstByOrderByCreatedAtAsc()
                    .map(SystemConfiguration::isAiModerationEnabled)
                    .orElse(true);
            return new AiModerationStatusResponse(
                    state != GeminiModerationClient.ConfigState.DISABLED,
                    state == GeminiModerationClient.ConfigState.READY,
                    geminiModerationClient.model(),
                    state.name(),
                    businessToggle,
                    jobRepository.countByStatus(AiScanJobStatus.QUEUED),
                    jobRepository.countByStatus(AiScanJobStatus.PROCESSING),
                    jobRepository.countByStatus(AiScanJobStatus.FAILED),
                    assessmentRepository.findFirstByStatusOrderByCompletedAtDesc(AiAssessmentStatus.COMPLETED)
                            .map(AiContentAssessment::getCompletedAt)
                            .orElse(null),
                    policySetService.currentHash(),
                    policyRepository.findByActiveTrueOrderByPolicyCodeAsc().size());
        } catch (Exception ex) {
            log.error("Failed to calculate AI moderation status summary", ex);
            GeminiModerationClient.ConfigState state = GeminiModerationClient.ConfigState.NOT_CONFIGURED;
            try {
                state = geminiModerationClient.configState();
            } catch (Exception ignored) {}
            return new AiModerationStatusResponse(
                    false,
                    false,
                    geminiModerationClient.model(),
                    state.name(),
                    true,
                    0, 0, 0,
                    null,
                    "",
                    0);
        }
    }
}
