package com.carebridge.backend.aimoderation.service;

import com.carebridge.backend.aimoderation.dto.AiVerdict;
import com.carebridge.backend.aimoderation.dto.request.AiPolicyTestRequest;
import com.carebridge.backend.aimoderation.dto.request.CreateAiPolicyRequest;
import com.carebridge.backend.aimoderation.dto.request.UpdateAiPolicyRequest;
import com.carebridge.backend.aimoderation.dto.response.AiPolicyPageResponse;
import com.carebridge.backend.aimoderation.dto.response.AiPolicyResponse;
import com.carebridge.backend.aimoderation.dto.response.AiPolicyTestResponse;
import com.carebridge.backend.aimoderation.entity.AiModerationPolicy;
import com.carebridge.backend.aimoderation.exception.AiModerationException;
import com.carebridge.backend.aimoderation.exception.AiVerdictParseException;
import com.carebridge.backend.aimoderation.mapper.AiModerationMapper;
import com.carebridge.backend.aimoderation.policy.AiModerationDecisionPolicy;
import com.carebridge.backend.aimoderation.policy.AiModerationDecisionPolicy.CaseDecision;
import com.carebridge.backend.aimoderation.policy.AiModerationPromptBuilder;
import com.carebridge.backend.aimoderation.policy.AiVerdictParser;
import com.carebridge.backend.aimoderation.repository.AiModerationPolicyRepository;
import com.carebridge.backend.aimoderation.service.AiPolicySetService.AiPolicySet;
import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.content.entity.ReportTargetType;
import com.carebridge.backend.integration.gemini.client.GeminiModerationClient;
import com.carebridge.backend.integration.gemini.client.GeminiModerationClient.ModerationCallResult;
import com.carebridge.backend.integration.gemini.exception.GeminiConfigurationException;
import com.carebridge.backend.integration.gemini.exception.GeminiUnavailableException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * SYSTEM_ADMIN management of AI moderation policies. Policies are validated data with length
 * limits and enum-constrained fields — not free-form system prompts. System defaults are
 * never hard-deleted (there is no delete operation at all); classification-affecting changes
 * bump the version, which rolls the policy-set hash and thereby forces fresh assessments.
 */
@Service
@RequiredArgsConstructor
public class AiPolicyServiceImpl implements AiPolicyService {

    private static final int MAX_GUIDANCE_LENGTH = 2000;

    private final AiModerationPolicyRepository policyRepository;
    private final AiModerationMapper mapper;
    private final AuditService auditService;
    private final AiPolicySetService policySetService;
    private final AiModerationPromptBuilder promptBuilder;
    private final AiVerdictParser verdictParser;
    private final AiModerationDecisionPolicy decisionPolicy;
    private final GeminiModerationClient geminiModerationClient;

    @Override
    @Transactional(readOnly = true)
    public AiPolicyPageResponse listPolicies(Boolean active, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "policyCode"));
        org.springframework.data.domain.Page<AiModerationPolicy> result = active != null
                ? policyRepository.findByActive(active, pageable)
                : policyRepository.findAll(pageable);
        return mapper.toPolicyPageResponse(result);
    }

    @Override
    @Transactional
    public AiPolicyResponse createPolicy(CreateAiPolicyRequest request, UUID actorUserId) {
        String code = request.policyCode().trim().toUpperCase(Locale.ROOT);
        if (policyRepository.existsByPolicyCodeIgnoreCase(code)) {
            throw AiModerationException.duplicatePolicyCode(code);
        }
        validateGuidance(request.detectionGuidance());
        validateThreshold(request.confidenceThreshold());
        String targetTypes = normalizeTargetTypes(request.applicableTargetTypes());

        String refLinksJson = mapper.serializeReferenceLinks(request.referenceLinks());
        String refFilesJson = mapper.serializeReferenceFiles(request.referenceFiles());

        AiModerationPolicy policy = policyRepository.save(AiModerationPolicy.builder()
                .policyCode(code)
                .name(request.name().trim())
                .detectionGuidance(request.detectionGuidance().trim())
                .violationCategory(request.violationCategory())
                .reportCategory(request.reportCategory())
                .severity(request.severity())
                .applicableTargetTypes(targetTypes)
                .confidenceThreshold(request.confidenceThreshold())
                .active(request.active() == null || request.active())
                .systemDefault(false)
                .version(1)
                .referenceLinks(refLinksJson)
                .referenceFiles(refFilesJson)
                .createdBy(actorUserId)
                .updatedBy(actorUserId)
                .build());

        auditService.log(AuditAction.AI_POLICY_CREATED, actorUserId, "AiModerationPolicy",
                policy.getId().toString(), "policyCode=" + code + " severity=" + policy.getSeverity()
                        + " category=" + policy.getViolationCategory() + " version=1");
        return mapper.toPolicyResponse(policy);
    }

    @Override
    @Transactional
    public AiPolicyResponse updatePolicy(UUID policyId, UpdateAiPolicyRequest request, UUID actorUserId) {
        AiModerationPolicy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> AiModerationException.policyNotFound(policyId.toString()));

        List<String> changedFields = new ArrayList<>();
        boolean classificationAffecting = false;

        if (request.name() != null && !request.name().isBlank()
                && !request.name().trim().equals(policy.getName())) {
            policy.setName(request.name().trim());
            changedFields.add("name");
        }
        if (request.detectionGuidance() != null && !request.detectionGuidance().isBlank()
                && !request.detectionGuidance().trim().equals(policy.getDetectionGuidance())) {
            validateGuidance(request.detectionGuidance());
            policy.setDetectionGuidance(request.detectionGuidance().trim());
            changedFields.add("detectionGuidance");
            classificationAffecting = true;
        }
        if (request.violationCategory() != null && request.violationCategory() != policy.getViolationCategory()) {
            policy.setViolationCategory(request.violationCategory());
            changedFields.add("violationCategory");
            classificationAffecting = true;
        }
        if (request.reportCategory() != null && request.reportCategory() != policy.getReportCategory()) {
            policy.setReportCategory(request.reportCategory());
            changedFields.add("reportCategory");
        }
        if (request.severity() != null && request.severity() != policy.getSeverity()) {
            policy.setSeverity(request.severity());
            changedFields.add("severity");
            classificationAffecting = true;
        }
        if (request.applicableTargetTypes() != null && !request.applicableTargetTypes().isEmpty()) {
            String normalized = normalizeTargetTypes(request.applicableTargetTypes());
            if (!normalized.equals(policy.getApplicableTargetTypes())) {
                policy.setApplicableTargetTypes(normalized);
                changedFields.add("applicableTargetTypes");
                classificationAffecting = true;
            }
        }
        if (request.confidenceThreshold() != null
                && request.confidenceThreshold().compareTo(policy.getConfidenceThreshold()) != 0) {
            validateThreshold(request.confidenceThreshold());
            policy.setConfidenceThreshold(request.confidenceThreshold());
            changedFields.add("confidenceThreshold");
            classificationAffecting = true;
        }
        if (request.active() != null && request.active() != policy.isActive()) {
            policy.setActive(request.active());
            changedFields.add("active");
            classificationAffecting = true;
        }
        if (request.referenceLinks() != null) {
            String newLinks = mapper.serializeReferenceLinks(request.referenceLinks());
            policy.setReferenceLinks(newLinks);
            changedFields.add("referenceLinks");
        }
        if (request.referenceFiles() != null) {
            String newFiles = mapper.serializeReferenceFiles(request.referenceFiles());
            policy.setReferenceFiles(newFiles);
            changedFields.add("referenceFiles");
        }

        if (!changedFields.isEmpty()) {
            if (classificationAffecting) {
                policy.setVersion(policy.getVersion() + 1);
            }
            policy.setUpdatedBy(actorUserId);
            policy = policyRepository.save(policy);
            auditService.log(AuditAction.AI_POLICY_UPDATED, actorUserId, "AiModerationPolicy",
                    policy.getId().toString(), "policyCode=" + policy.getPolicyCode()
                            + " changed=" + String.join(",", changedFields)
                            + " version=" + policy.getVersion());
        }
        return mapper.toPolicyResponse(policy);
    }

    @Override
    @Transactional
    public AiPolicyResponse updatePolicyStatus(UUID policyId, boolean active, UUID actorUserId) {
        AiModerationPolicy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> AiModerationException.policyNotFound(policyId.toString()));
        if (policy.isActive() != active) {
            policy.setActive(active);
            policy.setVersion(policy.getVersion() + 1);
            policy.setUpdatedBy(actorUserId);
            policy = policyRepository.save(policy);
            auditService.log(AuditAction.AI_POLICY_STATUS_CHANGED, actorUserId, "AiModerationPolicy",
                    policy.getId().toString(), "policyCode=" + policy.getPolicyCode()
                            + " active=" + active + " version=" + policy.getVersion());
        }
        return mapper.toPolicyResponse(policy);
    }

    @Override
    @Transactional(readOnly = true)
    public AiPolicyTestResponse testPolicies(AiPolicyTestRequest request, UUID actorUserId) {
        GeminiModerationClient.ConfigState state = geminiModerationClient.configState();
        if (state != GeminiModerationClient.ConfigState.READY) {
            throw AiModerationException.sandboxUnavailable(state.name());
        }
        AiPolicySet policySet = policySetService.activeSnapshotFor(request.targetType());
        if (policySet.isEmpty()) {
            throw AiModerationException.sandboxUnavailable("NO_ACTIVE_POLICIES");
        }

        ModerationCallResult callResult;
        AiVerdict verdict;
        try {
            callResult = geminiModerationClient.classify(
                    promptBuilder.buildSystemInstruction(policySet.policies()),
                    promptBuilder.buildUserContent(request.targetType(), request.sampleText()),
                    promptBuilder.responseSchema());
            verdict = verdictParser.parse(callResult.rawJson(), policySet.byCode(), request.sampleText());
        } catch (GeminiConfigurationException ex) {
            throw new AiModerationException("AIM-012",
                    "Sandbox classification failed: " + ex.getErrorCode(), HttpStatus.BAD_GATEWAY);
        } catch (GeminiUnavailableException ex) {
            throw new AiModerationException("AIM-012",
                    "Sandbox classification failed: Gemini unavailable", HttpStatus.BAD_GATEWAY);
        } catch (AiVerdictParseException ex) {
            throw new AiModerationException("AIM-012",
                    "Sandbox classification failed: invalid model response", HttpStatus.BAD_GATEWAY);
        }

        CaseDecision decision = decisionPolicy.decide(verdict, policySet.byCode());

        // Audit the event only — never the sample text (it may contain PII pasted by the admin).
        auditService.log(AuditAction.AI_POLICY_TEST_RUN, actorUserId, "AiModerationPolicy", null,
                "targetType=" + request.targetType() + " classification=" + verdict.classification()
                        + " matches=" + verdict.matchedPolicies().size());

        return new AiPolicyTestResponse(
                verdict.classification(),
                verdict.overallSeverity(),
                verdict.confidence(),
                verdict.recommendedAction(),
                verdict.explanation(),
                verdict.matchedPolicies().stream().map(mapper::toMatchResponse).toList(),
                decision.createCase(),
                decision.priority(),
                geminiModerationClient.model(),
                callResult.latencyMs());
    }

    private static void validateGuidance(String guidance) {
        if (guidance != null && guidance.length() > MAX_GUIDANCE_LENGTH) {
            throw AiModerationException.guidanceTooLong(MAX_GUIDANCE_LENGTH);
        }
    }

    private static void validateThreshold(BigDecimal threshold) {
        if (threshold.compareTo(BigDecimal.ZERO) < 0 || threshold.compareTo(BigDecimal.ONE) > 0) {
            throw AiModerationException.invalidConfidenceThreshold();
        }
    }

    private static String normalizeTargetTypes(List<ReportTargetType> targetTypes) {
        List<ReportTargetType> allowed = List.of(ReportTargetType.QUESTION, ReportTargetType.ANSWER,
                ReportTargetType.CONTENT);
        List<ReportTargetType> distinct = targetTypes.stream().distinct().toList();
        if (distinct.isEmpty() || !allowed.containsAll(distinct)) {
            throw AiModerationException.invalidTargetTypes(String.valueOf(targetTypes));
        }
        return distinct.stream().map(Enum::name).collect(Collectors.joining(","));
    }
}
