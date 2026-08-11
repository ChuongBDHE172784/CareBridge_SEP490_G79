package com.carebridge.backend.triage.service.impl;

import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.triage.IntakeStatus;
import com.carebridge.backend.triage.RiskLevel;
import com.carebridge.backend.triage.dto.request.TriageV2AnswerSelection;
import com.carebridge.backend.triage.dto.request.TriageV2ContinueRequest;
import com.carebridge.backend.triage.dto.request.TriageV2StartRequest;
import com.carebridge.backend.triage.dto.response.TriageV2SessionResponse;
import com.carebridge.backend.triage.entity.IntakeSession;
import com.carebridge.backend.triage.exception.TriageException;
import com.carebridge.backend.triage.policy.TriageDisclaimerPolicy;
import com.carebridge.backend.triage.repository.IIntakeSessionRepository;
import com.carebridge.backend.triage.repository.IntakeSessionWriter;
import com.carebridge.backend.triage.rules.CanonicalAnswerMapper;
import com.carebridge.backend.triage.rules.CareStage;
import com.carebridge.backend.triage.rules.IndependentGlobalSafetyFallback;
import com.carebridge.backend.triage.rules.QuestionCatalog;
import com.carebridge.backend.triage.rules.TargetEntity;
import com.carebridge.backend.triage.rules.TriageRuleRegistry;
import com.carebridge.backend.triage.rules.TriageV2ReadinessService;
import com.carebridge.backend.triage.service.ITriageConsentService;
import com.carebridge.backend.triage.service.ITriageV2SessionService;
import com.carebridge.backend.triage.service.TriageV2WorkflowClient;
import com.carebridge.backend.triage.service.TriageV2Metrics;
import com.carebridge.backend.triage.service.EvidenceSourceService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/** Java authority boundary for internal/shadow Triage V2 sessions. */
@Service
@Transactional
public class TriageV2SessionService implements ITriageV2SessionService {
    private static final String SCHEMA_VERSION = "triage-v2-1";
    private static final String REDACTED = "[REDACTED_HEALTH_TEXT]";
    private static final Set<String> INDEPENDENT_SIGNAL_CODES = Set.of(
            "ALTERED_CONSCIOUSNESS", "SEIZURE", "SEVERE_BREATHING_DIFFICULTY", "CYANOSIS",
            "SELF_HARM_IDEATION", "SELF_HARM_INTENT_OR_PLAN", "HARM_TO_BABY_IDEATION",
            "CANNOT_ENSURE_OWN_SAFETY");
    private static final Set<String> PRESENCE = Set.of(
            "PRESENT", "ABSENT", "UNKNOWN", "CONFLICTED", "UNAWARE_OR_UNMEASURABLE");
    private static final Set<String> TEMPORAL = Set.of("CURRENT", "HISTORICAL");
    /** Kept in lockstep with {@code _PROVENANCE} in the Python transport boundary. */
    private static final Set<String> PROVENANCE = Set.of(
            "USER_REPORTED", "QUESTION_ANSWER", "MEASURED", "LLM_EXTRACTED_VALIDATED",
            "PROFILE_CONTEXT", "HEALTH_MEMORY_CONTEXT", "USER_REPORTED_TEXT");
    private static final Set<String> CONFLICT_STATUS = Set.of("NONE", "CONFLICTED");
    private static final Set<String> OBSERVATION_FIELDS = Set.of(
            "presence", "temporalStatus", "currentVsHistorical", "explicitNegation", "current",
            "historicalPresence", "provenance", "sourceQuestionId", "sourceOptionCode",
            "mappingRuleVersion", "conflictStatus");
    private static final java.util.regex.Pattern SAFE_REFERENCE =
            java.util.regex.Pattern.compile("^[A-Za-z0-9_.-]{1,64}$");
    private static final Set<String> UNITS = Set.of(
            "C", "F", "MMHG", "BPM", "PERCENT", "WEEKS", "DAYS", "MONTHS", "KG", "CM");
    private static final Set<String> CARE_STAGES = Set.of(
            "PRECONCEPTION", "POSSIBLE_PREGNANCY", "PREGNANCY", "POSTPARTUM_MOTHER",
            "INFANT_0_12M", "TODDLER_12_24M", "UNKNOWN", "CONFLICTED");
    private static final Set<String> POSSIBLE_PREGNANCY = Set.of(
            "YES", "NO", "UNKNOWN", "CONFLICTED");
    private static final Set<String> OUTCOMES = Set.of(
            "RED", "YELLOW", "NEEDS_MORE_INFO", "OUT_OF_SCOPE");
    private static final Set<String> TARGETS = Set.of("MOTHER", "BABY", "UNKNOWN", "CONFLICTED");
    private static final Set<String> INTENTS = Set.of(
            "SYMPTOM_TRIAGE", "GENERAL_HEALTH_INFORMATION", "SOURCE_LOOKUP", "FOLLOW_UP_ANSWER",
            "EMERGENCY_HELP", "OUT_OF_SCOPE_REQUEST", "UNKNOWN", "CONFLICTED");
    private static final Set<String> WORKFLOW_STATE_FIELDS = Set.of(
            "sessionId", "stateVersion", "expectedStateVersion", "requestId", "messageId",
            "processedRequestIds", "processedMessageIds", "rawMessages", "latestUserMessage",
            "targetEntity", "targetEntitySource", "intent", "intentSource",
            "confirmedConversationIntent", "stage",
            "contextResolutionStatus", "contextConflicts", "activeProfileId", "possiblePregnancy",
            "gestationalWeek", "postpartumDay", "babyAgeMonths", "signals", "measurements",
            "dataConflicts", "answeredQuestionIds", "askedQuestionIds",
            "unknownFields", "safetyScreenStatus",
            "contextDatasetStatus", "greenEligibilityDatasetStatus", "scopeStatus",
            "subjectScope", "complaintScope", "outcomeAppliesTo", "coverageStatus",
            "coverageReasonCodes", "supportedSymptomCodes", "unsupportedSymptomCodes",
            "coverageLimitations", "blocksClinicalQuestionPlanner", "blocksGreen",
            "selectedCatalogType", "rejectedQuestionIds",
            "pendingRiskStatuses", "primaryPendingRiskStatus", "completionReason",
            "missingRequiredFields", "candidateQuestionIds", "plannedQuestionIds", "questionRound",
            "maximumQuestionRounds", "decisiveRuleIds", "allMatchedRules", "triageOutcome",
            "requiredAction", "reasonCodes", "stopConversation", "rulesetVersion", "rulesetHash",
            "processingErrors", "finalResponse", "citations", "readingLinks");
    private static final Pattern SHA256 = Pattern.compile("^[0-9a-f]{64}$");

    private final IIntakeSessionRepository repository;
    private final IntakeSessionWriter writer;
    private final TriageV2WorkflowClient workflowClient;
    private final TriageV2ReadinessService readinessService;
    private final IndependentGlobalSafetyFallback fallback;
    private final ITriageConsentService consentService;
    private final TriageDisclaimerPolicy disclaimerPolicy;
    private final QuestionCatalog questionCatalog;
    private final CanonicalAnswerMapper answerMapper;
    private final EvidenceSourceService evidenceSourceService;
    private final BabyProfileRepository babyProfileRepository;
    private final TriageV2Metrics metrics;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final int retentionDays;

    public TriageV2SessionService(
            IIntakeSessionRepository repository,
            IntakeSessionWriter writer,
            TriageV2WorkflowClient workflowClient,
            TriageV2ReadinessService readinessService,
            IndependentGlobalSafetyFallback fallback,
            ITriageConsentService consentService,
            TriageDisclaimerPolicy disclaimerPolicy,
            QuestionCatalog questionCatalog,
            CanonicalAnswerMapper answerMapper,
            EvidenceSourceService evidenceSourceService,
            BabyProfileRepository babyProfileRepository,
            TriageV2Metrics metrics,
            ObjectMapper objectMapper,
            @Value("${carebridge.triage.v2.internal-enabled:false}") boolean enabled,
            @Value("${carebridge.triage.v2.retention-days:30}") int retentionDays) {
        this.repository = repository;
        this.writer = writer;
        this.workflowClient = workflowClient;
        this.readinessService = readinessService;
        this.fallback = fallback;
        this.consentService = consentService;
        this.disclaimerPolicy = disclaimerPolicy;
        this.questionCatalog = questionCatalog;
        this.answerMapper = answerMapper;
        this.evidenceSourceService = evidenceSourceService;
        this.babyProfileRepository = babyProfileRepository;
        this.metrics = metrics;
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.retentionDays = Math.max(1, Math.min(retentionDays, 90));
    }

    @Override
    public synchronized TriageV2SessionResponse start(TriageV2StartRequest request, UUID userId) {
        requireEnabled();
        rejectCallerAuthoredClinicalState(request.signals(), request.measurements(), request.journeyContext());
        validateSelectedProfile(request.profileId(), request.selectedTarget(), userId);
        consentService.ensureActiveConsent(userId);
        Map<String, Object> journeyContext = structuredJourneyContext(request.journeyContext());
        structuredSignals(request.signals());
        structuredMeasurements(request.measurements());
        Object suppliedVersion = request.consentContext().get("disclaimerVersion");
        if (suppliedVersion != null && !disclaimerPolicy.currentVersion().equals(String.valueOf(suppliedVersion))) {
            throw conflict("TRIAGE_V2_CONSENT_CONTEXT_MISMATCH");
        }
        IntakeSession existing = repository.findByUserIdAndClientRequestId(userId, request.requestId())
                .orElse(null);
        if (existing != null) return requireV2Replay(existing);

        IntakeSession candidate = IntakeSession.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .babyProfileId("BABY".equals(request.selectedTarget()) ? request.profileId() : null)
                .motherProfileId("MOTHER".equals(request.selectedTarget()) ? request.profileId() : null)
                .stage(null)
                .clientRequestId(request.requestId())
                .symptoms("TRIAGE_V2_REDACTED")
                .status(IntakeStatus.PROCESSING)
                .disclaimer(disclaimerPolicy.disclaimerText())
                .disclaimerVersion(disclaimerPolicy.currentVersion())
                .createdAt(Instant.now())
                .createdBy(userId)
                .schemaVersion(SCHEMA_VERSION)
                .build();
        writer.insertConversationIfAbsent(candidate);
        IntakeSession session = repository.findByUserIdAndClientRequestId(userId, request.requestId())
                .orElseThrow(() -> new IllegalStateException("Triage V2 idempotency winner unavailable"));
        if (!session.getId().equals(candidate.getId())) return requireV2Replay(session);

        return executeAndPersist(session, 0, request.requestId(), request.messageId(), request.message(),
                request.profileId(), request.selectedTarget(), journeyContext, null,
                request.signals(), request.measurements(), List.of(), List.of(), userId);
    }

    @Override
    public TriageV2SessionResponse continueSession(TriageV2ContinueRequest request, UUID userId) {
        requireEnabled();
        rejectCallerAuthoredClinicalState(request.signals(), request.measurements(), Map.of());
        structuredSignals(request.signals());
        structuredMeasurements(request.measurements());
        IntakeSession session = locked(request.sessionId(), userId);
        Map<String, Object> envelope = envelope(session);
        if (request.requestId().equals(envelope.get("lastRequestId"))) return publicResponse(envelope);
        int currentVersion = integer(envelope.get("stateVersion"), 0);
        if (request.expectedStateVersion() != currentVersion) {
            metrics.recordFailure(TriageV2Metrics.Failure.STATE_CONFLICT);
            throw conflict("TRIAGE_V2_STATE_VERSION_CONFLICT");
        }
        if (session.getStatus() == IntakeStatus.COMPLETED || session.getStatus() == IntakeStatus.FAILED) {
            throw conflict("TRIAGE_V2_SESSION_TERMINAL");
        }
        Map<String, Object> previousState = workflowState(envelope);
        // The only trusted route from an answer to a clinical belief. The client supplied nothing
        // but identifiers; everything below is derived here, on the server.
        DerivedAnswers derived = mapCanonicalAnswers(request, previousState);

        return executeAndPersist(session, currentVersion, request.requestId(), request.messageId(),
                request.message(), session.getBabyProfileId() != null ? session.getBabyProfileId()
                        : session.getMotherProfileId(), null, Map.of(), previousState,
                derived.signals(), request.measurements(), derived.answeredQuestionIds(),
                derived.optionCodes(), userId);
    }

    /** Signals and answered questions derived from one batch of answers. */
    private record DerivedAnswers(
            Map<String, Object> signals,
            List<String> answeredQuestionIds,
            List<String> optionCodes) {
    }

    /**
     * Resolves every answered question in the turn against the session's own resolved target and
     * stage.
     *
     * <p>Answers are folded in order, and each one is mapped against the signals accumulated so
     * far rather than against the stored state alone. That is what lets two answers in the same
     * round contradict each other and be marked CONFLICTED instead of the later one silently
     * overwriting the earlier.
     */
    private DerivedAnswers mapCanonicalAnswers(
            TriageV2ContinueRequest request, Map<String, Object> previousState) {
        if (request.answers().isEmpty()) {
            return new DerivedAnswers(Map.of(), List.of(), List.of());
        }
        // A session can reach here with no stored workflow state — a first follow-up, or a turn
        // that previously fell back. Treat that as "nothing known yet" rather than dereferencing
        // null: an unresolved target simply makes entity-specific answers fail their own check.
        Map<String, Object> priorState = previousState == null ? Map.of() : previousState;
        TargetEntity target = enumValue(text(priorState.get("targetEntity")),
                TargetEntity.class, TargetEntity.UNKNOWN);
        CareStage stage = enumValue(text(priorState.get("stage")),
                CareStage.class, CareStage.UNKNOWN);

        Map<String, Object> accumulated = new LinkedHashMap<>(objectMap(priorState.get("signals")));
        Map<String, Object> derived = new LinkedHashMap<>();
        List<String> answered = new ArrayList<>();
        List<String> optionCodes = new ArrayList<>();
        Set<String> seenQuestions = new LinkedHashSet<>();

        for (TriageV2AnswerSelection answer : request.answers()) {
            if (!seenQuestions.add(answer.questionId())) {
                throw new TriageException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "TRIAGE_V2_DUPLICATE_ANSWER",
                        "The same question was answered twice in one turn");
            }
            CanonicalAnswerMapper.AnswerMapping mapping = answerMapper.map(
                    answer.questionId(), answer.optionCode(), request.messageId(),
                    target, stage, accumulated);
            answered.add(mapping.answeredQuestionId());
            optionCodes.add(answer.optionCode());
            Map<String, Object> mapped = mapping.toSignals();
            derived.putAll(mapped);
            accumulated.putAll(mapped);
        }
        return new DerivedAnswers(
                Map.copyOf(derived), List.copyOf(answered), List.copyOf(optionCodes));
    }

    private static <E extends Enum<E>> E enumValue(String value, Class<E> type, E fallback) {
        if (value == null) return fallback;
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException unknown) {
            return fallback;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public TriageV2SessionResponse get(UUID sessionId, UUID userId) {
        requireEnabled();
        return publicResponse(envelope(repository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> notFound("TRIAGE_V2_SESSION_NOT_FOUND"))));
    }

    @Override
    public TriageV2SessionResponse cancel(UUID sessionId, int expectedStateVersion, UUID userId) {
        requireEnabled();
        IntakeSession session = locked(sessionId, userId);
        Map<String, Object> envelope = envelope(session);
        int current = integer(envelope.get("stateVersion"), 0);
        if (expectedStateVersion != current) {
            metrics.recordFailure(TriageV2Metrics.Failure.STATE_CONFLICT);
            throw conflict("TRIAGE_V2_STATE_VERSION_CONFLICT");
        }
        if (session.getStatus() == IntakeStatus.COMPLETED) return publicResponse(envelope);
        Map<String, Object> priorState = state(envelope);
        priorState.put("stateVersion", current + 1);
        priorState.put("expectedStateVersion", current + 1);
        priorState.put("stopConversation", true);
        priorState.put("requiredAction", "SESSION_CANCELLED");
        priorState.put("completionReason", "CANCELLED_BY_USER");
        TriageV2SessionResponse response = response(session.getId(), priorState, fallbackReadiness());
        persist(session, priorState, response, "CANCELLED", current + 1, IntakeStatus.FAILED);
        return response;
    }

    private TriageV2SessionResponse executeAndPersist(
            IntakeSession session, int currentVersion, String requestId, String messageId,
            String message, UUID profileId, String selectedTarget, Map<String, Object> journeyContext,
            Map<String, Object> previousState,
            Map<String, Object> signals, Map<String, Object> measurements,
            List<String> answeredQuestionIds, List<String> submittedOptionCodes, UUID userId) {
        long started = System.nanoTime();
        String expectedHash = readinessService.registry().map(TriageRuleRegistry::rulesetSha256).orElse(null);
        if (!readinessService.isReady() || expectedHash == null) {
            return persistFallback(session, currentVersion, requestId, signals);
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sessionId", session.getId().toString());
        payload.put("stateVersion", currentVersion);
        payload.put("expectedStateVersion", currentVersion);
        payload.put("requestId", requestId);
        payload.put("messageId", messageId);
        payload.put("latestUserMessage", message);
        payload.put("activeProfileId", profileId == null ? null : profileId.toString());
        payload.put("selectedTarget", selectedTarget == null ? "UNKNOWN" : selectedTarget);
        payload.put("journeyContext", journeyContext);
        payload.put("previousState", previousState);
        payload.put("signals", structuredSignals(signals));
        payload.put("measurements", structuredMeasurements(measurements));
        payload.put("answeredQuestionIds",
                answeredQuestionIds == null ? List.of() : answeredQuestionIds);
        payload.put("submittedOptionCodes",
                submittedOptionCodes == null ? List.of() : submittedOptionCodes);
        payload.put("expectedRulesetHash", expectedHash);
        try {
            TriageV2WorkflowClient.WorkflowResult result = workflowClient.executeTurn(payload);
            if (!"READY".equals(result.readiness()) || !expectedHash.equals(result.rulesetHash())) {
                metrics.recordFailure(TriageV2Metrics.Failure.HASH_MISMATCH);
                throw new IllegalStateException("Triage V2 ruleset handshake failed");
            }
            Map<String, Object> newState = new LinkedHashMap<>(result.state());
            validateWorkflowState(session, currentVersion, expectedHash, newState);
            newState.put("signals", structuredSignals(objectMap(newState.get("signals"))));
            newState.put("measurements", structuredMeasurements(objectMap(newState.get("measurements"))));
            try {
                newState.put("citations", verifiedCitations(newState.get("citations")));
            } catch (RuntimeException rejectedCitation) {
                // Evidence is optional and post-outcome. A broken/unverified citation must
                // disappear, but it must never erase or downgrade a valid RED/YELLOW result.
                if (!(rejectedCitation instanceof TriageException)) {
                    metrics.recordFailure(TriageV2Metrics.Failure.CITATION_REJECTED);
                }
                newState.put("citations", List.of());
            }
            newState.put("stateVersion", currentVersion + 1);
            newState.put("expectedStateVersion", currentVersion + 1);
            TriageV2SessionResponse response = response(session.getId(), newState,
                    readinessService.statusReport());
            persist(session, sanitizeState(newState), response, requestId, currentVersion + 1,
                    Boolean.TRUE.equals(newState.get("stopConversation"))
                            ? IntakeStatus.COMPLETED : IntakeStatus.NEED_MORE_INFO);
            metrics.recordTurn(text(newState.get("triageOutcome")),
                    (System.nanoTime() - started) / 1_000_000,
                    strings(newState.get("plannedQuestionIds")).size(),
                    "CONFLICTED".equals(text(newState.get("targetEntity"))));
            return response;
        } catch (RuntimeException unavailable) {
            return persistFallback(session, currentVersion, requestId, signals);
        }
    }

    private TriageV2SessionResponse persistFallback(
            IntakeSession session, int currentVersion, String requestId, Map<String, Object> signals) {
        IndependentGlobalSafetyFallback.FallbackVerdict verdict = fallback.screen(structuredSignals(signals));
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("sessionId", session.getId().toString());
        state.put("stateVersion", currentVersion + 1);
        state.put("expectedStateVersion", currentVersion + 1);
        state.put("targetEntity", "UNKNOWN");
        state.put("intent", "UNKNOWN");
        state.put("stage", "UNKNOWN");
        state.put("triageOutcome", verdict.outcome());
        state.put("requiredAction", verdict.actionCode());
        state.put("stopConversation", verdict.stopConversation());
        state.put("plannedQuestionIds", List.of());
        state.put("scopeStatus", "UNKNOWN");
        state.put("pendingRiskStatuses", List.of());
        state.put("completionReason", verdict.completionReason());
        state.put("reasonCodes", verdict.reasonCodes());
        state.put("rulesetVersion", null);
        state.put("rulesetHash", null);
        state.put("readingLinks", List.of());
        TriageV2SessionResponse response = response(session.getId(), state, fallbackReadiness());
        persist(session, state, response, requestId, currentVersion + 1,
                "RED".equals(verdict.outcome()) ? IntakeStatus.COMPLETED : IntakeStatus.NEED_MORE_INFO);
        metrics.recordFailure(TriageV2Metrics.Failure.FALLBACK);
        metrics.recordTurn(verdict.outcome(), 0, 0, false);
        return response;
    }

    private void persist(IntakeSession session, Map<String, Object> state,
                         TriageV2SessionResponse response, String requestId, int version,
                         IntakeStatus status) {
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("contract", SCHEMA_VERSION);
        envelope.put("stateVersion", version);
        envelope.put("lastRequestId", requestId);
        envelope.put("retentionUntil", Instant.now().plus(retentionDays, ChronoUnit.DAYS).toString());
        envelope.put("v2State", state);
        envelope.put("publicResponse", objectMapper.convertValue(response, new TypeReference<Map<String, Object>>() {}));
        try {
            String json = objectMapper.writeValueAsString(envelope);
            session.setResultJson(json);
            session.setRawAiResponse(null);
            session.setStatus(status);
            session.setRiskLevel(risk(response.outcome()));
            session.setEmergency("RED".equals(response.outcome()));
            session.setCompletedAt(status == IntakeStatus.COMPLETED || status == IntakeStatus.FAILED
                    ? Instant.now() : null);
            session.setSchemaVersion(SCHEMA_VERSION);
            session.setContentHash(sha256(json));
            repository.save(session);
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("Could not persist Triage V2 state", failure);
        }
    }

    private TriageV2SessionResponse requireV2Replay(IntakeSession session) {
        if (!SCHEMA_VERSION.equals(session.getSchemaVersion())) {
            throw conflict("TRIAGE_V2_IDEMPOTENCY_KEY_CONFLICT");
        }
        return publicResponse(envelope(session));
    }

    private IntakeSession locked(UUID id, UUID userId) {
        IntakeSession session = repository.findForUpdateByIdAndUserId(id, userId)
                .orElseThrow(() -> notFound("TRIAGE_V2_SESSION_NOT_FOUND"));
        if (!SCHEMA_VERSION.equals(session.getSchemaVersion())) {
            throw notFound("TRIAGE_V2_SESSION_NOT_FOUND");
        }
        return session;
    }

    private Map<String, Object> envelope(IntakeSession session) {
        try {
            Map<String, Object> value = objectMapper.readValue(session.getResultJson(), new TypeReference<>() {});
            if (!SCHEMA_VERSION.equals(value.get("contract"))) throw new IllegalStateException();
            return value;
        } catch (Exception failure) {
            throw new TriageException(HttpStatus.SERVICE_UNAVAILABLE,
                    "TRIAGE_V2_STATE_UNAVAILABLE", "Triage V2 state is unavailable");
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> state(Map<String, Object> envelope) {
        Object value = envelope.get("v2State");
        if (!(value instanceof Map<?, ?>)) throw new IllegalStateException("Missing Triage V2 state");
        return new LinkedHashMap<>((Map<String, Object>) value);
    }

    private static Map<String, Object> workflowState(Map<String, Object> envelope) {
        Map<String, Object> value = state(envelope);
        // A Java-only outage envelope is intentionally sparse. Once Python recovers, start a
        // fresh deterministic graph turn at the persisted version instead of trusting or
        // trying to resume that synthetic fallback state.
        return value.containsKey("rawMessages") && value.containsKey("processedRequestIds")
                ? value : null;
    }

    private TriageV2SessionResponse publicResponse(Map<String, Object> envelope) {
        return objectMapper.convertValue(envelope.get("publicResponse"), TriageV2SessionResponse.class);
    }

    private TriageV2SessionResponse response(UUID id, Map<String, Object> state,
                                             Map<String, Object> readiness) {
        return new TriageV2SessionResponse(id, integer(state.get("stateVersion"), 0),
                text(state.get("targetEntity")), text(state.get("intent")), text(state.get("stage")),
                text(state.get("triageOutcome")), text(state.get("requiredAction")),
                Boolean.TRUE.equals(state.get("stopConversation")), strings(state.get("plannedQuestionIds")),
                text(state.get("scopeStatus")), strings(state.get("pendingRiskStatuses")),
                text(state.get("completionReason")), text(state.get("rulesetVersion")),
                text(state.get("rulesetHash")), verifiedCitations(state.get("citations")),
                disclaimerPolicy.disclaimerText(),
                new LinkedHashMap<>(readiness));
    }

    private List<Map<String, Object>> verifiedCitations(Object value) {
        if (value == null) return List.of();
        if (!(value instanceof List<?> list) || list.size() > 4) throw citationRejected();
        List<Map<String, Object>> safe = new ArrayList<>();
        int whoCount = 0;
        Set<String> allowed = Set.of("sourceId", "title", "organization", "publisher", "url",
                "domain", "section", "contentHash", "sourceStatus", "retrievalMode", "ruleIds");
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> map)
                    || map.keySet().stream().anyMatch(key -> !(key instanceof String name)
                    || !allowed.contains(name))) throw citationRejected();
            String sourceId = boundedText(map.get("sourceId"), 80);
            String title = boundedText(map.get("title"), 255);
            String organization = boundedText(map.get("organization"), 255);
            String publisher = boundedText(map.get("publisher"), 255);
            String url = boundedText(map.get("url"), 500);
            String domain = boundedText(map.get("domain"), 253).toLowerCase();
            String section = boundedText(map.get("section"), 255);
            String hash = boundedText(map.get("contentHash"), 64).toLowerCase();
            if (!"SOURCE_VERIFIED".equals(map.get("sourceStatus"))
                    || !"LOCAL_BM25".equals(map.get("retrievalMode")) || !SHA256.matcher(hash).matches()) {
                throw citationRejected();
            }
            URI uri;
            try { uri = URI.create(url); } catch (IllegalArgumentException failure) {
                throw citationRejected();
            }
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase().replaceFirst("^www\\.", "");
            if (!"https".equals(uri.getScheme()) || !(host.equals(domain) || host.endsWith("." + domain))
                    || !evidenceSourceService.isApprovedDeepLink(uri)) throw citationRejected();
            boolean who = domain.equals("who.int") || domain.endsWith(".who.int")
                    || (organization + " " + publisher).toLowerCase(java.util.Locale.ROOT)
                    .contains("world health organization");
            if (who && ++whoCount > 1) throw citationRejected();
            List<String> ruleIds = strings(map.get("ruleIds"));
            if (ruleIds.size() > 32 || ruleIds.stream().anyMatch(id -> id.length() > 80)) {
                throw citationRejected();
            }
            Map<String, Object> citation = new LinkedHashMap<>();
            citation.put("sourceId", sourceId); citation.put("title", title);
            citation.put("organization", organization); citation.put("publisher", publisher);
            citation.put("url", url); citation.put("domain", domain); citation.put("section", section);
            citation.put("contentHash", hash); citation.put("sourceStatus", "SOURCE_VERIFIED");
            citation.put("retrievalMode", "LOCAL_BM25"); citation.put("ruleIds", ruleIds);
            safe.add(citation);
        }
        return List.copyOf(safe);
    }

    private TriageException citationRejected() {
        metrics.recordFailure(TriageV2Metrics.Failure.CITATION_REJECTED);
        return invalidStructuredPayload();
    }

    private static String boundedText(Object value, int max) {
        if (!(value instanceof String text) || text.isBlank() || text.length() > max) {
            throw invalidStructuredPayload();
        }
        return text;
    }

    private Map<String, Object> sanitizeState(Map<String, Object> value) {
        Map<String, Object> safe = new LinkedHashMap<>(value);
        // Error prose is telemetry-only and can contain upstream exception text. Persist
        // the controlled state without it rather than risking raw health-text retention.
        safe.remove("processingErrors");
        safe.put("latestUserMessage", REDACTED);
        Object messages = safe.get("rawMessages");
        if (messages instanceof List<?> list) {
            List<Object> sanitized = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    Map<String, Object> copy = new LinkedHashMap<>();
                    for (String key : List.of("role", "messageId")) {
                        if (map.get(key) instanceof String text) copy.put(key, text);
                    }
                    copy.put("content", REDACTED);
                    sanitized.add(copy);
                }
            }
            safe.put("rawMessages", sanitized);
        }
        return safe;
    }

    private void validateWorkflowState(IntakeSession session, int currentVersion, String expectedHash,
                                       Map<String, Object> state) {
        if (!session.getId().toString().equals(state.get("sessionId"))
                || integer(state.get("stateVersion"), -1) != currentVersion
                || !expectedHash.equals(state.get("rulesetHash"))
                || !isAllowedOutcome(state.get("triageOutcome"))
                || !TARGETS.contains(state.get("targetEntity"))
                || !CARE_STAGES.contains(state.get("stage"))
                || !INTENTS.contains(state.get("intent"))
                || state.keySet().stream().anyMatch(key -> !WORKFLOW_STATE_FIELDS.contains(key))
                || !isValidCoverageState(state)
                || !(state.get("requiredAction") instanceof String action) || action.isBlank()
                || !(state.get("stopConversation") instanceof Boolean)
                || strings(state.get("plannedQuestionIds")).size() > questionCatalog.maxQuestionsPerTurn()
                || strings(state.get("plannedQuestionIds")).stream()
                        .anyMatch(id -> questionCatalog.question(id).isEmpty())
                || (!"NEEDS_MORE_INFO".equals(state.get("triageOutcome"))
                        && !strings(state.get("plannedQuestionIds")).isEmpty())) {
            throw new IllegalStateException("Invalid Triage V2 workflow state");
        }
        try {
            if (objectMapper.writeValueAsBytes(state).length > 65_536) {
                throw new IllegalStateException("Triage V2 workflow state exceeds persistence limit");
            }
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("Invalid Triage V2 workflow state", failure);
        }
    }

    private static boolean isAllowedOutcome(Object value) {
        return value instanceof String outcome && OUTCOMES.contains(outcome);
    }

    private static boolean isValidCoverageState(Map<String, Object> state) {
        for (String field : List.of("subjectScope", "complaintScope", "outcomeAppliesTo",
                "coverageStatus", "selectedCatalogType")) {
            Object value = state.get(field);
            if (value != null && (!(value instanceof String text) || text.isBlank()
                    || text.length() > 64)) return false;
        }
        for (String field : List.of("coverageReasonCodes", "supportedSymptomCodes",
                "unsupportedSymptomCodes", "coverageLimitations")) {
            if (!isBoundedStringList(state.get(field), 50, 128)) return false;
        }
        for (String field : List.of("blocksClinicalQuestionPlanner", "blocksGreen")) {
            Object value = state.get(field);
            if (value != null && !(value instanceof Boolean)) return false;
        }
        return isRejectedQuestionList(state.get("rejectedQuestionIds"));
    }

    private static boolean isBoundedStringList(Object value, int maxItems, int maxLength) {
        if (value == null) return true;
        if (!(value instanceof List<?> list) || list.size() > maxItems) return false;
        return list.stream().allMatch(item -> item instanceof String text
                && !text.isBlank() && text.length() <= maxLength);
    }

    private static boolean isRejectedQuestionList(Object value) {
        if (value == null) return true;
        if (!(value instanceof List<?> list) || list.size() > 50) return false;
        return list.stream().allMatch(item -> {
            if (!(item instanceof Map<?, ?> map)
                    || !map.keySet().stream().allMatch(key -> Set.of("questionId", "reason").contains(key))) {
                return false;
            }
            return map.get("questionId") instanceof String questionId && !questionId.isBlank()
                    && questionId.length() <= 128
                    && map.get("reason") instanceof String reason && !reason.isBlank()
                    && reason.length() <= 128;
        });
    }

    private static void rejectCallerAuthoredClinicalState(Map<String, Object> signals,
                                                           Map<String, Object> measurements,
                                                           Map<String, Object> journeyContext) {
        if ((signals != null && !signals.isEmpty())
                || (measurements != null && !measurements.isEmpty())
                || (journeyContext != null && !journeyContext.isEmpty())) {
            throw new TriageException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "TRIAGE_V2_UNTRUSTED_CLINICAL_STATE",
                    "Clinical state must be derived by the server from canonical answers");
        }
    }

    private void validateSelectedProfile(UUID profileId, String selectedTarget, UUID userId) {
        if (profileId == null) return;
        if ("MOTHER".equals(selectedTarget) && profileId.equals(userId)) return;
        if ("BABY".equals(selectedTarget)
                && babyProfileRepository.findByIdAndOwnerUserId(profileId, userId).isPresent()) return;
        throw new TriageException(HttpStatus.FORBIDDEN, "TRIAGE_V2_PROFILE_FORBIDDEN",
                "Selected profile does not belong to the current user");
    }

    private Map<String, Object> fallbackReadiness() {
        Map<String, Object> report = new LinkedHashMap<>(readinessService.statusReport());
        report.put("technicalStatus", "FALLBACK_ONLY");
        return report;
    }

    private Map<String, Object> structuredSignals(Map<String, Object> value) {
        if (value == null) return Map.of();
        requireStructuredSize(value);
        Set<String> allowedCodes = readinessService.registry()
                .map(registry -> registry.signalDisplayText().keySet())
                .orElse(INDEPENDENT_SIGNAL_CODES);
        Map<String, Object> safe = new LinkedHashMap<>();
        value.forEach((code, observation) -> {
            requireCode(code, allowedCodes);
            safe.put(code, structuredSignalValue(code, observation));
        });
        return safe;
    }

    private static Object structuredSignalValue(String code, Object value) {
        if (value instanceof String text) {
            if (!PRESENCE.contains(text)) throw invalidStructuredPayload();
            return text;
        }
        if (value instanceof List<?> list) {
            if (list.isEmpty() || list.size() > 4) throw invalidStructuredPayload();
            return list.stream().map(item -> structuredSignalValue(code, item)).toList();
        }
        if (!(value instanceof Map<?, ?> map) || map.keySet().stream().anyMatch(key ->
                !(key instanceof String name) || !OBSERVATION_FIELDS.contains(name))) {
            throw invalidStructuredPayload();
        }
        if (!PRESENCE.contains(map.get("presence"))) throw invalidStructuredPayload();
        for (String field : List.of("temporalStatus", "currentVsHistorical")) {
            if (map.containsKey(field) && !TEMPORAL.contains(map.get(field))) throw invalidStructuredPayload();
        }
        if (map.containsKey("historicalPresence") && !PRESENCE.contains(map.get("historicalPresence"))) {
            throw invalidStructuredPayload();
        }
        for (String field : List.of("explicitNegation", "current")) {
            if (map.containsKey(field) && !(map.get(field) instanceof Boolean)) throw invalidStructuredPayload();
        }
        if (map.containsKey("provenance") && !PROVENANCE.contains(map.get("provenance"))) {
            throw invalidStructuredPayload();
        }
        if (map.containsKey("conflictStatus") && !CONFLICT_STATUS.contains(map.get("conflictStatus"))) {
            throw invalidStructuredPayload();
        }
        for (String field : List.of("sourceQuestionId", "sourceOptionCode", "mappingRuleVersion")) {
            if (map.containsKey(field) && !(map.get(field) instanceof String reference
                    && SAFE_REFERENCE.matcher(reference).matches())) {
                throw invalidStructuredPayload();
            }
        }
        Map<String, Object> safe = new LinkedHashMap<>();
        map.forEach((key, item) -> safe.put(String.valueOf(key), item));
        return safe;
    }

    private Map<String, Object> structuredMeasurements(Map<String, Object> value) {
        if (value == null) return Map.of();
        requireStructuredSize(value);
        Set<String> allowedCodes = java.util.stream.Stream.concat(
                questionCatalog.questions().values().stream()
                        .filter(QuestionCatalog.Question::measurement)
                        .flatMap(question -> question.resolvesFields().stream()),
                java.util.stream.Stream.of("temperatureC", "babyAgeMonths"))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        Map<String, Object> safe = new LinkedHashMap<>();
        value.forEach((code, measurement) -> {
            requireCode(code, allowedCodes);
            if (measurement instanceof Number number && !(measurement instanceof Boolean)) {
                requireFinite(number);
                safe.put(code, number);
                return;
            }
            if (!(measurement instanceof Map<?, ?> map) || map.keySet().stream().anyMatch(key ->
                    !(key instanceof String name) || !Set.of("value", "unit", "status",
                            "temporalStatus", "provenance").contains(name))) {
                throw invalidStructuredPayload();
            }
            if (map.get("value") != null) {
                if (!(map.get("value") instanceof Number number)) throw invalidStructuredPayload();
                requireFinite(number);
            }
            if (map.containsKey("unit") && !UNITS.contains(map.get("unit"))) throw invalidStructuredPayload();
            if (map.containsKey("status") && !Set.of("UNKNOWN", "UNAWARE_OR_UNMEASURABLE")
                    .contains(map.get("status"))) throw invalidStructuredPayload();
            if (map.containsKey("temporalStatus") && !TEMPORAL.contains(map.get("temporalStatus"))) {
                throw invalidStructuredPayload();
            }
            if (map.containsKey("provenance") && !PROVENANCE.contains(map.get("provenance"))) {
                throw invalidStructuredPayload();
            }
            Map<String, Object> copy = new LinkedHashMap<>();
            map.forEach((key, item) -> copy.put(String.valueOf(key), item));
            safe.put(code, copy);
        });
        return safe;
    }

    private static Map<String, Object> structuredJourneyContext(Map<String, Object> value) {
        if (value == null) return Map.of();
        Set<String> allowed = Set.of(
                "stage", "possiblePregnancy", "gestationalWeek", "postpartumDay", "babyAgeMonths");
        if (value.keySet().stream().anyMatch(key -> !allowed.contains(key))) {
            throw invalidStructuredPayload();
        }
        Map<String, Object> safe = new LinkedHashMap<>();
        if (value.containsKey("stage")) {
            if (!CARE_STAGES.contains(value.get("stage"))) throw invalidStructuredPayload();
            safe.put("stage", value.get("stage"));
        }
        if (value.containsKey("possiblePregnancy")) {
            if (!POSSIBLE_PREGNANCY.contains(value.get("possiblePregnancy"))) {
                throw invalidStructuredPayload();
            }
            safe.put("possiblePregnancy", value.get("possiblePregnancy"));
        }
        for (String field : List.of("gestationalWeek", "postpartumDay", "babyAgeMonths")) {
            if (!value.containsKey(field)) continue;
            if (!(value.get(field) instanceof Byte || value.get(field) instanceof Short
                    || value.get(field) instanceof Integer || value.get(field) instanceof Long)) {
                throw invalidStructuredPayload();
            }
            long number = ((Number) value.get(field)).longValue();
            if (number < 0 || number > 1_000_000) throw invalidStructuredPayload();
            safe.put(field, value.get(field));
        }
        return safe;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> objectMap(Object value) {
        if (value == null) return Map.of();
        if (!(value instanceof Map<?, ?>)) throw invalidStructuredPayload();
        return (Map<String, Object>) value;
    }

    private static void requireStructuredSize(Map<String, Object> value) {
        if (value.size() > 50) throw invalidStructuredPayload();
    }

    private static void requireCode(String code, Set<String> allowedCodes) {
        if (code == null || !allowedCodes.contains(code)) throw invalidStructuredPayload();
    }

    private static void requireFinite(Number number) {
        double value = number.doubleValue();
        if (!Double.isFinite(value) || Math.abs(value) > 1_000_000) throw invalidStructuredPayload();
    }

    private static TriageException invalidStructuredPayload() {
        return new TriageException(HttpStatus.BAD_REQUEST, "TRIAGE_V2_PAYLOAD_INVALID",
                "Structured triage payload is invalid");
    }

    private void requireEnabled() {
        if (!enabled) throw notFound("TRIAGE_V2_INTERNAL_ONLY");
    }

    private static RiskLevel risk(String value) {
        return switch (String.valueOf(value)) {
            case "RED" -> RiskLevel.RED;
            case "YELLOW" -> RiskLevel.YELLOW;
            case "GREEN" -> RiskLevel.GREEN;
            default -> null;
        };
    }

    private static int integer(Object value, int fallback) {
        return value instanceof Number number ? number.intValue() : fallback;
    }

    private static String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static List<String> strings(Object value) {
        if (!(value instanceof List<?> list)) return List.of();
        return list.stream().map(String::valueOf).toList();
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception failure) {
            throw new IllegalStateException("SHA-256 unavailable", failure);
        }
    }

    private static TriageException conflict(String code) {
        return new TriageException(HttpStatus.CONFLICT, code, "Triage V2 request conflict");
    }

    private static TriageException notFound(String code) {
        return new TriageException(HttpStatus.NOT_FOUND, code, "Triage V2 session not found");
    }
}
