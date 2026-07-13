package com.carebridge.backend.triage.service.impl;

import com.carebridge.backend.triage.IntakeStatus;
import com.carebridge.backend.triage.RiskLevel;
import com.carebridge.backend.triage.dto.request.RunIntakeRequest;
import com.carebridge.backend.triage.dto.request.StartIntakeConversationRequest;
import com.carebridge.backend.triage.dto.request.ContinueIntakeConversationRequest;
import com.carebridge.backend.triage.dto.response.IntakeConversationResponse;
import com.carebridge.backend.triage.dto.response.IntakeSessionResponse;
import com.carebridge.backend.triage.dto.response.TriageResultResponse;
import com.carebridge.backend.triage.entity.IntakeSession;
import com.carebridge.backend.triage.engine.ChildTriageResult;
import com.carebridge.backend.triage.engine.TriageGraphService;
import com.carebridge.backend.triage.event.IntakeSessionCompleted;
import com.carebridge.backend.triage.event.IntakeSessionFailed;
import com.carebridge.backend.triage.exception.TriageException;
import com.carebridge.backend.triage.repository.IIntakeSessionRepository;
import com.carebridge.backend.triage.service.ChildTriageAiClient;
import com.carebridge.backend.triage.service.ITriageService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class TriageService implements ITriageService {

    private static final Logger log = LoggerFactory.getLogger(TriageService.class);
    private static final java.util.Set<String> OFFICIAL_EVIDENCE_DOMAINS = java.util.Set.of(
            "who.int", "moh.gov.vn", "mch.moh.gov.vn", "cdc.gov", "unicef.org",
            "benhviennhitrunguong.gov.vn", "nhidong.org.vn", "bvndtp.org.vn");

    private final IIntakeSessionRepository intakeSessionRepository;
    private final ChildTriageAiClient childTriageAiClient;
    private final TriageGraphService triageGraphService;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public synchronized IntakeConversationResponse startConversation(StartIntakeConversationRequest request, UUID userId) {
        validateBoundedPayload(request.getCurrentIntake(), "currentIntake");
        String clientRequestId = normalizeClientRequestId(request.getClientRequestId());
        IntakeSession existing = clientRequestId == null ? null : intakeSessionRepository
                .findByUserIdAndClientRequestId(userId, clientRequestId).orElse(null);
        if (existing != null && existing.getRawAiResponse() != null) {
            return toConversationResponse(readJsonObject(existing.getRawAiResponse()));
        }
        IntakeSession session = existing;
        if (session == null) {
            session = IntakeSession.builder()
                    .userId(userId)
                    .clientRequestId(clientRequestId)
                    .symptoms("CONVERSATION_INTAKE")
                    .status(IntakeStatus.PROCESSING)
                    .createdAt(Instant.now())
                    .createdBy(userId)
                    .build();
            session = intakeSessionRepository.save(session);
        }
        Map<String, Object> canonicalRequest = new LinkedHashMap<>();
        canonicalRequest.put("initialText", request.getInitialText());
        canonicalRequest.put("currentIntake", request.getCurrentIntake() == null ? Map.of() : request.getCurrentIntake());
        canonicalRequest.put("intakeSessionId", session.getId().toString());
        Map<String, Object> envelope;
        try {
            envelope = readJsonObject(childTriageAiClient.startIntake(canonicalRequest));
        } catch (Exception exception) {
            log.warn("AI triage conversation start unavailable for session [{}], using Java fallback: {}",
                    session.getId(), exception.getClass().getSimpleName());
            envelope = fallbackConversation(canonicalRequest, true);
        }
        envelope = ensureSafeEnvelope(envelope, canonicalRequest, true);
        envelope.put("intakeSessionId", session.getId().toString());
        persistConversationEnvelope(session, envelope, userId);
        return toConversationResponse(envelope);
    }

    @Override
    public IntakeConversationResponse continueConversation(ContinueIntakeConversationRequest request, UUID userId) {
        validateBoundedPayload(request.getNewAnswers(), "newAnswers");
        UUID sessionId = parseSessionId(request.getIntakeSessionId());
        IntakeSession session = intakeSessionRepository.findForUpdateByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new TriageException(HttpStatus.NOT_FOUND, "TRIAGE-003", "Intake session not found"));
        if (!"CONVERSATION_INTAKE".equals(session.getSymptoms())) {
            throw new TriageException(HttpStatus.CONFLICT, "TRIAGE-009", "Session is not a conversation intake");
        }

        Map<String, Object> previous = readJsonObject(session.getRawAiResponse());
        Map<String, Object> normalizedAnswers = coerceConversationAnswers(request.getNewAnswers());
        Map<String, Object> canonical = new LinkedHashMap<>();
        canonical.put("intakeSessionId", session.getId().toString());
        canonical.put("currentIntake", previous.getOrDefault("mergedIntake", Map.of()));
        canonical.put("messages", List.of());
        canonical.put("newAnswers", normalizedAnswers);
        canonical.put("round", number(previous.get("round"), 1));

        if (session.getStatus() == IntakeStatus.COMPLETED) {
            if (answersAlreadyApplied(canonical)) {
                return toConversationResponse(previous);
            }
            throw new TriageException(HttpStatus.CONFLICT, "TRIAGE-008", "Intake session is already completed");
        }
        if (session.getStatus() == IntakeStatus.FAILED) {
            throw new TriageException(HttpStatus.CONFLICT, "TRIAGE-008", "Intake session has failed");
        }

        if (answersAlreadyApplied(canonical)) {
            return toConversationResponse(previous);
        }

        java.util.Set<String> allowedQuestionKeys = outstandingQuestionKeys(previous);
        normalizedAnswers.entrySet().removeIf(entry -> !allowedQuestionKeys.contains(entry.getKey()));
        if (normalizedAnswers.isEmpty()) {
            throw new TriageException(HttpStatus.BAD_REQUEST, "TRIAGE-010",
                    "newAnswers must answer a currently requested question");
        }

        Map<String, Object> envelope;
        try {
            envelope = readJsonObject(childTriageAiClient.continueIntake(canonical));
        } catch (Exception exception) {
            log.warn("AI triage conversation continue unavailable for session [{}], using Java fallback: {}",
                    session.getId(), exception.getClass().getSimpleName());
            envelope = fallbackConversation(canonical, false);
        }
        envelope = ensureSafeEnvelope(envelope, canonical, false);
        envelope.put("intakeSessionId", session.getId().toString());
        persistConversationEnvelope(session, envelope, userId);
        return toConversationResponse(envelope);
    }

    @Override
    public IntakeSessionResponse runIntake(RunIntakeRequest request, UUID userId) {
        IntakeSession session = IntakeSession.builder()
                .userId(userId)
                .symptoms(snapshotRequest(request))
                .babyProfileId(request.getBabyProfileId())
                .status(IntakeStatus.PROCESSING)
                .createdAt(Instant.now())
                .createdBy(userId)
                .build();
        session = intakeSessionRepository.save(session);
        UUID sessionId = session.getId();
        log.info("Processing intake for session [{}]", sessionId);

        try {
            String aiResponse = triageWithAiServiceOrFallback(request, sessionId);
            JsonNode result = objectMapper.readTree(aiResponse);
            String triageStatus = result.path("status").asText(null);
            String riskLevel = result.path("riskLevel").isMissingNode() || result.path("riskLevel").isNull()
                    ? null
                    : result.path("riskLevel").asText();
            if (triageStatus == null) {
                triageStatus = "NEED_MORE_INFO".equals(riskLevel) ? "NEED_MORE_INFO" : "COMPLETED";
            }

            session.setRawAiResponse(aiResponse);
            session.setRiskLevel(isPersistableRiskLevel(riskLevel) ? RiskLevel.valueOf(riskLevel) : null);
            session.setDisclaimer(result.path("disclaimer").asText(null));
            boolean needsMore = "NEED_MORE_INFO".equals(triageStatus);
            session.setStatus(needsMore ? IntakeStatus.NEED_MORE_INFO : IntakeStatus.COMPLETED);
            session.setCompletedAt(needsMore ? null : Instant.now());
            session = intakeSessionRepository.save(session);

            if (session.getStatus() == IntakeStatus.COMPLETED && session.getRiskLevel() != null) {
                eventPublisher.publishEvent(new IntakeSessionCompleted(
                        UUID.randomUUID(), session.getId(), userId,
                        session.getRiskLevel(), session.getCompletedAt()));
            }

            log.info("Intake completed for session [{}] status=[{}] riskLevel=[{}]",
                    sessionId, session.getStatus(), session.getRiskLevel());
        } catch (Exception e) {
            log.warn("Triage graph failed for session [{}]: {}", sessionId, e.getClass().getSimpleName());
            session.setStatus(IntakeStatus.FAILED);
            intakeSessionRepository.save(session);
            eventPublisher.publishEvent(new IntakeSessionFailed(
                    UUID.randomUUID(), session.getId(), userId,
                    "Triage processing failed", Instant.now()));
            throw new TriageException(HttpStatus.SERVICE_UNAVAILABLE, "TRIAGE-005", "Triage processing failed");
        }

        return toResponse(session);
    }

    private String triageWithAiServiceOrFallback(RunIntakeRequest request, UUID sessionId) throws JsonProcessingException {
        try {
            String response = childTriageAiClient.triageChild(request);
            String risk = objectMapper.readTree(response).path("riskLevel").asText(null);
            if (!isPersistableRiskLevel(risk) && !"NEED_MORE_INFO".equals(risk)) {
                throw new IllegalStateException("AI triage returned invalid risk contract");
            }
            return response;
        } catch (Exception e) {
            log.warn("AI triage service unavailable for session [{}], falling back to Java rule engine: {}",
                    sessionId, e.getClass().getSimpleName());
            ChildTriageResult graphResult = triageGraphService.run(request);
            Map<String, Object> result = objectMapper.convertValue(
                    graphResult, new TypeReference<Map<String, Object>>() {});
            addJavaFallbackMetadata(result);
            return objectMapper.writeValueAsString(result);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public TriageResultResponse getResult(UUID sessionId, UUID userId) {
        IntakeSession session = intakeSessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new TriageException(HttpStatus.NOT_FOUND, "TRIAGE-003",
                        "Intake session not found: " + sessionId));

        List<Map<String, Object>> rawCitations = readObjectList(session, "citations");
        List<Map<String, Object>> citations = readValidatedCitations(session);
        String evidenceWarning = readText(session, "warning");
        if (!rawCitations.isEmpty() && citations.isEmpty()) {
            evidenceWarning = "Nguồn bằng chứng không hợp lệ đã bị loại; rủi ro vẫn do bộ quy tắc quyết định.";
        }

        return TriageResultResponse.builder()
                .sessionId(session.getId())
                .triageStatus(readText(session, "status", session.getStatus().name()))
                .riskLevel(readText(session, "riskLevel"))
                .riskColor(readText(session, "riskColor"))
                .summary(readText(session, "summary"))
                .possibleConcern(readText(session, "possibleConcern"))
                .recommendedAction(readText(session, "recommendedAction"))
                .emergencyActionRequired(readBoolean(session, "emergencyActionRequired"))
                .redFlags(readStringList(session, "redFlags"))
                .matchedRules(readStringList(session, "matchedRules"))
                .citations(citations)
                .evidence(readObject(session, "evidence"))
                .disclaimer(readText(session, "disclaimer", session.getDisclaimer()))
                .questions(readStringList(session, "questions"))
                .warning(evidenceWarning)
                .normalizedSymptoms(readStringList(session, "normalizedSymptoms"))
                .normalizedSymptomDetails(readObjectList(session, "normalizedSymptomDetails"))
                .evidenceIds(readStringList(session, "evidenceIds"))
                .recommendationCode(readText(session, "recommendationCode"))
                .explainabilityMetrics(readObject(session, "explainabilityMetrics"))
                .graphVersion(readText(session, "graphVersion"))
                .ruleSetVersion(readText(session, "ruleSetVersion"))
                .ontologyVersion(readText(session, "ontologyVersion"))
                .responseSchemaVersion(readText(session, "responseSchemaVersion", "1.0"))
                .fallbackUsed(readBoolean(session, "fallbackUsed"))
                .status(session.getStatus().name())
                .createdAt(session.getCreatedAt())
                .completedAt(session.getCompletedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<IntakeSessionResponse> listSessions(UUID userId) {
        return intakeSessionRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private IntakeSessionResponse toResponse(IntakeSession session) {
        return IntakeSessionResponse.builder()
                .sessionId(session.getId())
                .status(session.getStatus().name())
                .riskLevel(session.getRiskLevel() != null ? session.getRiskLevel().name() : null)
                .disclaimer(session.getDisclaimer())
                .createdAt(session.getCreatedAt())
                .completedAt(session.getCompletedAt())
                .build();
    }

    private String snapshotRequest(RunIntakeRequest request) {
        try {
            Map<String, Object> replaySafe = new LinkedHashMap<>();
            replaySafe.put("childAgeMonths", request.getChildAgeMonths());
            replaySafe.put("temperatureC", request.getTemperatureC());
            replaySafe.put("feedingStatus", request.getFeedingStatus());
            replaySafe.put("breathingStatus", request.getBreathingStatus());
            replaySafe.put("consciousnessStatus", request.getConsciousnessStatus());
            replaySafe.put("seizure", request.getSeizure());
            replaySafe.put("dehydrationSigns", request.getDehydrationSigns());
            replaySafe.put("hasFreeText", request.getParentFreeText() != null || request.getSymptoms() != null);
            return objectMapper.writeValueAsString(replaySafe);
        } catch (JsonProcessingException e) {
            return "{\"snapshotUnavailable\":true}";
        }
    }

    private JsonNode rawResult(IntakeSession session) {
        if (session.getRawAiResponse() == null || session.getRawAiResponse().isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            JsonNode root = objectMapper.readTree(session.getRawAiResponse());
            JsonNode nested = root.path("triageResult");
            return nested.isObject() ? nested : root;
        } catch (JsonProcessingException e) {
            return objectMapper.createObjectNode();
        }
    }

    private String readText(IntakeSession session, String field) {
        return readText(session, field, null);
    }

    private String readText(IntakeSession session, String field, String fallback) {
        JsonNode node = rawResult(session).path(field);
        if (node.isMissingNode() || node.isNull()) {
            if ("riskLevel".equals(field) && session.getRiskLevel() != null) {
                return session.getRiskLevel().name();
            }
            return fallback;
        }
        return node.asText();
    }

    private boolean isPersistableRiskLevel(String riskLevel) {
        return "GREEN".equals(riskLevel) || "YELLOW".equals(riskLevel) || "RED".equals(riskLevel);
    }

    private Boolean readBoolean(IntakeSession session, String field) {
        JsonNode node = rawResult(session).path(field);
        return node.isMissingNode() || node.isNull() ? Boolean.FALSE : node.asBoolean();
    }

    private List<String> readStringList(IntakeSession session, String field) {
        JsonNode node = rawResult(session).path(field);
        if (!node.isArray()) {
            return Collections.emptyList();
        }
        List<String> values = new ArrayList<>();
        node.forEach(item -> values.add(item.asText()));
        return values;
    }

    private List<Map<String, Object>> readObjectList(IntakeSession session, String field) {
        JsonNode node = rawResult(session).path(field);
        if (!node.isArray()) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> values = new ArrayList<>();
        node.forEach(item -> {
            if (item.isObject()) {
                values.add(objectMapper.convertValue(item, new TypeReference<Map<String, Object>>() {}));
            }
        });
        return values;
    }

    private Map<String, Object> readObject(IntakeSession session, String field) {
        JsonNode node = rawResult(session).path(field);
        if (!node.isObject()) {
            return Collections.emptyMap();
        }
        return objectMapper.convertValue(node, new TypeReference<Map<String, Object>>() {});
    }

    private Map<String, Object> readJsonObject(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Invalid AI triage response contract", exception);
        }
    }

    private void persistConversationEnvelope(IntakeSession session, Map<String, Object> envelope, UUID userId) {
        sanitizeEnvelope(envelope);
        try {
            session.setRawAiResponse(objectMapper.writeValueAsString(envelope));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to persist triage conversation", exception);
        }
        String flowStatus = String.valueOf(envelope.getOrDefault("status", "ASK_MORE"));
        Object resultValue = envelope.get("triageResult");
        Map<String, Object> result = resultValue instanceof Map<?, ?> map
                ? objectMapper.convertValue(map, new TypeReference<Map<String, Object>>() {}) : Map.of();
        String risk = result.get("riskLevel") == null ? null : String.valueOf(result.get("riskLevel"));
        session.setRiskLevel(isPersistableRiskLevel(risk) ? RiskLevel.valueOf(risk) : null);
        session.setDisclaimer(result.get("disclaimer") == null ? null : String.valueOf(result.get("disclaimer")));
        boolean complete = "TRIAGE_COMPLETE".equals(flowStatus);
        session.setStatus(complete ? IntakeStatus.COMPLETED : IntakeStatus.NEED_MORE_INFO);
        session.setCompletedAt(complete ? Instant.now() : null);
        intakeSessionRepository.save(session);
        if (complete && session.getRiskLevel() != null) {
            eventPublisher.publishEvent(new IntakeSessionCompleted(
                    UUID.randomUUID(), session.getId(), userId, session.getRiskLevel(), session.getCompletedAt()));
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fallbackConversation(Map<String, Object> request, boolean start) {
        Object currentValue = request.get("currentIntake");
        Map<String, Object> current = currentValue instanceof Map<?, ?> map
                ? new LinkedHashMap<>((Map<String, Object>) map) : new LinkedHashMap<>();
        if (start && request.get("initialText") != null && current.get("parentFreeText") == null) {
            current.put("parentFreeText", request.get("initialText"));
            current.put("symptomList", List.of(String.valueOf(request.get("initialText"))));
        }
        Object answerValue = request.get("newAnswers");
        if (answerValue instanceof Map<?, ?> answers) {
            coerceConversationAnswers(objectMapper.convertValue(
                    answers, new TypeReference<Map<String, Object>>() {})).forEach(current::put);
        }
        RunIntakeRequest intake = objectMapper.convertValue(current, RunIntakeRequest.class);
        ChildTriageResult graphResult = triageGraphService.run(intake);
        Map<String, Object> result = objectMapper.convertValue(graphResult, new TypeReference<Map<String, Object>>() {});
        addJavaFallbackMetadata(result);
        String fallbackRisk = result.get("riskLevel") == null ? null : String.valueOf(result.get("riskLevel"));
        result.putIfAbsent("recommendationCode", switch (String.valueOf(fallbackRisk)) {
            case "RED" -> "SEEK_EMERGENCY_CARE";
            case "YELLOW" -> "CONTACT_HEALTHCARE_PROVIDER";
            case "GREEN" -> "MONITOR_AT_HOME";
            default -> "PROVIDE_MORE_INFORMATION";
        });
        Map<String, Object> envelope = new LinkedHashMap<>();
        boolean needMore = "NEED_MORE_INFO".equals(result.get("status"));
        boolean questionLimitReached = number(request.get("round"), 1) >= 3;
        if (needMore && questionLimitReached) {
            needMore = false;
            result.put("status", "COMPLETED");
            result.put("riskLevel", "YELLOW");
            result.put("riskColor", "#FACC15");
            result.put("emergencyActionRequired", false);
            result.put("matchedRules", List.of("YELLOW_INCOMPLETE_INFORMATION"));
            result.put("recommendationCode", "CONTACT_HEALTHCARE_PROVIDER");
            result.put("warning", "Thông tin chưa đầy đủ, kết quả được phân loại thận trọng.");
        }
        sanitizeMergedIntake(current, result);
        envelope.put("status", needMore ? "ASK_MORE" : "TRIAGE_COMPLETE");
        envelope.put("intakeSessionId", request.get("intakeSessionId"));
        envelope.put("mergedIntake", current);
        envelope.put("assistantMessage", needMore
                ? "CareBridge cần thêm một vài thông tin để phân loại rủi ro an toàn hơn."
                : "CareBridge đã hoàn tất phân loại rủi ro bằng chế độ dự phòng an toàn.");
        envelope.put("questions", needMore ? fallbackQuestions(intake) : List.of());
        envelope.put("round", Math.min(3, number(request.get("round"), 1) + (needMore ? 1 : 0)));
        envelope.put("triageResult", needMore ? null : result);
        return envelope;
    }

    private List<Map<String, Object>> fallbackQuestions(RunIntakeRequest intake) {
        List<Map<String, Object>> questions = new ArrayList<>();
        if (intake.getChildAgeMonths() == null) {
            questions.add(fallbackQuestion(
                    "childAgeMonths",
                    "Bé hiện bao nhiêu tháng tuổi?",
                    "NUMBER",
                    List.of()));
        }
        if (intake.getBreathingStatus() == null || intake.getBreathingStatus().isBlank()) {
            questions.add(fallbackQuestion(
                    "breathingStatus",
                    "Hiện tại bé có khó thở, rút lõm lồng ngực hoặc tím môi không?",
                    "SINGLE_CHOICE",
                    List.of("Không", "Khó thở", "Thở rút lõm ngực", "Tím tái", "Không chắc")));
        }
        if (intake.getConsciousnessStatus() == null || intake.getConsciousnessStatus().isBlank()) {
            questions.add(fallbackQuestion(
                    "consciousnessStatus",
                    "Bé có tỉnh táo bình thường hay lơ mơ, li bì hoặc khó đánh thức?",
                    "SINGLE_CHOICE",
                    List.of("Tỉnh táo", "Lơ mơ", "Li bì", "Khó đánh thức", "Không chắc")));
        }
        if (intake.getFeedingStatus() == null || intake.getFeedingStatus().isBlank()) {
            questions.add(fallbackQuestion(
                    "feedingStatus",
                    "Bé có bú hoặc uống được như bình thường không?",
                    "SINGLE_CHOICE",
                    List.of("Bú/uống tốt", "Bú/uống kém", "Bỏ bú", "Không uống được", "Không chắc")));
        }
        if (questions.isEmpty()) {
            questions.add(fallbackQuestion(
                    "parentFreeText",
                    "Bạn hãy mô tả cụ thể hơn dấu hiệu đang quan sát được ở bé và triệu chứng đã kéo dài bao lâu.",
                    "TEXT",
                    List.of()));
        }
        return questions.stream().limit(3).toList();
    }

    private Map<String, Object> fallbackQuestion(
            String questionKey, String text, String answerType, List<String> options) {
        Map<String, Object> question = new LinkedHashMap<>();
        question.put("questionKey", questionKey);
        question.put("text", text);
        question.put("answerType", answerType);
        question.put("options", options);
        return question;
    }

    private void addJavaFallbackMetadata(Map<String, Object> result) {
        String fallbackRisk = result.get("riskLevel") == null ? null : String.valueOf(result.get("riskLevel"));
        result.putIfAbsent("graphVersion", "java-fallback-1.0");
        result.putIfAbsent("ruleSetVersion", "java-pediatric-risk-rules-1.0");
        result.putIfAbsent("ontologyVersion", "java-child-symptoms-1.0");
        result.putIfAbsent("responseSchemaVersion", "2.0");
        result.put("fallbackUsed", true);
        result.putIfAbsent("normalizedSymptoms", List.of());
        result.putIfAbsent("evidenceIds", List.of());
        result.putIfAbsent("recommendationCode", switch (String.valueOf(fallbackRisk)) {
            case "RED" -> "SEEK_EMERGENCY_CARE";
            case "YELLOW" -> "CONTACT_HEALTHCARE_PROVIDER";
            case "GREEN" -> "MONITOR_AT_HOME";
            default -> "PROVIDE_MORE_INFORMATION";
        });
    }

    @SuppressWarnings("unchecked")
    private boolean answersAlreadyApplied(Map<String, Object> request) {
        Object answersValue = request.get("newAnswers");
        Object intakeValue = request.get("currentIntake");
        if (!(answersValue instanceof Map<?, ?> answers) || answers.isEmpty() || !(intakeValue instanceof Map<?, ?> intake)) {
            return false;
        }
        return answers.entrySet().stream().allMatch(entry ->
                java.util.Objects.equals(intake.get(entry.getKey()), entry.getValue()));
    }

    private UUID parseSessionId(Object value) {
        try {
            return UUID.fromString(String.valueOf(value));
        } catch (Exception exception) {
            throw new TriageException(HttpStatus.BAD_REQUEST, "TRIAGE-007", "Invalid intakeSessionId");
        }
    }

    private Map<String, Object> ensureSafeEnvelope(
            Map<String, Object> envelope, Map<String, Object> canonicalRequest, boolean start) {
        String status = String.valueOf(envelope.get("status"));
        if ("ASK_MORE".equals(status)) {
            boolean valid = envelope.get("mergedIntake") instanceof Map<?, ?>
                    && envelope.get("questions") instanceof List<?> questions && !questions.isEmpty()
                    && questions.stream().allMatch(this::isRenderableQuestion)
                    && envelope.get("round") instanceof Number round && round.intValue() >= 1 && round.intValue() <= 3;
            if (valid) {
                sanitizeEnvelope(envelope);
                return envelope;
            }
            log.warn("AI conversation returned invalid ASK_MORE envelope; using Java fallback");
            return fallbackConversation(canonicalRequest, start);
        }
        if (!"TRIAGE_COMPLETE".equals(status)) {
            log.warn("AI conversation returned unknown envelope status; using Java fallback");
            return fallbackConversation(canonicalRequest, start);
        }
        Object resultValue = envelope.get("triageResult");
        Map<String, Object> result = resultValue instanceof Map<?, ?> map
                ? objectMapper.convertValue(map, new TypeReference<Map<String, Object>>() {}) : null;
        String risk = result == null || result.get("riskLevel") == null
                ? null : String.valueOf(result.get("riskLevel"));
        boolean redInvariant = !"RED".equals(risk)
                || (Boolean.TRUE.equals(result.get("emergencyActionRequired"))
                && "SEEK_EMERGENCY_CARE".equals(result.get("recommendationCode"))
                && result.get("matchedRules") instanceof List<?> rules && !rules.isEmpty());
        if (isPersistableRiskLevel(risk) && redInvariant) {
            sanitizeEnvelope(envelope);
            return envelope;
        }
        log.warn("AI conversation returned unsafe completed envelope; using Java fallback");
        return fallbackConversation(canonicalRequest, start);
    }

    private boolean isRenderableQuestion(Object value) {
        if (!(value instanceof Map<?, ?> question)) return false;
        String questionKey = nonBlank(question.get("questionKey"));
        String text = nonBlank(question.get("text"));
        String answerType = nonBlank(question.get("answerType"));
        if (questionKey == null || text == null || answerType == null || !(question.get("options") instanceof List<?>)) {
            return false;
        }
        if (!java.util.Set.of("TEXT", "NUMBER", "SINGLE_CHOICE", "MULTI_CHOICE", "BOOLEAN")
                .contains(answerType)) {
            return false;
        }
        return !java.util.Set.of("SINGLE_CHOICE", "MULTI_CHOICE", "BOOLEAN").contains(answerType)
                || !((List<?>) question.get("options")).isEmpty();
    }

    private java.util.Set<String> outstandingQuestionKeys(Map<String, Object> envelope) {
        Object questionsValue = envelope.get("questions");
        if (!(questionsValue instanceof List<?> questions)) return java.util.Set.of();
        return questions.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(question -> nonBlank(question.get("questionKey")))
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private List<Map<String, Object>> readValidatedCitations(IntakeSession session) {
        List<Map<String, Object>> raw = readObjectList(session, "citations");
        List<Map<String, Object>> valid = new ArrayList<>();
        boolean legacy = "1.0".equals(readText(session, "responseSchemaVersion", "1.0"));
        for (Map<String, Object> citation : raw) {
            String title = nonBlank(citation.get("title"));
            String source = nonBlank(citation.get("organization"));
            if (source == null) source = nonBlank(citation.get("source"));
            String url = nonBlank(citation.get("url"));
            String excerpt = nonBlank(citation.get("excerpt"));
            if (title == null || source == null || url == null || excerpt == null || !isOfficialHttpsUrl(url)) {
                continue;
            }
            Map<String, Object> safe = new LinkedHashMap<>(citation);
            String domain = URI.create(url).getHost().toLowerCase().replaceFirst("^www\\.", "");
            if (legacy) {
                safe.putIfAbsent("sourceId", safe.getOrDefault("id", "LEGACY_" + Integer.toUnsignedString(url.hashCode())));
                safe.putIfAbsent("domain", domain);
                safe.putIfAbsent("sourceVersion", "legacy-unknown");
                safe.putIfAbsent("lastReviewed", "legacy-unknown");
                safe.putIfAbsent("section", "legacy evidence");
                safe.putIfAbsent("heading", safe.get("section"));
                safe.putIfAbsent("matchedSymptoms", List.of());
                safe.putIfAbsent("matchedRules", List.of());
                safe.putIfAbsent("sourceStatus", "APPROVED");
                safe.putIfAbsent("retrievedAt", session.getCreatedAt().toString());
                safe.putIfAbsent("retrievalMode", "LOCAL");
            } else if (!hasCompleteCitationContract(safe, domain)) {
                continue;
            }
            valid.add(safe);
        }
        return valid;
    }

    private boolean hasCompleteCitationContract(Map<String, Object> citation, String expectedDomain) {
        return nonBlank(citation.get("sourceId")) != null
                && nonBlank(citation.get("domain")) != null
                && expectedDomain.equals(String.valueOf(citation.get("domain")).replaceFirst("^www\\.", ""))
                && nonBlank(citation.get("sourceVersion")) != null
                && nonBlank(citation.get("lastReviewed")) != null
                && (nonBlank(citation.get("section")) != null || nonBlank(citation.get("heading")) != null)
                && citation.get("matchedSymptoms") instanceof List<?>
                && citation.get("matchedRules") instanceof List<?>
                && java.util.Set.of("APPROVED", "PENDING_REVIEW").contains(nonBlank(citation.get("sourceStatus")))
                && nonBlank(citation.get("retrievedAt")) != null
                && java.util.Set.of("LOCAL", "REALTIME").contains(nonBlank(citation.get("retrievalMode")));
    }

    private boolean isOfficialHttpsUrl(String value) {
        try {
            URI uri = URI.create(value);
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase();
            return "https".equalsIgnoreCase(uri.getScheme())
                    && uri.getPath() != null && !uri.getPath().replace("/", "").isBlank()
                    && OFFICIAL_EVIDENCE_DOMAINS.stream().anyMatch(
                    domain -> host.equals(domain) || host.endsWith("." + domain));
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private String nonBlank(Object value) {
        return value == null || String.valueOf(value).isBlank() ? null : String.valueOf(value);
    }

    private String normalizeClientRequestId(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void validateBoundedPayload(Object value, String field) {
        try {
            byte[] json = objectMapper.writeValueAsBytes(value == null ? Map.of() : value);
            if (json.length > 16_384 || !isBoundedValue(value, 0)) {
                throw new TriageException(HttpStatus.BAD_REQUEST, "TRIAGE-010", field + " is too large or deeply nested");
            }
        } catch (JsonProcessingException exception) {
            throw new TriageException(HttpStatus.BAD_REQUEST, "TRIAGE-010", field + " is invalid");
        }
    }

    private boolean isBoundedValue(Object value, int depth) {
        if (value == null || value instanceof Number || value instanceof Boolean) return true;
        if (depth > 4) return false;
        if (value instanceof String text) return text.length() <= 2_000;
        if (value instanceof Map<?, ?> map) {
            return map.size() <= 50 && map.entrySet().stream().allMatch(entry ->
                    String.valueOf(entry.getKey()).length() <= 80 && isBoundedValue(entry.getValue(), depth + 1));
        }
        if (value instanceof List<?> list) {
            return list.size() <= 50 && list.stream().allMatch(item -> isBoundedValue(item, depth + 1));
        }
        return false;
    }

    private Map<String, Object> coerceConversationAnswers(Map<String, Object> answers) {
        if (answers == null || answers.isEmpty()) return Map.of();
        Map<String, Object> normalized = new LinkedHashMap<>();
        answers.forEach((key, value) -> {
            Object safe = value;
            if ("seizure".equals(key) && value != null && !(value instanceof Boolean)) {
                String text = normalizeAnswerToken(value);
                safe = java.util.Set.of("co", "yes", "true", "1").contains(text) ? Boolean.TRUE
                        : java.util.Set.of("khong", "no", "false", "0").contains(text) ? Boolean.FALSE : null;
            } else if ("childAgeMonths".equals(key) && value != null) {
                try {
                    double parsed = Double.parseDouble(String.valueOf(value));
                    safe = Double.isFinite(parsed) && parsed >= 0 && parsed <= 216 && parsed == Math.rint(parsed)
                            ? (int) parsed : null;
                } catch (NumberFormatException ignored) { safe = null; }
            } else if ("temperatureC".equals(key) && value != null) {
                try {
                    double parsed = Double.parseDouble(String.valueOf(value).replace(',', '.'));
                    safe = Double.isFinite(parsed) && parsed >= 30 && parsed <= 45 ? parsed : null;
                } catch (NumberFormatException ignored) { safe = null; }
            } else if ("dehydrationSigns".equals(key) && value instanceof String text) {
                safe = "khong".equals(normalizeAnswerToken(text)) ? List.of() : List.of(text);
            }
            normalized.put(key, safe);
        });
        return normalized;
    }

    private String normalizeAnswerToken(Object value) {
        String normalized = java.text.Normalizer.normalize(
                String.valueOf(value).trim().toLowerCase(java.util.Locale.ROOT),
                java.text.Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}+", "").replace('đ', 'd');
    }

    @SuppressWarnings("unchecked")
    private void sanitizeEnvelope(Map<String, Object> envelope) {
        Object intakeValue = envelope.get("mergedIntake");
        Object resultValue = envelope.get("triageResult");
        Map<String, Object> result = resultValue instanceof Map<?, ?> map
                ? objectMapper.convertValue(map, new TypeReference<Map<String, Object>>() {}) : Map.of();
        if (intakeValue instanceof Map<?, ?> map) {
            Map<String, Object> intake = new LinkedHashMap<>((Map<String, Object>) map);
            sanitizeMergedIntake(intake, result);
            envelope.put("mergedIntake", intake);
        }
    }

    private void sanitizeMergedIntake(Map<String, Object> intake, Map<String, Object> result) {
        intake.put("parentFreeText", null);
        Object normalized = result.get("normalizedSymptoms");
        if (normalized instanceof List<?>) {
            intake.put("symptomList", normalized);
        } else {
            Object symptomList = intake.get("symptomList");
            if (symptomList instanceof List<?> list) {
                intake.put("symptomList", list.stream()
                        .map(String::valueOf)
                        .filter(item -> item.matches("[a-z_]{2,40}"))
                        .limit(24)
                        .toList());
            } else {
                intake.put("symptomList", List.of());
            }
        }
    }

    private int number(Object value, int fallback) {
        return value instanceof Number number ? number.intValue() : fallback;
    }

    private IntakeConversationResponse toConversationResponse(Map<String, Object> envelope) {
        return objectMapper.convertValue(envelope, IntakeConversationResponse.class);
    }
}
