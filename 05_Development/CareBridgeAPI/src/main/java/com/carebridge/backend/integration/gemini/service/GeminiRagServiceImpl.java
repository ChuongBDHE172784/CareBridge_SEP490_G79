package com.carebridge.backend.integration.gemini.service;

import com.carebridge.backend.content.entity.ContentItem;
import com.carebridge.backend.integration.gemini.builder.GeminiPromptBuilder;
import com.carebridge.backend.integration.gemini.client.GeminiClient;
import com.carebridge.backend.integration.gemini.dto.RagAnswerRequest;
import com.carebridge.backend.integration.gemini.dto.RagAnswerResponse;
import com.carebridge.backend.integration.gemini.dto.RagSafetyResult;
import com.carebridge.backend.integration.gemini.dto.RagSource;
import com.carebridge.backend.integration.gemini.exception.GeminiUnavailableException;
import com.carebridge.backend.integration.gemini.filter.RagSafetyFilter;
import com.carebridge.backend.integration.gemini.retriever.RagContextRetriever;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile({"prod", "dev"})
@RequiredArgsConstructor
@Slf4j
public class GeminiRagServiceImpl implements RagService {

    static final String STANDARD_DISCLAIMER =
            "Đây là thông tin hỗ trợ AI — không phải chẩn đoán y tế. " +
            "Vui lòng tham khảo bác sĩ hoặc chuyên gia y tế.";

    static final String CONSERVATIVE_FALLBACK =
            "Tôi hiện không thể trả lời câu hỏi này. " +
            "Vui lòng tham khảo bác sĩ hoặc chuyên gia y tế của bạn.";

    private final RagContextRetriever contextRetriever;
    private final GeminiClient geminiClient;
    private final RagSafetyFilter safetyFilter;
    private final GeminiPromptBuilder promptBuilder;

    @Override
    public RagAnswerResponse generateAnswer(RagAnswerRequest request) {
        // C1: Safety check FIRST — before any retrieval or Gemini call
        RagSafetyResult safetyResult = safetyFilter.check(request.getQuery());
        if (safetyResult.isRedFlag()) {
            log.info("RagRedFlagDetected for query hash: {}", request.getQuery().hashCode());
            return buildRedFlagResponse(safetyResult.getEmergencyGuidance());
        }

        int maxChunks = request.getMaxContextChunks() > 0 ? request.getMaxContextChunks() : 5;

        List<ContentItem> contextItems = contextRetriever.retrieveContext(
                request.getQuery(), request.getTopicId(), maxChunks);

        String prompt = promptBuilder.buildSafetyConstrainedPrompt(
                request.getQuery(), contextItems, request.getUserStage());

        // C2: Catch Gemini unavailability — NEVER propagate exception to caller
        try {
            String answer = geminiClient.generate(prompt);
            List<RagSource> sources = contextItems.stream()
                    .map(item -> RagSource.builder()
                            .contentId(item.getId())
                            .title(item.getTitle())
                            .build())
                    .toList();
            return RagAnswerResponse.builder()
                    .answer(answer)
                    .disclaimer(STANDARD_DISCLAIMER)  // C4: constant, never from Gemini
                    .sources(sources)
                    .fallback(false)
                    .generatedAt(LocalDateTime.now())
                    .build();
        } catch (GeminiUnavailableException e) {
            log.warn("RagFallbackTriggered: Gemini unavailable — {}", e.getMessage());
            return buildFallbackResponse();
        }
    }

    private RagAnswerResponse buildFallbackResponse() {
        return RagAnswerResponse.builder()
                .answer(CONSERVATIVE_FALLBACK)
                .disclaimer(STANDARD_DISCLAIMER)
                .sources(List.of())
                .fallback(true)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    private RagAnswerResponse buildRedFlagResponse(String guidance) {
        return RagAnswerResponse.builder()
                .answer(guidance)
                .disclaimer(STANDARD_DISCLAIMER)
                .sources(List.of())
                .fallback(true)
                .generatedAt(LocalDateTime.now())
                .build();
    }
}
