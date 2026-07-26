package com.carebridge.backend.aimoderation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.aimoderation.dto.request.CreateAiPolicyRequest;
import com.carebridge.backend.aimoderation.dto.request.UpdateAiPolicyRequest;
import com.carebridge.backend.aimoderation.entity.AiModerationPolicy;
import com.carebridge.backend.aimoderation.entity.AiPolicySeverity;
import com.carebridge.backend.aimoderation.entity.AiViolationCategory;
import com.carebridge.backend.aimoderation.exception.AiModerationException;
import com.carebridge.backend.aimoderation.mapper.AiModerationMapper;
import com.carebridge.backend.aimoderation.policy.AiModerationDecisionPolicy;
import com.carebridge.backend.aimoderation.policy.AiModerationPromptBuilder;
import com.carebridge.backend.aimoderation.policy.AiVerdictParser;
import com.carebridge.backend.aimoderation.repository.AiModerationPolicyRepository;
import com.carebridge.backend.aimoderation.service.AiPolicyServiceImpl;
import com.carebridge.backend.aimoderation.service.AiPolicySetService;
import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.content.entity.ReportCategory;
import com.carebridge.backend.content.entity.ReportTargetType;
import com.carebridge.backend.integration.gemini.client.GeminiModerationClient;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiPolicyServiceImplTest {

    @Mock
    private AiModerationPolicyRepository policyRepository;
    @Mock
    private AiModerationMapper mapper;
    @Mock
    private AuditService auditService;
    @Mock
    private AiPolicySetService policySetService;
    @Mock
    private AiModerationPromptBuilder promptBuilder;
    @Mock
    private AiVerdictParser verdictParser;
    @Mock
    private AiModerationDecisionPolicy decisionPolicy;
    @Mock
    private GeminiModerationClient geminiModerationClient;

    @InjectMocks
    private AiPolicyServiceImpl service;

    private static final UUID ACTOR = UUID.randomUUID();

    private static AiModerationPolicy existingPolicy(boolean systemDefault) {
        return AiModerationPolicy.builder()
                .id(UUID.randomUUID())
                .policyCode("SPAM_ADVERTISING")
                .name("Spam")
                .detectionGuidance("guidance cũ")
                .violationCategory(AiViolationCategory.SPAM_ADVERTISING)
                .reportCategory(ReportCategory.SPAM)
                .severity(AiPolicySeverity.MEDIUM)
                .applicableTargetTypes("QUESTION,ANSWER")
                .confidenceThreshold(new BigDecimal("0.700"))
                .active(true)
                .systemDefault(systemDefault)
                .version(1)
                .build();
    }

    // Scenario 19 support: duplicate code rejected with AIM-002
    @Test
    void create_duplicateCode_throwsConflict() {
        when(policyRepository.existsByPolicyCodeIgnoreCase("SPAM_ADVERTISING")).thenReturn(true);
        CreateAiPolicyRequest request = new CreateAiPolicyRequest("SPAM_ADVERTISING", "Spam", "g",
                AiViolationCategory.SPAM_ADVERTISING, ReportCategory.SPAM, AiPolicySeverity.MEDIUM,
                List.of(ReportTargetType.QUESTION), new BigDecimal("0.7"), true);
        assertThatThrownBy(() -> service.createPolicy(request, ACTOR))
                .isInstanceOf(AiModerationException.class)
                .extracting(ex -> ((AiModerationException) ex).getCode())
                .isEqualTo("AIM-002");
    }

    @Test
    void create_invalidTargetTypes_throwsAim003() {
        when(policyRepository.existsByPolicyCodeIgnoreCase(anyString())).thenReturn(false);
        CreateAiPolicyRequest request = new CreateAiPolicyRequest("NEW_POLICY", "n", "g",
                AiViolationCategory.OTHER, ReportCategory.OTHER, AiPolicySeverity.LOW,
                List.of(ReportTargetType.ACCOUNT), new BigDecimal("0.7"), true);
        assertThatThrownBy(() -> service.createPolicy(request, ACTOR))
                .isInstanceOf(AiModerationException.class)
                .extracting(ex -> ((AiModerationException) ex).getCode())
                .isEqualTo("AIM-003");
    }

    @Test
    void create_invalidThreshold_throwsAim004() {
        when(policyRepository.existsByPolicyCodeIgnoreCase(anyString())).thenReturn(false);
        CreateAiPolicyRequest request = new CreateAiPolicyRequest("NEW_POLICY", "n", "g",
                AiViolationCategory.OTHER, ReportCategory.OTHER, AiPolicySeverity.LOW,
                List.of(ReportTargetType.QUESTION), new BigDecimal("1.5"), true);
        assertThatThrownBy(() -> service.createPolicy(request, ACTOR))
                .isInstanceOf(AiModerationException.class)
                .extracting(ex -> ((AiModerationException) ex).getCode())
                .isEqualTo("AIM-004");
    }

    // Version bumps only for classification-affecting changes — and is audited
    @Test
    void update_guidanceChange_bumpsVersionAndAudits() {
        AiModerationPolicy policy = existingPolicy(true);
        when(policyRepository.findById(policy.getId())).thenReturn(Optional.of(policy));
        when(policyRepository.save(any(AiModerationPolicy.class))).thenAnswer(inv -> inv.getArgument(0));

        service.updatePolicy(policy.getId(),
                new UpdateAiPolicyRequest(null, "guidance mới chặt chẽ hơn", null, null, null, null, null, null),
                ACTOR);

        assertThat(policy.getVersion()).isEqualTo(2);
        verify(auditService).log(eq(AuditAction.AI_POLICY_UPDATED), eq(ACTOR), eq("AiModerationPolicy"),
                anyString(), any());
    }

    @Test
    void update_nameOnlyChange_doesNotBumpVersion() {
        AiModerationPolicy policy = existingPolicy(false);
        when(policyRepository.findById(policy.getId())).thenReturn(Optional.of(policy));
        when(policyRepository.save(any(AiModerationPolicy.class))).thenAnswer(inv -> inv.getArgument(0));

        service.updatePolicy(policy.getId(),
                new UpdateAiPolicyRequest("Tên mới", null, null, null, null, null, null, null), ACTOR);

        assertThat(policy.getVersion()).isEqualTo(1);
    }

    // System defaults may be deactivated (audited, version-bumped) but never hard-deleted:
    // the service exposes no delete operation at all — enforced at compile time.
    @Test
    void statusChange_bumpsVersionAndAudits() {
        AiModerationPolicy policy = existingPolicy(true);
        when(policyRepository.findById(policy.getId())).thenReturn(Optional.of(policy));
        when(policyRepository.save(any(AiModerationPolicy.class))).thenAnswer(inv -> inv.getArgument(0));

        service.updatePolicyStatus(policy.getId(), false, ACTOR);

        assertThat(policy.isActive()).isFalse();
        assertThat(policy.getVersion()).isEqualTo(2);
        verify(auditService).log(eq(AuditAction.AI_POLICY_STATUS_CHANGED), eq(ACTOR),
                eq("AiModerationPolicy"), anyString(), any());
    }

    // Sandbox refuses politely when Gemini is not configured (no fake results)
    @Test
    void sandbox_whenGeminiDisabled_throwsAim009() {
        when(geminiModerationClient.configState()).thenReturn(GeminiModerationClient.ConfigState.DISABLED);
        assertThatThrownBy(() -> service.testPolicies(
                new com.carebridge.backend.aimoderation.dto.request.AiPolicyTestRequest(
                        ReportTargetType.QUESTION, "text"), ACTOR))
                .isInstanceOf(AiModerationException.class)
                .extracting(ex -> ((AiModerationException) ex).getCode())
                .isEqualTo("AIM-009");
    }
}
