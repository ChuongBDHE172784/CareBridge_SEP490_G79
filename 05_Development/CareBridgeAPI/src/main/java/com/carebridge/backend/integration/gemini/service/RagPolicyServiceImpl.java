package com.carebridge.backend.integration.gemini.service;

import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.policy.LifecycleContentStageResolver;
import com.carebridge.backend.integration.gemini.dto.RagAnswerRequest;
import com.carebridge.backend.integration.gemini.dto.RagAnswerResponse;
import com.carebridge.backend.integration.gemini.dto.RagAudienceContext;
import com.carebridge.backend.integration.gemini.dto.RagExecutionContext;
import com.carebridge.backend.integration.gemini.dto.RagSafetyResult;
import com.carebridge.backend.integration.gemini.dto.UserStage;
import com.carebridge.backend.integration.gemini.filter.RagSafetyFilter;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RagPolicyServiceImpl implements RagPolicyService {
    private static final String DISCLAIMER =
            "AI-assisted information; not a medical diagnosis. Consult a qualified professional.";
    private final RagSafetyFilter safetyFilter;
    private final LifecycleContentStageResolver lifecycleContentStageResolver;
    private final RagService ragService;

    @Override
    public RagAnswerResponse generateAnswer(RagAnswerRequest request, RagAudienceContext context) {
        RagSafetyResult safety = safetyFilter.check(request.getQuery());
        if (safety.isRedFlag()) {
            return RagAnswerResponse.builder().answer(safety.getEmergencyGuidance())
                    .disclaimer(DISCLAIMER).sources(List.of()).fallback(true)
                    .generatedAt(LocalDateTime.now()).build();
        }
        RagExecutionContext executionContext;
        if (context.triageStage() != null) {
            // Internal AI Triage enrichment supplies the canonical selected stage. This
            // takes precedence over account lifecycle state so the answer matches the intake.
            executionContext = new RagExecutionContext(
                    context.mother(), context.triageStage(), mapCanonicalStage(context.triageStage()));
        } else if (context.mother()) {
            ContentStage canonical = ContentStage.PREGNANCY;
            UserStage mappedStage = UserStage.PREGNANCY;
            try {
                canonical = lifecycleContentStageResolver.resolve(context.callerId());
                mappedStage = mapCanonicalStage(canonical);
            } catch (Exception ignored) {
                // Default to PREGNANCY context if no journey is configured yet
            }
            executionContext = new RagExecutionContext(true, canonical, mappedStage);
        } else {
            UserStage stage = request.getUserStage() != null ? request.getUserStage() : UserStage.PREGNANCY;
            executionContext = new RagExecutionContext(false, null, stage);
        }
        return ragService.generateAnswer(request, executionContext);
    }

    UserStage mapCanonicalStage(ContentStage stage) {
        return switch (stage) {
            case PRE_PREGNANCY -> UserStage.PRE_PREGNANCY;
            case PREGNANCY -> UserStage.PREGNANCY;
            case POSTPARTUM -> UserStage.POSTPARTUM;
            case BABY_CARE -> throw new IllegalArgumentException("BABY_CARE is outside maternal RAG");
        };
    }
}
