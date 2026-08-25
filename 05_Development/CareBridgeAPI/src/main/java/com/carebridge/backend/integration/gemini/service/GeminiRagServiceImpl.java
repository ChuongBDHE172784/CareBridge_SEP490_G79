package com.carebridge.backend.integration.gemini.service;

import com.carebridge.backend.content.entity.ContentItem;
import com.carebridge.backend.content.entity.ContentSource;
import com.carebridge.backend.integration.gemini.builder.GeminiPromptBuilder;
import com.carebridge.backend.integration.gemini.client.GeminiClient;
import com.carebridge.backend.integration.gemini.dto.RagAnswerRequest;
import com.carebridge.backend.integration.gemini.dto.RagAnswerResponse;
import com.carebridge.backend.integration.gemini.dto.RagSource;
import com.carebridge.backend.integration.gemini.dto.RagExecutionContext;
import com.carebridge.backend.integration.gemini.exception.GeminiUnavailableException;
import com.carebridge.backend.integration.gemini.retriever.RagContextRetriever;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("!test")
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
    private final GeminiPromptBuilder promptBuilder;

    @Override
    public RagAnswerResponse generateAnswer(RagAnswerRequest request, RagExecutionContext context) {
        int maxChunks = (request.getMaxContextChunks() != null && request.getMaxContextChunks() > 0)
                ? request.getMaxContextChunks() : 5;

        List<ContentItem> contextItems;
        try {
            contextItems = context.mother() || context.canonicalStage() != null
                    ? contextRetriever.retrieveContext(
                            request.getQuery(), request.getTopicId(), context.canonicalStage(), maxChunks)
                    : contextRetriever.retrieveContext(request.getQuery(), request.getTopicId(), maxChunks);
        } catch (RuntimeException exception) {
            log.warn("RAG retrieval unavailable reason={}", exception.getClass().getSimpleName());
            return buildFallbackResponse();
        }

        String prompt = promptBuilder.buildSafetyConstrainedPrompt(
                request.getQuery(), contextItems, context.promptStage());

        // C2: Catch Gemini unavailability — NEVER propagate exception to caller
        try {
            String answer = geminiClient.generate(prompt);
            List<RagSource> sources;
            try {
                sources = contextItems.stream().map(this::toSource).toList();
            } catch (RuntimeException exception) {
                log.warn("RAG source provenance unavailable reason={}",
                        exception.getClass().getSimpleName());
                sources = List.of();
            }
            return RagAnswerResponse.builder()
                    .answer(answer)
                    .disclaimer(STANDARD_DISCLAIMER)  // C4: constant, never from Gemini
                    .sources(sources)
                    .fallback(false)
                    .generatedAt(LocalDateTime.now())
                    .build();
        } catch (GeminiUnavailableException exception) {
            log.warn("RagFallbackTriggered reason=GEMINI_UNAVAILABLE exceptionType={}",
                    exception.getClass().getSimpleName());
            return buildFallbackResponse();
        } catch (RuntimeException exception) {
            log.warn("RAG generation unavailable reason={}", exception.getClass().getSimpleName());
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

    /**
     * Keep source provenance attached to the retrieved content.  The triage enrichment
     * boundary performs the final DB-backed HTTPS/deep-link approval check before exposing
     * any of these fields to a user; this mapper deliberately never invents a URL.
     */
    private RagSource toSource(ContentItem item) {
        ContentSource source = selectSource(item);
        String excerpt = item.getSummary();
        if (excerpt == null || excerpt.isBlank()) {
            excerpt = item.getBody();
        }
        if (excerpt != null && excerpt.length() > 500) {
            excerpt = excerpt.substring(0, 500).trim() + "…";
        }
        return RagSource.builder()
                .contentId(item.getId())
                .title(item.getTitle())
                .url(source == null ? null : source.getUrl())
                .publisher(source == null ? null : source.getPublisher())
                .excerpt(excerpt)
                .sourceVersion(item.getVersionNo() == null ? null : String.valueOf(item.getVersionNo()))
                .lastReviewed(item.getUpdatedAt() == null
                        ? (item.getPublishedAt() == null ? null : item.getPublishedAt().toString())
                        : item.getUpdatedAt().toString())
                .build();
    }

    private ContentSource selectSource(ContentItem item) {
        if (item == null) return null;
        try {
            if (item.getSources() == null) return null;
            return item.getSources().stream()
                    .filter(candidate -> candidate != null && isUsableSourceUrl(candidate.getUrl()))
                    .findFirst()
                    .orElse(null);
        } catch (RuntimeException exception) {
            log.warn("RAG source collection unavailable reason={}",
                    exception.getClass().getSimpleName());
            return null;
        }
    }

    private boolean isUsableSourceUrl(String value) {
        if (value == null || value.isBlank()) return false;
        try {
            URI uri = URI.create(value);
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase();
            String path = uri.getPath() == null ? "" : uri.getPath().toLowerCase();
            return "https".equalsIgnoreCase(uri.getScheme())
                    && !host.isBlank()
                    && (uri.getPort() == -1 || uri.getPort() == 443)
                    && uri.getUserInfo() == null
                    && !Set.of("google.com", "bing.com", "yahoo.com")
                            .contains(host.replaceFirst("^www\\.", ""))
                    && !path.matches(".*/(search|query|find)(/.*)?/?");
        } catch (IllegalArgumentException exception) {
            return false;
        }
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
