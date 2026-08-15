package com.carebridge.backend.triage.service.impl;

import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.ai.event.EmergencyEscalationTriggered;
import com.carebridge.backend.triage.IntakeStatus;
import com.carebridge.backend.triage.RiskLevel;
import com.carebridge.backend.triage.TriageStage;
import com.carebridge.backend.triage.dto.request.TriageAnswerSelection;
import com.carebridge.backend.triage.dto.request.TriageSessionContinueRequest;
import com.carebridge.backend.triage.dto.request.TriageSessionStartRequest;
import com.carebridge.backend.triage.dto.request.StartIntakeConversationRequest;
import com.carebridge.backend.triage.dto.response.TriageSessionResponse;
import com.carebridge.backend.triage.dto.response.TriageQuestionOptionResponse;
import com.carebridge.backend.triage.dto.response.TriageQuestionResponse;
import com.carebridge.backend.triage.entity.IntakeSession;
import com.carebridge.backend.triage.event.IntakeSessionCompleted;
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
import com.carebridge.backend.triage.rules.TriageReadinessService;
import com.carebridge.backend.triage.service.ITriageConsentService;
import com.carebridge.backend.triage.service.ITriageSessionService;
import com.carebridge.backend.triage.service.LifecycleBinding;
import com.carebridge.backend.triage.service.LifecycleIntakeBindingService;
import com.carebridge.backend.triage.service.TriageWorkflowClient;
import com.carebridge.backend.triage.service.TriageMetrics;
import com.carebridge.backend.triage.service.EvidenceSourceService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.math.BigDecimal;
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

/** Java authority boundary for canonical deterministic triage sessions. */
@Service
@Transactional
public class CanonicalTriageSessionService implements ITriageSessionService {
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
    private static final Set<String> EMERGENCY_ACTIONS = Set.of(
            "IMMEDIATE_EMERGENCY_ASSESSMENT", "IMMEDIATE_SAFETY_SUPPORT");
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
            "submittedOptionQuestionIds",
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
            "processingErrors", "finalResponse", "citations", "evidenceStatus",
            "evidenceRejections", "rationale", "readingLinks");
    private static final Pattern SHA256 = Pattern.compile("^[0-9a-f]{64}$");

    private final IIntakeSessionRepository repository;
    private final IntakeSessionWriter writer;
    private final TriageWorkflowClient workflowClient;
    private final TriageReadinessService readinessService;
    private final IndependentGlobalSafetyFallback fallback;
    private final ITriageConsentService consentService;
    private final TriageDisclaimerPolicy disclaimerPolicy;
    private final QuestionCatalog questionCatalog;
    private final CanonicalAnswerMapper answerMapper;
    private final EvidenceSourceService evidenceSourceService;
    private final BabyProfileRepository babyProfileRepository;
    private final TriageMetrics metrics;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final int retentionDays;

    /** Validates and binds an optional Mother Journey/Baby Profile origin. */
    @Autowired(required = false)
    private LifecycleIntakeBindingService lifecycleBindingService;

    public CanonicalTriageSessionService(
            IIntakeSessionRepository repository,
            IntakeSessionWriter writer,
            TriageWorkflowClient workflowClient,
            TriageReadinessService readinessService,
            IndependentGlobalSafetyFallback fallback,
            ITriageConsentService consentService,
            TriageDisclaimerPolicy disclaimerPolicy,
            QuestionCatalog questionCatalog,
            CanonicalAnswerMapper answerMapper,
            EvidenceSourceService evidenceSourceService,
            BabyProfileRepository babyProfileRepository,
            TriageMetrics metrics,
            ApplicationEventPublisher eventPublisher,
            ObjectMapper objectMapper,
            @Value("${carebridge.triage.engine.enabled:true}") boolean enabled,
            @Value("${carebridge.triage.engine.retention-days:30}") int retentionDays) {
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
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.retentionDays = Math.max(1, Math.min(retentionDays, 90));
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public TriageSessionResponse start(TriageSessionStartRequest request, UUID userId) {
        requireEnabled();
        String requestFingerprint = startFingerprint(request);
        rejectCallerAuthoredClinicalState(request.signals(), request.measurements(), request.journeyContext());
        validateSelectedProfile(request.profileId(), request.selectedTarget(), userId);
        consentService.ensureActiveConsent(userId);
        // selectedStage is an explicit UI choice, not caller-authored evidence. Convert it at the
        // authority boundary into the narrow context understood by the deterministic resolver.
        // The general journeyContext/signals/measurements maps remain rejected below.
        Map<String, Object> journeyContext = selectedStageContext(request.selectedStage());
        structuredSignals(request.signals());
        structuredMeasurements(request.measurements());
        Object suppliedVersion = request.consentContext().get("disclaimerVersion");
        if (suppliedVersion != null && !disclaimerPolicy.currentVersion().equals(String.valueOf(suppliedVersion))) {
            throw conflict("TRIAGE_CONSENT_CONTEXT_MISMATCH");
        }
        LifecycleBinding lifecycle = bindLifecycle(request, userId);
        IntakeSession existing = repository.findByUserIdAndClientRequestId(userId, request.requestId())
                .orElse(null);
        if (existing != null) {
            validateLifecycleReplay(existing, lifecycle);
            return requireV2Replay(existing, requestFingerprint);
        }

        IntakeSession candidate = IntakeSession.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .babyProfileId("BABY".equals(request.selectedTarget()) ? request.profileId() : null)
                .motherProfileId("MOTHER".equals(request.selectedTarget()) ? request.profileId() : null)
                .stage(legacyStage(request.selectedStage()))
                .clientRequestId(request.requestId())
                .journeyId(lifecycle == null ? null : lifecycle.journeyId())
                .originDashboard(lifecycle == null ? null : lifecycle.originDashboard())
                .originReferenceId(lifecycle == null ? null : lifecycle.originReferenceId())
                .continuationToken(lifecycle == null ? null : lifecycle.continuationToken())
                .continuationExpiresAt(lifecycle == null ? null : lifecycle.continuationExpiresAt())
                .symptoms("TRIAGE_REDACTED")
                .contentHash(requestFingerprint)
                .status(IntakeStatus.PROCESSING)
                .disclaimer(disclaimerPolicy.disclaimerText())
                .disclaimerVersion(disclaimerPolicy.currentVersion())
                .createdAt(Instant.now())
                .createdBy(userId)
                .schemaVersion(SCHEMA_VERSION)
                .build();
        writer.insertConversationIfAbsent(candidate);
        IntakeSession session = repository.findByUserIdAndClientRequestId(userId, request.requestId())
                .orElseThrow(() -> new IllegalStateException("Triage idempotency winner unavailable"));
        if (!session.getId().equals(candidate.getId())) {
            validateLifecycleReplay(session, lifecycle);
            return requireV2Replay(session, requestFingerprint);
        }
        if (lifecycle != null) lifecycleBindingService.recordCreated();

        return executeAndPersist(session, 0, request.requestId(), requestFingerprint,
                request.messageId(), request.message(),
                request.profileId(), request.selectedTarget(), journeyContext, null,
                request.signals(), request.measurements(), List.of(), List.of(), List.of(), userId);
    }

    @Override
    public TriageSessionResponse continueSession(TriageSessionContinueRequest request, UUID userId) {
        requireEnabled();
        rejectCallerAuthoredClinicalState(request.signals(), request.measurements(), Map.of());
        structuredSignals(request.signals());
        structuredMeasurements(request.measurements());
        IntakeSession session = locked(request.sessionId(), userId);
        Map<String, Object> envelope = envelope(session);
        String mutationFingerprint = continueFingerprint(request);
        if (request.requestId().equals(envelope.get("lastRequestId"))) {
            return requireContinueReplay(envelope, mutationFingerprint);
        }
        int currentVersion = integer(envelope.get("stateVersion"), 0);
        if (request.expectedStateVersion() != currentVersion) {
            metrics.recordFailure(TriageMetrics.Failure.STATE_CONFLICT);
            throw conflict("TRIAGE_STATE_VERSION_CONFLICT");
        }
        if (session.getStatus() == IntakeStatus.COMPLETED || session.getStatus() == IntakeStatus.FAILED) {
            throw conflict("TRIAGE_SESSION_TERMINAL");
        }
        IndependentGlobalSafetyFallback.FallbackVerdict immediate =
                fallback.screenWithLatestMessage(Map.of(), request.message());
        if ("RED".equals(immediate.outcome())) {
            return persistFallback(session, currentVersion, request.requestId(), mutationFingerprint,
                    Map.of(), request.message());
        }
        consentService.ensureActiveConsent(userId);
        Map<String, Object> previousState = workflowState(envelope);
        // The only trusted route from an answer to a clinical belief. The client supplied nothing
        // but identifiers; everything below is derived here, on the server.
        DerivedAnswers derived = mapCanonicalAnswers(request, previousState);

        Map<String, Object> resumedState = previousState == null
                ? null : new LinkedHashMap<>(previousState);
        if (resumedState != null) resumedState.putAll(derived.contextUpdates());
        return executeAndPersist(session, currentVersion, request.requestId(), mutationFingerprint,
                request.messageId(),
                request.message(), session.getBabyProfileId() != null ? session.getBabyProfileId()
                        : session.getMotherProfileId(), null, Map.of(), resumedState,
                derived.signals(), request.measurements(), derived.answeredQuestionIds(),
                derived.optionQuestionIds(), derived.optionCodes(), userId);
    }

    /** Signals and answered questions derived from one batch of answers. */
    private record DerivedAnswers(
            Map<String, Object> signals,
            Map<String, Object> contextUpdates,
            List<String> answeredQuestionIds,
            List<String> optionQuestionIds,
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
            TriageSessionContinueRequest request, Map<String, Object> previousState) {
        if (request.answers().isEmpty()) {
            return new DerivedAnswers(Map.of(), Map.of(), List.of(), List.of(), List.of());
        }
        // A session can reach here with no stored workflow state â€” a first follow-up, or a turn
        // that previously fell back. Treat that as "nothing known yet" rather than dereferencing
        // null: an unresolved target simply makes entity-specific answers fail their own check.
        Map<String, Object> priorState = previousState == null ? Map.of() : previousState;
        TargetEntity target = enumValue(text(priorState.get("targetEntity")),
                TargetEntity.class, TargetEntity.UNKNOWN);
        CareStage stage = enumValue(text(priorState.get("stage")),
                CareStage.class, CareStage.UNKNOWN);

        Map<String, Object> accumulated = new LinkedHashMap<>(objectMap(priorState.get("signals")));
        Map<String, Object> derived = new LinkedHashMap<>();
        Map<String, Object> contextUpdates = new LinkedHashMap<>();
        List<String> answered = new ArrayList<>();
        List<String> optionQuestionIds = new ArrayList<>();
        List<String> optionCodes = new ArrayList<>();
        Set<String> seenQuestions = new LinkedHashSet<>();
        Set<String> plannedQuestions = new LinkedHashSet<>(strings(priorState.get("plannedQuestionIds")));

        for (TriageAnswerSelection answer : request.answers()) {
            if (!seenQuestions.add(answer.questionId())) {
                throw new TriageException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "TRIAGE_DUPLICATE_ANSWER",
                        "The same question was answered twice in one turn");
            }
            if (questionCatalog.question(answer.questionId()).isEmpty()) {
                throw new TriageException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "TRIAGE_UNKNOWN_QUESTION", "Unknown canonical question");
            }
            if (!plannedQuestions.contains(answer.questionId())) {
                throw new TriageException(HttpStatus.CONFLICT,
                        "TRIAGE_ANSWER_NOT_PLANNED",
                        "Answer does not belong to the current planned question set");
            }
            QuestionCatalog.Question question = questionCatalog.question(answer.questionId())
                    .orElseThrow();
            if (answer.numericValue() != null) {
                validateNumericQuestionContext(question, target, stage);
                Map.Entry<String, Object> contextUpdate =
                        numericContextField(question, answer.numericValue());
                contextUpdates.put(contextUpdate.getKey(), contextUpdate.getValue());
                answered.add(answer.questionId());
                continue;
            }
            CanonicalAnswerMapper.AnswerMapping mapping = answerMapper.map(
                    answer.questionId(), answer.optionCode(), request.messageId(),
                    target, stage, accumulated);
            answered.add(mapping.answeredQuestionId());
            optionQuestionIds.add(mapping.answeredQuestionId());
            optionCodes.add(answer.optionCode());
            Map<String, Object> mapped = mapping.toSignals();
            derived.putAll(mapped);
            accumulated.putAll(mapped);
        }
        return new DerivedAnswers(
                Map.copyOf(derived), Map.copyOf(contextUpdates),
                List.copyOf(answered), List.copyOf(optionQuestionIds), List.copyOf(optionCodes));
    }

    private static Map.Entry<String, Object> numericContextField(
            QuestionCatalog.Question question, BigDecimal numericValue) {
        if (!"NUMBER".equals(question.answerType()) || numericValue.stripTrailingZeros().scale() > 0) {
            throw new TriageException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "TRIAGE_INVALID_NUMERIC_ANSWER", "Giá trị trả lời phải là số nguyên");
        }
        int value;
        try {
            value = numericValue.intValueExact();
        } catch (ArithmeticException invalid) {
            throw new TriageException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "TRIAGE_INVALID_NUMERIC_ANSWER", "Giá trị số nằm ngoài phạm vi cho phép");
        }
        return switch (question.questionId()) {
            case "Q_BABY_AGE_MONTHS" -> boundedContext("babyAgeMonths", value, 0, 23);
            case "Q_GESTATIONAL_WEEK" -> boundedContext("gestationalWeek", value, 1, 45);
            case "Q_POSTPARTUM_DAY" -> boundedContext(
                    "postpartumDay", value, 0, Integer.MAX_VALUE);
            default -> throw new TriageException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "TRIAGE_NUMERIC_QUESTION_UNSUPPORTED",
                    "Câu hỏi này không chấp nhận câu trả lời dạng số");
        };
    }

    private static void validateNumericQuestionContext(
            QuestionCatalog.Question question, TargetEntity target, CareStage stage) {
        if (!question.targetEntities().isEmpty() && !question.targetEntities().contains(target)) {
            throw new TriageException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "TRIAGE_ANSWER_ENTITY_MISMATCH",
                    "Câu trả lời không phù hợp với đối tượng đang được đánh giá");
        }
        if (!question.applicableStages().isEmpty()
                && !question.applicableStages().contains(stage)) {
            throw new TriageException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "TRIAGE_ANSWER_STAGE_MISMATCH",
                    "Câu trả lời không phù hợp với giai đoạn đang được đánh giá");
        }
    }

    private static Map.Entry<String, Object> boundedContext(
            String field, int value, int minimum, int maximum) {
        if (value < minimum || value > maximum) {
            throw new TriageException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "TRIAGE_NUMERIC_ANSWER_OUT_OF_RANGE",
                    "Giá trị số nằm ngoài phạm vi cho phép");
        }
        return Map.entry(field, value);
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
    public TriageSessionResponse get(UUID sessionId, UUID userId) {
        requireEnabled();
        return publicResponse(envelope(repository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> notFound("TRIAGE_SESSION_NOT_FOUND"))));
    }

    @Override
    public TriageSessionResponse cancel(UUID sessionId, int expectedStateVersion, UUID userId) {
        requireEnabled();
        IntakeSession session = locked(sessionId, userId);
        Map<String, Object> envelope = envelope(session);
        int current = integer(envelope.get("stateVersion"), 0);
        if (expectedStateVersion != current) {
            metrics.recordFailure(TriageMetrics.Failure.STATE_CONFLICT);
            throw conflict("TRIAGE_STATE_VERSION_CONFLICT");
        }
        if (session.getStatus() == IntakeStatus.COMPLETED) return publicResponse(envelope);
        Map<String, Object> priorState = state(envelope);
        priorState.put("stateVersion", current + 1);
        priorState.put("expectedStateVersion", current + 1);
        priorState.put("stopConversation", true);
        priorState.put("requiredAction", "SESSION_CANCELLED");
        priorState.put("completionReason", "CANCELLED_BY_USER");
        TriageSessionResponse response = response(session.getId(), priorState, fallbackReadiness());
        persist(session, priorState, response, "CANCELLED", null,
                current + 1, IntakeStatus.FAILED);
        return response;
    }

    private TriageSessionResponse executeAndPersist(
            IntakeSession session, int currentVersion, String requestId, String mutationFingerprint,
            String messageId,
            String message, UUID profileId, String selectedTarget, Map<String, Object> journeyContext,
            Map<String, Object> previousState,
            Map<String, Object> signals, Map<String, Object> measurements,
            List<String> answeredQuestionIds, List<String> submittedOptionQuestionIds,
            List<String> submittedOptionCodes, UUID userId) {
        long started = System.nanoTime();
        String expectedHash = readinessService.registry().map(TriageRuleRegistry::rulesetSha256).orElse(null);
        if (!readinessService.isReady() || expectedHash == null) {
            return persistFallback(session, currentVersion, requestId, mutationFingerprint,
                    signals, message);
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
        payload.put("submittedOptionQuestionIds",
                submittedOptionQuestionIds == null ? List.of() : submittedOptionQuestionIds);
        payload.put("submittedOptionCodes",
                submittedOptionCodes == null ? List.of() : submittedOptionCodes);
        payload.put("expectedRulesetHash", expectedHash);
        TriageWorkflowClient.WorkflowResult result;
        try {
            result = workflowClient.executeTurn(payload);
        } catch (RuntimeException unavailable) {
            return persistFallback(session, currentVersion, requestId, mutationFingerprint,
                    signals, message);
        }
        try {
            if (!"READY".equals(result.readiness()) || !expectedHash.equals(result.rulesetHash())) {
                metrics.recordFailure(TriageMetrics.Failure.HASH_MISMATCH);
                throw new IllegalStateException("Triage ruleset handshake failed");
            }
            Map<String, Object> newState = new LinkedHashMap<>(result.state());
            validateWorkflowState(session, currentVersion, expectedHash, newState);
            newState.put("signals", structuredSignals(objectMap(newState.get("signals"))));
            newState.put("measurements", structuredMeasurements(objectMap(newState.get("measurements"))));
            try {
                Object rawCitations = newState.get("citations");
                int submittedCitationCount = rawCitations instanceof List<?> list ? list.size() : 0;
                List<Map<String, Object>> verified = verifiedCitations(
                        rawCitations, strings(newState.get("decisiveRuleIds")),
                        text(newState.get("stage")));
                newState.put("citations", verified);
                if ((rawCitations != null && !(rawCitations instanceof List<?>))
                        || verified.size() < submittedCitationCount) {
                    List<String> rejections = new ArrayList<>(
                            strings(newState.get("evidenceRejections")));
                    if (!rejections.contains("CITATION_REJECTED")) {
                        rejections.add("CITATION_REJECTED");
                    }
                    newState.put("evidenceRejections", List.copyOf(rejections));
                }
                newState.put("evidenceStatus", publicEvidenceStatus(newState, verified));
            } catch (RuntimeException rejectedCitation) {
                // Evidence is optional and post-outcome. A broken/unverified citation must
                // disappear, but it must never erase or downgrade a valid RED/YELLOW result.
                if (!(rejectedCitation instanceof TriageException)) {
                    metrics.recordFailure(TriageMetrics.Failure.CITATION_REJECTED);
                }
                newState.put("citations", List.of());
            }
            newState.put("stateVersion", currentVersion + 1);
            newState.put("expectedStateVersion", currentVersion + 1);
            TriageSessionResponse response = response(session.getId(), newState,
                    readinessService.statusReport());
            persist(session, sanitizeState(newState), response, requestId, mutationFingerprint,
                    currentVersion + 1,
                    Boolean.TRUE.equals(newState.get("stopConversation"))
                            ? IntakeStatus.COMPLETED : IntakeStatus.NEED_MORE_INFO);
            metrics.recordTurn(text(newState.get("triageOutcome")),
                    (System.nanoTime() - started) / 1_000_000,
                    strings(newState.get("plannedQuestionIds")).size(),
                    "CONFLICTED".equals(text(newState.get("targetEntity"))));
            return response;
        } catch (TriageException validationFailure) {
            throw validationFailure;
        } catch (IllegalArgumentException | IllegalStateException validationFailure) {
            throw new TriageException(HttpStatus.SERVICE_UNAVAILABLE,
                    "TRIAGE_INVALID_WORKFLOW_RESPONSE",
                    "Triage workflow returned an invalid response");
        }
    }

    private TriageSessionResponse persistFallback(
            IntakeSession session, int currentVersion, String requestId, String mutationFingerprint,
            Map<String, Object> signals, String message) {
        IndependentGlobalSafetyFallback.FallbackVerdict verdict =
                fallback.screenWithLatestMessage(structuredSignals(signals), message);
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
        state.put("rationale", "RED".equals(verdict.outcome())
                ? "Kết quả Đỏ được xác định từ dấu hiệu nguy hiểm đã ghi nhận; bạn cần được nhân viên y tế đánh giá ngay."
                : "Hiện chưa đủ dữ kiện để đưa ra định hướng an toàn.");
        state.put("evidenceStatus", "RED".equals(verdict.outcome()) ? "PENDING" : "UNAVAILABLE");
        state.put("citations", List.of());
        state.put("readingLinks", List.of());
        TriageSessionResponse response = response(session.getId(), state, fallbackReadiness());
        persist(session, state, response, requestId, mutationFingerprint, currentVersion + 1,
                "RED".equals(verdict.outcome()) ? IntakeStatus.COMPLETED
                        : verdict.stopConversation() ? IntakeStatus.FAILED : IntakeStatus.NEED_MORE_INFO);
        metrics.recordFailure(TriageMetrics.Failure.FALLBACK);
        metrics.recordTurn(verdict.outcome(), 0, 0, false);
        return response;
    }

    private void persist(IntakeSession session, Map<String, Object> state,
                         TriageSessionResponse response, String requestId,
                         String mutationFingerprint, int version,
                         IntakeStatus status) {
        boolean wasCompleted = session.getStatus() == IntakeStatus.COMPLETED;
        Map<String, Object> envelope = new LinkedHashMap<>();
        String requestFingerprint = persistedRequestFingerprint(session);
        envelope.put("contract", SCHEMA_VERSION);
        envelope.put("requestFingerprint", requestFingerprint);
        envelope.put("stateVersion", version);
        envelope.put("lastRequestId", requestId);
        if (mutationFingerprint != null) {
            envelope.put("lastRequestFingerprint", mutationFingerprint);
        }
        envelope.put("retentionUntil", Instant.now().plus(retentionDays, ChronoUnit.DAYS).toString());
        envelope.put("triageState", state);
        envelope.put("publicResponse", objectMapper.convertValue(response, new TypeReference<Map<String, Object>>() {}));
        try {
            String json = objectMapper.writeValueAsString(envelope);
            session.setResultJson(json);
            session.setRawAiResponse(null);
            session.setStatus(status);
            session.setRiskLevel(risk(response.outcome()));
            session.setEmergency("RED".equals(response.outcome()));
            TriageStage resolvedStage = legacyStage(response.stage());
            if (resolvedStage != null) {
                session.setStage(resolvedStage);
            }
            session.setCompletedAt(status == IntakeStatus.COMPLETED || status == IntakeStatus.FAILED
                    ? Instant.now() : null);
            if (status == IntakeStatus.COMPLETED && session.getOriginDashboard() != null) {
                if (lifecycleBindingService == null) {
                    throw new IllegalStateException("Lifecycle intake binding service is unavailable");
                }
                lifecycleBindingService.renewForTerminal(session);
            }
            session.setSchemaVersion(SCHEMA_VERSION);
            session.setContentHash(sha256(json));
            repository.save(session);
            if (status == IntakeStatus.COMPLETED && !wasCompleted && session.getRiskLevel() != null) {
                publishCompletionEvents(session);
            }
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("Could not persist triage state", failure);
        }
    }

    private TriageSessionResponse requireV2Replay(IntakeSession session, String requestFingerprint) {
        if (!SCHEMA_VERSION.equals(session.getSchemaVersion())) {
            throw conflict("TRIAGE_IDEMPOTENCY_KEY_CONFLICT");
        }
        Map<String, Object> stored = envelope(session);
        Object storedFingerprint = stored.get("requestFingerprint");
        if (!(storedFingerprint instanceof String value) || !value.equals(requestFingerprint)) {
            throw conflict("TRIAGE_IDEMPOTENCY_KEY_CONFLICT");
        }
        return publicResponse(stored);
    }

    private void publishCompletionEvents(IntakeSession session) {
        if (session.getRiskLevel() == RiskLevel.RED) {
            eventPublisher.publishEvent(new EmergencyEscalationTriggered(
                    UUID.randomUUID(), session.getId(), session.getUserId(),
                    "AUTO_TRIAGE", session.getCompletedAt()));
        }
        eventPublisher.publishEvent(new IntakeSessionCompleted(
                UUID.randomUUID(), session.getId(), session.getUserId(),
                session.getRiskLevel(), session.getCompletedAt()));
    }

    /** Maps the canonical stage to the still-public legacy persistence vocabulary. */
    private static TriageStage legacyStage(String canonicalStage) {
        if (canonicalStage == null) return null;
        return switch (canonicalStage) {
            case "PRECONCEPTION" -> TriageStage.PRECONCEPTION;
            case "PREGNANCY" -> TriageStage.PREGNANCY;
            case "POSTPARTUM_MOTHER" -> TriageStage.POSTPARTUM;
            case "INFANT_0_12M" -> TriageStage.INFANT;
            case "TODDLER_12_24M" -> TriageStage.TODDLER;
            // No safe legacy representation exists. Keep the canonical value in result_jsonb;
            // callers must read it from the version-aware projection instead of guessing.
            case "POSSIBLE_PREGNANCY", "UNKNOWN", "CONFLICTED" -> null;
            default -> null;
        };
    }

    private String persistedRequestFingerprint(IntakeSession session) {
        if (session.getResultJson() != null && !session.getResultJson().isBlank()) {
            try {
                Map<String, Object> previous = objectMapper.readValue(
                        session.getResultJson(), new TypeReference<>() {});
                Object value = previous.get("requestFingerprint");
                if (value instanceof String fingerprint && !fingerprint.isBlank()) return fingerprint;
            } catch (Exception ignored) {
                // The strict state parser will reject a corrupt envelope at its public boundary.
            }
        }
        if (session.getContentHash() == null || session.getContentHash().isBlank()) {
            throw new IllegalStateException("Missing triage request fingerprint");
        }
        return session.getContentHash();
    }

    private String startFingerprint(TriageSessionStartRequest request) {
        try {
            Map<String, Object> canonical = new LinkedHashMap<>();
            canonical.put("profileId", request.profileId());
            canonical.put("selectedTarget", request.selectedTarget());
            canonical.put("selectedStage", request.selectedStage());
            canonical.put("journeyContext", request.journeyContext());
            canonical.put("message", request.message());
            canonical.put("messageId", request.messageId());
            canonical.put("consentContext", request.consentContext());
            canonical.put("journeyId", request.journeyId());
            canonical.put("originDashboard", request.originDashboard());
            canonical.put("originReferenceId", request.originReferenceId());
            return sha256(objectMapper.writeValueAsString(canonical));
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("Could not fingerprint triage start request", failure);
        }
    }

    private LifecycleBinding bindLifecycle(TriageSessionStartRequest request, UUID userId) {
        boolean supplied = request.journeyId() != null || request.originDashboard() != null
                || request.originReferenceId() != null;
        if (!supplied) return null;
        TriageStage stage = legacyStage(request.selectedStage());
        if (stage == null || lifecycleBindingService == null) {
            throw new TriageException(HttpStatus.BAD_REQUEST, "TRIAGE-012",
                    "Lifecycle intake requires a supported canonical stage");
        }
        StartIntakeConversationRequest legacyBoundary = StartIntakeConversationRequest.builder()
                .clientRequestId(request.requestId())
                .stage(stage)
                .currentIntake(Map.of("stage", stage.name()))
                .babyProfileId("BABY".equals(request.selectedTarget()) ? request.profileId() : null)
                .motherProfileId("MOTHER".equals(request.selectedTarget()) ? request.profileId() : null)
                .journeyId(request.journeyId())
                .originDashboard(request.originDashboard())
                .originReferenceId(request.originReferenceId())
                .build();
        return lifecycleBindingService.bindForStart(legacyBoundary, stage, userId);
    }

    private void validateLifecycleReplay(IntakeSession session, LifecycleBinding lifecycle) {
        if (lifecycle == null && session.getOriginDashboard() == null) return;
        if (lifecycle == null || lifecycleBindingService == null) {
            throw conflict("TRIAGE_IDEMPOTENCY_KEY_CONFLICT");
        }
        lifecycleBindingService.validateReplay(session, lifecycle);
    }

    private String continueFingerprint(TriageSessionContinueRequest request) {
        try {
            Map<String, Object> canonical = new LinkedHashMap<>();
            canonical.put("sessionId", request.sessionId());
            canonical.put("expectedStateVersion", request.expectedStateVersion());
            canonical.put("message", request.message());
            canonical.put("messageId", request.messageId());
            canonical.put("answers", request.answers());
            canonical.put("signals", request.signals());
            canonical.put("measurements", request.measurements());
            return sha256(objectMapper.writeValueAsString(canonical));
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("Could not fingerprint triage continuation", failure);
        }
    }

    private TriageSessionResponse requireContinueReplay(
            Map<String, Object> envelope, String mutationFingerprint) {
        Object stored = envelope.get("lastRequestFingerprint");
        if (!(stored instanceof String value) || !value.equals(mutationFingerprint)) {
            throw conflict("TRIAGE_IDEMPOTENCY_KEY_CONFLICT");
        }
        return publicResponse(envelope);
    }

    private IntakeSession locked(UUID id, UUID userId) {
        IntakeSession session = repository.findForUpdateByIdAndUserId(id, userId)
                .orElseThrow(() -> notFound("TRIAGE_SESSION_NOT_FOUND"));
        if (!SCHEMA_VERSION.equals(session.getSchemaVersion())) {
            throw notFound("TRIAGE_SESSION_NOT_FOUND");
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
                    "TRIAGE_STATE_UNAVAILABLE", "Triage state is unavailable");
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> state(Map<String, Object> envelope) {
        Object value = envelope.get("triageState");
        if (!(value instanceof Map<?, ?>)) {
            // Compatibility reader for sessions persisted before the single-engine cutover.
            value = envelope.get("v2State");
        }
        if (!(value instanceof Map<?, ?>)) throw new IllegalStateException("Missing triage state");
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

    private TriageSessionResponse publicResponse(Map<String, Object> envelope) {
        Map<String, Object> persistedState = state(envelope);
        UUID sessionId;
        try {
            sessionId = UUID.fromString(text(persistedState.get("sessionId")));
        } catch (IllegalArgumentException invalid) {
            throw new TriageException(HttpStatus.SERVICE_UNAVAILABLE,
                    "TRIAGE_STATE_UNAVAILABLE", "Triage state is unavailable");
        }
        Map<String, Object> readiness = readinessService.isReady()
                ? readinessService.statusReport() : fallbackReadiness();
        // Re-project from the persisted canonical state on every GET/replay. Never trust a stale
        // publicResponse snapshot created before the current citation and disposition guards.
        return response(sessionId, persistedState, readiness);
    }

    private TriageSessionResponse response(UUID id, Map<String, Object> state,
                                             Map<String, Object> readiness) {
        List<String> plannedQuestionIds = strings(state.get("plannedQuestionIds"));
        List<Map<String, Object>> citations = verifiedCitations(
                state.get("citations"), strings(state.get("decisiveRuleIds")),
                text(state.get("stage")));
        return new TriageSessionResponse(id, integer(state.get("stateVersion"), 0),
                text(state.get("targetEntity")), text(state.get("intent")), text(state.get("stage")),
                text(state.get("triageOutcome")), text(state.get("requiredAction")),
                Boolean.TRUE.equals(state.get("stopConversation")), plannedQuestionIds,
                publicQuestions(plannedQuestionIds),
                text(state.get("scopeStatus")), strings(state.get("pendingRiskStatuses")),
                text(state.get("completionReason")), text(state.get("rulesetVersion")),
                text(state.get("rulesetHash")), publicRationale(state),
                publicEvidenceStatus(state, citations), citations,
                disclaimerPolicy.disclaimerText(),
                new LinkedHashMap<>(readiness));
    }

    private List<TriageQuestionResponse> publicQuestions(List<String> questionIds) {
        return questionIds.stream().map(questionId -> {
            QuestionCatalog.Question question = questionCatalog.question(questionId)
                    .orElseThrow(() -> new IllegalStateException(
                            "workflow planned an unknown canonical question"));
            return new TriageQuestionResponse(
                    question.questionId(), question.text(), question.answerType(),
                    question.options().stream()
                            .map(option -> new TriageQuestionOptionResponse(
                                    option.optionCode(), option.displayText()))
                            .toList());
        }).toList();
    }

    private List<Map<String, Object>> verifiedCitations(
            Object value, List<String> decisiveRuleIds, String stage) {
        if (value == null) return List.of();
        if (!(value instanceof List<?> list)) {
            citationRejected();
            return List.of();
        }
        List<?> candidates = list;
        if (list.size() > 4) {
            // The response contract caps public evidence at four. Reject only the overflow,
            // preserving independently valid items already inside the bounded prefix.
            citationRejected();
            candidates = list.subList(0, 4);
        }
        List<Map<String, Object>> safe = new ArrayList<>();
        int whoCount = 0;
        Set<String> allowed = Set.of("sourceId", "title", "organization", "publisher", "url",
                "domain", "section", "contentHash", "sourceStatus", "retrievalMode", "ruleIds");
        for (Object item : candidates) {
            try {
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
            List<String> ruleIds = strings(map.get("ruleIds"));
            List<String> matchedRuleIds = ruleIds.stream()
                    .filter(decisiveRuleIds::contains)
                    .filter(id -> isApplicableRule(id, stage))
                    .distinct()
                    .toList();
            if (ruleIds.size() > 32 || ruleIds.stream().anyMatch(id -> id.length() > 80)
                    || decisiveRuleIds.isEmpty() || matchedRuleIds.isEmpty()) {
                throw citationRejected();
            }
            boolean who = domain.equals("who.int") || domain.endsWith(".who.int")
                    || (organization + " " + publisher).toLowerCase(java.util.Locale.ROOT)
                    .contains("world health organization");
            if (who && ++whoCount > 1) throw citationRejected();
            Map<String, Object> citation = new LinkedHashMap<>();
            citation.put("sourceId", sourceId); citation.put("title", title);
            citation.put("organization", organization); citation.put("publisher", publisher);
            citation.put("url", url); citation.put("domain", domain); citation.put("section", section);
            citation.put("contentHash", hash); citation.put("sourceStatus", "SOURCE_VERIFIED");
            citation.put("retrievalMode", "LOCAL_BM25");
            // Expose only the rule mappings that actually support this disposition. A source may
            // cover more rules in the corpus, but those unrelated IDs must not be attributed here.
            citation.put("ruleIds", matchedRuleIds);
            safe.add(citation);
            } catch (TriageException rejected) {
                // Evidence is post-outcome and item-scoped. Keep every independently valid
                // citation; one malformed or rule-mismatched item cannot erase the others.
            }
        }
        return List.copyOf(safe);
    }

    private boolean isApplicableRule(String ruleId, String stage) {
        return readinessService.registry().map(registry ->
                registry.byId(ruleId).map(rule -> rule.appliesToStage(stage)).orElseGet(() ->
                        registry.safetyPolicies().stream().anyMatch(policy ->
                                policy.enabled() && policy.policyId().equals(ruleId)
                                        && policy.appliesToStage(stage))))
                .orElse(false);
    }

    private static String publicRationale(Map<String, Object> state) {
        if (state.get("rationale") instanceof String rationale && !rationale.isBlank()) {
            return rationale;
        }
        return switch (text(state.get("triageOutcome"))) {
            case "RED" -> "Kết quả Đỏ được xác định từ dấu hiệu nguy hiểm đã ghi nhận; bạn cần được nhân viên y tế đánh giá ngay.";
            case "YELLOW" -> "Kết quả Vàng được xác định từ các dữ kiện đã cung cấp; bạn nên được nhân viên y tế đánh giá sớm.";
            case "OUT_OF_SCOPE" -> "Nội dung hiện tại nằm ngoài phạm vi định hướng của công cụ.";
            default -> "Hiện chưa đủ dữ kiện để đưa ra định hướng an toàn.";
        };
    }

    private static String publicEvidenceStatus(
            Map<String, Object> state, List<Map<String, Object>> citations) {
        if (!citations.isEmpty()) return "AVAILABLE";
        String status = state.get("evidenceStatus") instanceof String value ? value : "";
        if (Set.of("PENDING", "UNAVAILABLE", "REJECTED").contains(status)) return status;
        return "AVAILABLE".equals(status) ? "REJECTED" : "UNAVAILABLE";
    }

    private TriageException citationRejected() {
        metrics.recordFailure(TriageMetrics.Failure.CITATION_REJECTED);
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
                || !isValidSafetyDisposition(state)
                || strings(state.get("plannedQuestionIds")).size() > questionCatalog.maxQuestionsPerTurn()
                || strings(state.get("plannedQuestionIds")).stream()
                        .anyMatch(id -> questionCatalog.question(id).isEmpty())
                || (!"NEEDS_MORE_INFO".equals(state.get("triageOutcome"))
                        && !strings(state.get("plannedQuestionIds")).isEmpty())) {
            throw new IllegalStateException("Invalid triage workflow state");
        }
        try {
            if (objectMapper.writeValueAsBytes(state).length > 65_536) {
                throw new IllegalStateException("Triage workflow state exceeds persistence limit");
            }
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("Invalid triage workflow state", failure);
        }
    }

    private static boolean isAllowedOutcome(Object value) {
        return value instanceof String outcome && OUTCOMES.contains(outcome);
    }

    private static boolean isValidSafetyDisposition(Map<String, Object> state) {
        if (!"RED".equals(state.get("triageOutcome"))) return true;
        return Boolean.TRUE.equals(state.get("stopConversation"))
                && EMERGENCY_ACTIONS.contains(state.get("requiredAction"));
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
                    "TRIAGE_UNTRUSTED_CLINICAL_STATE",
                    "Clinical state must be derived by the server from canonical answers");
        }
    }

    private void validateSelectedProfile(UUID profileId, String selectedTarget, UUID userId) {
        if (profileId == null) return;
        if ("MOTHER".equals(selectedTarget) && profileId.equals(userId)) return;
        if ("BABY".equals(selectedTarget)
                && babyProfileRepository.findByIdAndOwnerUserId(profileId, userId).isPresent()) return;
        throw new TriageException(HttpStatus.FORBIDDEN, "TRIAGE_PROFILE_FORBIDDEN",
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

    private static Map<String, Object> selectedStageContext(String selectedStage) {
        if (selectedStage == null || selectedStage.isBlank()) return Map.of();
        if (!CARE_STAGES.contains(selectedStage)
                || "UNKNOWN".equals(selectedStage)
                || "CONFLICTED".equals(selectedStage)) {
            throw invalidStructuredPayload();
        }
        return Map.of("stage", selectedStage);
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
        return new TriageException(HttpStatus.BAD_REQUEST, "TRIAGE_PAYLOAD_INVALID",
                "Structured triage payload is invalid");
    }

    private void requireEnabled() {
        if (!enabled) throw notFound("TRIAGE_INTERNAL_ONLY");
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
        return new TriageException(HttpStatus.CONFLICT, code, "Triage request conflict");
    }

    private static TriageException notFound(String code) {
        return new TriageException(HttpStatus.NOT_FOUND, code, "Triage session not found");
    }
}
