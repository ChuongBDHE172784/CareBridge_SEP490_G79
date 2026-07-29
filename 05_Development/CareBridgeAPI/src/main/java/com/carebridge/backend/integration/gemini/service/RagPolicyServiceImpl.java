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
        if (context.mother()) {
            ContentStage canonical = lifecycleContentStageResolver.resolve(context.callerId());
            executionContext = new RagExecutionContext(true, canonical, mapCanonicalStage(canonical));
        } else {
            executionContext = new RagExecutionContext(false, null, request.getUserStage());
        }
        return ragService.generateAnswer(request, executionContext);
    }

    UserStage mapCanonicalStage(ContentStage stage) {
        return switch (stage) {
            case PRE_PREGNANCY -> UserStage.PRE_PREGNANCY;
            case PREGNANCY -> UserStage.PREGNANCY;
            case POSTPARTUM -> UserStage.POSTPARTUM;
        };
    }
}
