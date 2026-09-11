package com.carebridge.backend.integration.gemini.service;

import com.carebridge.backend.integration.gemini.dto.RagAnswerRequest;
import com.carebridge.backend.integration.gemini.dto.RagAnswerResponse;
import com.carebridge.backend.integration.gemini.dto.RagExecutionContext;
import com.carebridge.backend.integration.gemini.dto.RagSource;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Answers maternal questions through the CareBridge AI RAG service.
 *
 * <p>The assistant screen needs three things the in-process Gemini path cannot
 * produce: citations drawn from the approved corpus stored in pgvector, a flag
 * when the question carries danger signs that belong in front of a clinician,
 * and follow-up prompts. All three already exist on the Python service, which
 * owns the knowledge base; this class carries them across the hop.
 *
 * <p>The mobile client used to call that service directly, cycling through
 * host:8001, 10.0.2.2 and 127.0.0.1. None of those is reachable from a phone,
 * because 8001 is exposed only inside the Docker network, so every question paid
 * three connection timeouts before falling back. Routing through the backend
 * keeps the port closed and removes that wait.
 *
 * <p>{@link GeminiRagServiceImpl} stays as the fallback: when the RAG service is
 * unreachable the mother still gets an answer, just without citations.
 */
@Service
@Primary
@Profile("!test")
@Slf4j
public class MaternalRagServiceImpl implements RagService {

    private static final int MAX_HISTORY_TURNS = 10;
    private static final int MAX_HISTORY_TURN_CHARS = 2_000;

    private final GeminiRagServiceImpl fallbackRagService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String endpoint;
    private final String internalKey;
    private final Duration timeout;
    private final boolean enabled;

    public MaternalRagServiceImpl(
            GeminiRagServiceImpl fallbackRagService,
            ObjectMapper objectMapper,
            @Value("${ai.triage-service.url}") String baseUrl,
            @Value("${ai.triage-service.internal-api-key:}") String internalKey,
            @Value("${ai.maternal-rag.request-timeout-ms:45000}") long timeoutMs,
            @Value("${ai.maternal-rag.enabled:true}") boolean enabled) {
        this.fallbackRagService = fallbackRagService;
        this.objectMapper = objectMapper;
        this.endpoint = trimTrailingSlash(baseUrl) + "/api/v1/chat/message";
        this.internalKey = internalKey;
        this.timeout = Duration.ofMillis(Math.max(1_000L, timeoutMs));
        this.enabled = enabled;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    @Override
    public RagAnswerResponse generateAnswer(RagAnswerRequest request, RagExecutionContext context) {
        // The switch lives here rather than on the bean: turning this class off at
        // the container level would leave two unranked RagService candidates.
        if (!enabled) {
            return fallbackRagService.generateAnswer(request, context);
        }
        try {
            HttpResponse<String> httpResponse = httpClient.send(
                    buildRequest(request, context),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (httpResponse.statusCode() != 200) {
                log.warn("MaternalRagFallback reason=HTTP_{}", httpResponse.statusCode());
                return fallbackRagService.generateAnswer(request, context);
            }

            JsonNode body = objectMapper.readTree(httpResponse.body());
            String answer = body.path("answer").asText("");
            if (answer.isBlank()) {
                log.warn("MaternalRagFallback reason=EMPTY_ANSWER");
                return fallbackRagService.generateAnswer(request, context);
            }

            return RagAnswerResponse.builder()
                    .answer(answer)
                    // Disclaimer stays server-owned rather than echoed back from the
                    // model, the same constraint the Gemini path already applies.
                    .disclaimer(GeminiRagServiceImpl.STANDARD_DISCLAIMER)
                    .sources(toSources(body.path("sources")))
                    .needExpertConsultation(body.path("need_expert_consultation").asBoolean(false))
                    .hasCriticalWarning(body.path("has_critical_warning").asBoolean(false))
                    .suggestedFollowups(toStringList(body.path("suggested_followups")))
                    .fallback(false)
                    .generatedAt(LocalDateTime.now())
                    .build();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            log.warn("MaternalRagFallback reason=INTERRUPTED");
            return fallbackRagService.generateAnswer(request, context);
        } catch (Exception exception) {
            // The RagService contract forbids propagating: the assistant degrades to
            // the in-process path rather than showing the mother an error.
            log.warn("MaternalRagFallback reason={}", exception.getClass().getSimpleName());
            return fallbackRagService.generateAnswer(request, context);
        }
    }

    private HttpRequest buildRequest(RagAnswerRequest request, RagExecutionContext context)
            throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("message", request.getQuery());
        payload.put("stage", toMaternalStage(context));
        payload.put("user_role", context.mother() ? "MOTHER" : "FAMILY");
        payload.put("conversation_history", toHistory(request.getConversationHistory()));
        // The remaining context is personal to the mother; a family member asking on
        // her behalf does not get her chart.
        if (context.mother()) {
            if (request.getGestationalAgeWeeks() != null) {
                payload.put("gestational_age_weeks", request.getGestationalAgeWeeks());
            }
            if (request.getSurveyProfile() != null && !request.getSurveyProfile().isEmpty()) {
                payload.put("survey_profile", request.getSurveyProfile());
            }
            if (request.getRecentMetrics() != null && !request.getRecentMetrics().isEmpty()) {
                payload.put("recent_metrics", request.getRecentMetrics());
            }
        }

        return HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .header("X-CareBridge-Internal-Key", internalKey)
                .POST(HttpRequest.BodyPublishers.ofString(
                        objectMapper.writeValueAsString(payload), StandardCharsets.UTF_8))
                .build();
    }

    /**
     * Keeps only the tail of the conversation. History arrives from the client, so
     * it is bounded here rather than trusted: an unbounded transcript would blow up
     * the prompt and the per-question cost.
     */
    private static List<Map<String, String>> toHistory(
            List<RagAnswerRequest.ConversationTurn> history) {
        List<Map<String, String>> turns = new ArrayList<>();
        if (history == null || history.isEmpty()) {
            return turns;
        }
        int from = Math.max(0, history.size() - MAX_HISTORY_TURNS);
        for (RagAnswerRequest.ConversationTurn turn : history.subList(from, history.size())) {
            if (turn == null || turn.getContent() == null || turn.getContent().isBlank()) {
                continue;
            }
            String content = turn.getContent();
            if (content.length() > MAX_HISTORY_TURN_CHARS) {
                content = content.substring(0, MAX_HISTORY_TURN_CHARS);
            }
            turns.add(Map.of(
                    "role", "assistant".equalsIgnoreCase(turn.getRole()) ? "assistant" : "user",
                    "content", content));
        }
        return turns;
    }

    /** The RAG corpus labels preconception material PRECONCEPTION, not PRE_PREGNANCY. */
    private static String toMaternalStage(RagExecutionContext context) {
        if (context.promptStage() == null) {
            return "ALL";
        }
        return switch (context.promptStage()) {
            case PRE_PREGNANCY -> "PRECONCEPTION";
            case PREGNANCY -> "PREGNANCY";
            case POSTPARTUM -> "POSTPARTUM";
        };
    }

    /**
     * Citations from the RAG corpus are chunks of ingested documents, not
     * ContentItem rows, so contentId and url stay null and are dropped by the
     * NON_NULL include on RagSource.
     */
    private static List<RagSource> toSources(JsonNode node) {
        List<RagSource> sources = new ArrayList<>();
        if (!node.isArray()) {
            return sources;
        }
        for (JsonNode entry : node) {
            String title = entry.path("title").asText("");
            if (title.isBlank()) {
                continue;
            }
            sources.add(RagSource.builder()
                    .title(title)
                    .publisher(blankToNull(entry.path("source").asText("")))
                    .excerpt(blankToNull(entry.path("snippet").asText("")))
                    .build());
        }
        return sources;
    }

    private static List<String> toStringList(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (!node.isArray()) {
            return values;
        }
        for (JsonNode entry : node) {
            String value = entry.asText("");
            if (!value.isBlank()) {
                values.add(value);
            }
        }
        return values;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
