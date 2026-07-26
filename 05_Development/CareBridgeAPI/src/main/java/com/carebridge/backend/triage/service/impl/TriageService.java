package com.carebridge.backend.triage.service.impl;

import com.carebridge.backend.ai.event.EmergencyEscalationTriggered;
import com.carebridge.backend.triage.IntakeStatus;
import com.carebridge.backend.triage.RiskLevel;
import com.carebridge.backend.triage.TriageStage;
import com.carebridge.backend.journey.service.LifecycleConsentValidator;
import com.carebridge.backend.triage.TriageRecommendationCode;
import com.carebridge.backend.triage.dto.HealthMemoryContextItem;
import com.carebridge.backend.triage.dto.request.RunIntakeRequest;
import com.carebridge.backend.triage.dto.request.StartIntakeConversationRequest;
import com.carebridge.backend.triage.dto.request.ContinueIntakeConversationRequest;
import com.carebridge.backend.triage.dto.response.IntakeConversationResponse;
import com.carebridge.backend.triage.dto.response.IntakeSessionResponse;
import com.carebridge.backend.triage.dto.response.TriageResultResponse;
import com.carebridge.backend.triage.entity.EvidenceSource;
import com.carebridge.backend.triage.entity.EvidenceSourceReviewLog;
import com.carebridge.backend.triage.entity.IntakeSession;
import com.carebridge.backend.triage.engine.ChildTriageResult;
import com.carebridge.backend.triage.engine.TriageGraphService;
import com.carebridge.backend.triage.event.IntakeSessionCompleted;
import com.carebridge.backend.triage.event.IntakeSessionFailed;
import com.carebridge.backend.triage.exception.TriageException;
import com.carebridge.backend.triage.policy.PreScreenOutcome;
import com.carebridge.backend.triage.policy.PreScreenResult;
import com.carebridge.backend.triage.policy.TriageRedFlagPreScreenPolicy;
import com.carebridge.backend.triage.repository.IIntakeSessionRepository;
import com.carebridge.backend.triage.repository.IntakeSessionWriter;
import com.carebridge.backend.triage.repository.TriageSessionEvidenceWriter;
import com.carebridge.backend.triage.service.ChildTriageAiClient;
import com.carebridge.backend.triage.service.HealthMemoryService;
import com.carebridge.backend.triage.service.LifecycleBinding;
import com.carebridge.backend.triage.service.LifecycleIntakeBindingService;
import com.carebridge.backend.triage.service.EvidenceSourceService;
import com.carebridge.backend.triage.service.ITriageService;
import com.carebridge.backend.triage.service.TriagePreScreenMetrics;
import com.carebridge.backend.triage.service.TriageFallbackMetrics;
import com.carebridge.backend.triage.service.TriageStageLegacyDefaultMetrics;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.net.URI;
import java.net.http.HttpTimeoutException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
@Transactional
public class TriageService implements ITriageService {

    private static final Logger log = LoggerFactory.getLogger(TriageService.class);
    private static final Set<String> CONVERSATION_RESPONSE_FIELDS = Set.of(
            "status",
            "intakeSessionId",
            "stage",
            "mergedIntake",
            "normalizedSymptomDetails",
            "assistantMessage",
            "questions",
            "round",
            "triageResult");
    private static final Set<String> CONVERSATION_RESPONSE_METADATA_FIELDS = Set.of(
            "assistantProvider",
            "assistantFallbackUsed",
            "conversationSummary");

    private final IIntakeSessionRepository intakeSessionRepository;
    private final ChildTriageAiClient childTriageAiClient;
    private final TriageGraphService triageGraphService;
    private final EvidenceSourceService evidenceSourceService;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final TriageFallbackMetrics triageFallbackMetrics;
    private final TriageStageLegacyDefaultMetrics triageStageLegacyDefaultMetrics;
    private final Consumer<UUID> postpartumEligibilityCheck;

    @Autowired(required = false)
    private LifecycleIntakeBindingService lifecycleBindingService;

    @Autowired
    private IntakeSessionWriter intakeSessionWriter;

    @Autowired(required = false)
    private TriageSessionEvidenceWriter triageSessionEvidenceWriter;

    // CB-TRIAGE-IMP-003 — red-flag pre-screen (C1). Optional wiring keeps every existing test
    // constructor byte-compatible: when absent the intake flows behave exactly pre-feature.
    @Autowired(required = false)
    private TriageRedFlagPreScreenPolicy preScreenPolicy;

    @Autowired(required = false)
    private TriagePreScreenMetrics preScreenMetrics;

    // CB-TRIAGE-THMC-IMP-001 — health-context memory injection (US-THMC-002). Optional wiring
    // keeps every existing test constructor byte-compatible: when absent the intake flows
    // behave exactly pre-feature (legacy one-arg AI/fallback calls, no context handling).
    @Autowired(required = false)
    private HealthMemoryService healthMemoryService;

    // CB-TRIAGE-CONSENT-IMP-001 (BR-TDC-004 / C3) — disclaimer consent gate for the TWO
    // elective entry points ONLY (runIntake, startConversation). Optional wiring keeps every
    // existing test constructor byte-compatible (sibling pattern: preScreenPolicy above); in
    // the real application context the @Service implementation is always present. NEVER call
    // this from continueConversation, continuations, or any emergency/escalation path.
    @Autowired(required = false)
    private com.carebridge.backend.triage.service.ITriageConsentService triageConsentService;

    // CB-TRIAGE-CONSENT-IMP-001 (ADR-TDC-003) — stamps triage_sessions.disclaimer_version at
    // session creation once the gate passes. Optional for the same constructor-compat reason.
    @Autowired(required = false)
    private com.carebridge.backend.triage.policy.TriageDisclaimerPolicy triageDisclaimerPolicy;

    @Autowired
    public TriageService(
            IIntakeSessionRepository intakeSessionRepository,
            ChildTriageAiClient childTriageAiClient,
            TriageGraphService triageGraphService,
            EvidenceSourceService evidenceSourceService,
            ObjectMapper objectMapper,
            ApplicationEventPublisher eventPublisher,
            TriageFallbackMetrics triageFallbackMetrics,
            TriageStageLegacyDefaultMetrics triageStageLegacyDefaultMetrics,
            LifecycleConsentValidator lifecycleConsentValidator) {
        this(intakeSessionRepository, childTriageAiClient, triageGraphService,
                evidenceSourceService, objectMapper, eventPublisher, triageFallbackMetrics,
                triageStageLegacyDefaultMetrics, lifecycleConsentValidator::ensureEligibleForMutation);
    }

    public TriageService(
            IIntakeSessionRepository intakeSessionRepository,
            ChildTriageAiClient childTriageAiClient,
            TriageGraphService triageGraphService,
            EvidenceSourceService evidenceSourceService,
            ObjectMapper objectMapper,
            ApplicationEventPublisher eventPublisher,
            TriageFallbackMetrics triageFallbackMetrics,
            TriageStageLegacyDefaultMetrics triageStageLegacyDefaultMetrics) {
        this(intakeSessionRepository, childTriageAiClient, triageGraphService,
                evidenceSourceService, objectMapper, eventPublisher, triageFallbackMetrics,
                triageStageLegacyDefaultMetrics, missingPostpartumConsentValidator());
    }

    private TriageService(
            IIntakeSessionRepository intakeSessionRepository,
            ChildTriageAiClient childTriageAiClient,
            TriageGraphService triageGraphService,
            EvidenceSourceService evidenceSourceService,
            ObjectMapper objectMapper,
            ApplicationEventPublisher eventPublisher,
            TriageFallbackMetrics triageFallbackMetrics,
            TriageStageLegacyDefaultMetrics triageStageLegacyDefaultMetrics,
            Consumer<UUID> postpartumEligibilityCheck) {
        this.intakeSessionRepository = intakeSessionRepository;
        this.childTriageAiClient = childTriageAiClient;
        this.triageGraphService = triageGraphService;
        this.evidenceSourceService = evidenceSourceService;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
        this.triageFallbackMetrics = triageFallbackMetrics;
        this.triageStageLegacyDefaultMetrics = triageStageLegacyDefaultMetrics;
        this.postpartumEligibilityCheck = postpartumEligibilityCheck;
    }

    public TriageService(
            IIntakeSessionRepository intakeSessionRepository,
            ChildTriageAiClient childTriageAiClient,
            TriageGraphService triageGraphService,
            ObjectMapper objectMapper,
            ApplicationEventPublisher eventPublisher) {
        this(intakeSessionRepository, childTriageAiClient, triageGraphService,
                legacyEvidenceSourceService(), objectMapper, eventPublisher, new TriageFallbackMetrics(),
                new TriageStageLegacyDefaultMetrics());
    }

    public TriageService(
            IIntakeSessionRepository intakeSessionRepository,
            ChildTriageAiClient childTriageAiClient,
            TriageGraphService triageGraphService,
            ObjectMapper objectMapper,
            ApplicationEventPublisher eventPublisher,
            TriageFallbackMetrics triageFallbackMetrics) {
        this(intakeSessionRepository, childTriageAiClient, triageGraphService,
                legacyEvidenceSourceService(), objectMapper, eventPublisher, triageFallbackMetrics,
                new TriageStageLegacyDefaultMetrics());
    }

    public TriageService(
            IIntakeSessionRepository intakeSessionRepository,
            ChildTriageAiClient childTriageAiClient,
            TriageGraphService triageGraphService,
            ObjectMapper objectMapper,
            ApplicationEventPublisher eventPublisher,
            TriageFallbackMetrics triageFallbackMetrics,
            TriageStageLegacyDefaultMetrics triageStageLegacyDefaultMetrics) {
        this(intakeSessionRepository, childTriageAiClient, triageGraphService,
                legacyEvidenceSourceService(), objectMapper, eventPublisher, triageFallbackMetrics,
                triageStageLegacyDefaultMetrics);
    }

    public TriageService(
            IIntakeSessionRepository intakeSessionRepository,
            ChildTriageAiClient childTriageAiClient,
            TriageGraphService triageGraphService,
            ObjectMapper objectMapper,
            ApplicationEventPublisher eventPublisher,
            LifecycleConsentValidator lifecycleConsentValidator) {
        this(intakeSessionRepository, childTriageAiClient, triageGraphService,
                legacyEvidenceSourceService(), objectMapper, eventPublisher, new TriageFallbackMetrics(),
                new TriageStageLegacyDefaultMetrics(), lifecycleConsentValidator::ensureEligibleForMutation);
    }

    private static EvidenceSourceService legacyEvidenceSourceService() {
        return new EvidenceSourceService() {
            @Override
            public EvidenceSource propose(String baseUrl, String organization, String category, String applicableStages, String notes, UUID actorUserId) {
                throw new UnsupportedOperationException("Evidence source admin service is not available in this test constructor");
            }

            @Override
            public List<EvidenceSource> list(String status) {
                return List.of();
            }

            @Override
            public List<EvidenceSource> approvedForStage(String stage) {
                return List.of();
            }

            @Override
            public EvidenceSource changeStatus(UUID id, String newStatus, String notes, UUID actorUserId, String actorRole) {
                throw new UnsupportedOperationException("Evidence source admin service is not available in this test constructor");
            }

            @Override
            public List<EvidenceSourceReviewLog> reviewLog(UUID id) {
                return List.of();
            }

            @Override
            public boolean isApprovedDeepLink(URI uri) {
                String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase().replaceFirst("^www\\.", "");
                String path = uri.getPath() == null ? "" : uri.getPath().replace("/", "").trim();
                return "https".equalsIgnoreCase(uri.getScheme()) && !path.isBlank()
                        && java.util.Set.of("who.int", "moh.gov.vn", "mch.moh.gov.vn", "cdc.gov", "unicef.org",
                        "benhviennhitrunguong.gov.vn", "nhidong.org.vn", "bvndtp.org.vn")
                        .stream().anyMatch(domain -> host.equals(domain) || host.endsWith("." + domain));
            }
        };
    }

    private static Consumer<UUID> missingPostpartumConsentValidator() {
        return ignored -> {
            throw new IllegalStateException(
                    "LifecycleConsentValidator is required for POSTPARTUM triage");
        };
    }

    /**
     * CB-TRIAGE-CONSENT-IMP-001 (BR-TDC-004 / C3): elective-entry disclaimer gate. Throws
     * {@code TriageException(409, "TRIAGE_CONSENT_REQUIRED")} when no ACTIVE consent matches
     * the current disclaimer version. Called from {@code runIntake} and
     * {@code startConversation} ONLY — never from continueConversation, continuations, or any
     * emergency/escalation path (BR-SAFETY). When the optional collaborator is absent
     * (legacy unit-test constructors), behaviour is exactly pre-feature.
     */
    private void ensureDisclaimerConsent(UUID userId) {
        if (triageConsentService != null) {
            triageConsentService.ensureActiveConsent(userId);
        }
    }

    /**
     * CB-TRIAGE-CONSENT-IMP-001 (ADR-TDC-003): stamps the pre-existing baseline column
     * {@code triage_sessions.disclaimer_version} with the configured canonical version at
     * session creation (post-gate). No-op when the optional policy is absent.
     */
    private void stampDisclaimerVersion(IntakeSession session) {
        if (triageDisclaimerPolicy != null) {
            session.setDisclaimerVersion(triageDisclaimerPolicy.currentVersion());
        }
    }

    @Override
    public synchronized IntakeConversationResponse startConversation(StartIntakeConversationRequest request, UUID userId) {
        // CB-TRIAGE-CONSENT-IMP-001 C3: disclaimer consent gate — FIRST statement, before any
        // validation or persistence (TDC-TC-07: no session row may leak before rejection).
        ensureDisclaimerConsent(userId);
        validateBoundedPayload(request.getCurrentIntake(), "currentIntake");
        TriageStage stage = resolveStartStage(request.getStage(), request.getCurrentIntake(),
                request.getBabyProfileId(), request.getMotherProfileId(), userId);
        ensurePostpartumEligible(stage, userId);
        validateStageProfile(stage, request.getBabyProfileId(), request.getMotherProfileId(), false);
        LifecycleBinding requestedBinding = bindLifecycle(request, stage, userId);
        String clientRequestId = normalizeClientRequestId(request.getClientRequestId());
        IntakeSession existing = clientRequestId == null ? null : intakeSessionRepository
                .findByUserIdAndClientRequestId(userId, clientRequestId).orElse(null);
        if (existing != null && existing.getRawAiResponse() != null) {
            validateStartReplay(existing, requestedBinding, stage,
                    request.getBabyProfileId(), request.getMotherProfileId());
            return toConversationResponse(readJsonObject(existing.getRawAiResponse()), existing);
        }
        IntakeSession session = existing;
        if (session == null) {
            // CB-TRIAGE-FDBB-IMP-001 (ADR-TFBF-003): no pre-assigned id in the builder. The id
            // is @GeneratedValue; pre-assigning made repository.save() below take Spring Data's
            // detached-merge path and fail with StaleObjectStateException on real PostgreSQL.
            session = IntakeSession.builder()
                    .userId(userId)
                    .clientRequestId(clientRequestId)
                    .stage(stage)
                    .babyProfileId(stage.isPediatric() ? request.getBabyProfileId() : null)
                    .motherProfileId(stage.isMaternal() ? request.getMotherProfileId() : null)
                    .symptoms("CONVERSATION_INTAKE")
                    .status(IntakeStatus.PROCESSING)
                    .createdAt(Instant.now())
                    .createdBy(userId)
                    .build();
            stampDisclaimerVersion(session);   // ADR-TDC-003 — post-gate session stamping
            applyBinding(session, requestedBinding);
            boolean databaseArbitrated = clientRequestId != null && intakeSessionWriter != null;
            boolean created = true;
            if (databaseArbitrated) {
                // The DB-arbitrated native insert consumes a caller-supplied id
                // (IntakeSessionWriter binds candidate.getId()); JPA generation never
                // runs on this path, so assign it here — and only here.
                session.setId(UUID.randomUUID());
                created = intakeSessionWriter.insertConversationIfAbsent(session).created();
                session = intakeSessionRepository.findByUserIdAndClientRequestId(userId, clientRequestId)
                        .orElseThrow(() -> new IllegalStateException(
                                "Idempotency conflict winner was not visible"));
            } else {
                session = intakeSessionRepository.save(session);
            }
            if (!created) {
                validateStartReplay(session, requestedBinding, stage,
                        request.getBabyProfileId(), request.getMotherProfileId());
                if (session.getRawAiResponse() != null) {
                    return toConversationResponse(readJsonObject(session.getRawAiResponse()), session);
                }
            } else if (requestedBinding != null) {
                lifecycleBindingService.recordCreated();
            }
        } else {
            validateStartReplay(session, requestedBinding, stage,
                    request.getBabyProfileId(), request.getMotherProfileId());
        }
        Map<String, Object> canonicalRequest = new LinkedHashMap<>();
        canonicalRequest.put("initialText", request.getInitialText());
        canonicalRequest.put("currentIntake", withCanonicalStage(
                request.getCurrentIntake() == null ? Map.of() : request.getCurrentIntake(),
                session.getStage(), session.getBabyProfileId(), session.getMotherProfileId()));
        canonicalRequest.put("intakeSessionId", session.getId().toString());
        canonicalRequest.put("stage", session.getStage().name());
        // CB-TRIAGE-IMP-003 C1: pre-screen BEFORE the AI call (after all existing validations
        // and idempotency arbitration). ESCALATE_RED completes through the existing
        // persistConversationEnvelope path only (C2).
        PreScreenResult preScreen = preScreenPolicy == null ? null : preScreenPolicy.screen(
                preScreenText(request.getInitialText(), request.getCurrentIntake()));
        if (preScreen != null && preScreen.outcome() == PreScreenOutcome.ESCALATE_RED) {
            recordPreScreenShortCircuit("conversation_start", preScreen);
            Map<String, Object> redEnvelope = buildPreScreenRedEnvelope(
                    session, preScreen, 1, castToMap(canonicalRequest.get("currentIntake")));
            persistConversationEnvelope(session, redEnvelope, userId);
            return toConversationResponse(redEnvelope, session);
        }
        if (preScreen != null && preScreen.outcome() == PreScreenOutcome.ANNOTATE_ONLY) {
            recordPreScreenAnnotation("conversation_start", preScreen);
            // Additive key, tolerated by the Python contract (O1 verified: IntakeStartRequest
            // has no extra="forbid" — pydantic ignores unknown keys).
            canonicalRequest.put("preScreenFlags", preScreen.matchedKeywords());
        }
        // CB-TRIAGE-THMC-IMP-001 (US-THMC-002): server-assembled context, loaded only when
        // proceeding to AI/fallback (pre-screen RED short-circuit above never reads memories).
        // Additive canonical key — Python IntakeStartRequest treats it as optional (§9.2).
        List<HealthMemoryContextItem> healthContext = loadHealthContextFailOpen(
                userId, sessionStage(session), session.getBabyProfileId(), session.getMotherProfileId());
        if (healthContext != null && !healthContext.isEmpty()) {
            // Additive key only when there is real context — an absent key is contract-identical
            // to an empty list on the Python side (default_factory=list).
            canonicalRequest.put("healthContext", healthContextPayload(healthContext));
        }
        Map<String, Object> envelope;
        try {
            envelope = readJsonObject(childTriageAiClient.startIntake(canonicalRequest));
        } catch (Exception exception) {
            triageFallbackMetrics.record(fallbackReason(exception), "conversation_start");
            log.warn("AI triage conversation start unavailable; using Java fallback reason={}",
                    exception.getClass().getSimpleName());
            envelope = fallbackConversation(canonicalRequest, true);
        }
        envelope = ensureSafeEnvelope(envelope, canonicalRequest, true);
        envelope.put("intakeSessionId", session.getId().toString());
        envelope.put("stage", session.getStage().name());
        persistConversationEnvelope(session, envelope, userId);
        return toConversationResponse(envelope, session);
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
        TriageStage stage = requireCanonicalSessionStage(session);
        ensurePostpartumEligible(stage, userId);

        Map<String, Object> previous = readJsonObject(session.getRawAiResponse());
        Map<String, Object> normalizedAnswers = coerceConversationAnswers(request.getNewAnswers());
        Map<String, Object> canonical = new LinkedHashMap<>();
        canonical.put("intakeSessionId", session.getId().toString());
        canonical.put("currentIntake", withCanonicalStage(
                previous.getOrDefault("mergedIntake", Map.of()),
                stage, session.getBabyProfileId(), session.getMotherProfileId()));
        canonical.put("messages", List.of());
        canonical.put("newAnswers", normalizedAnswers);
        canonical.put("round", number(previous.get("round"), 1));
        canonical.put("stage", stage.name());

        if (session.getStatus() == IntakeStatus.COMPLETED) {
            if (answersAlreadyApplied(canonical)) {
                return toConversationResponse(previous, session);
            }
            throw new TriageException(HttpStatus.CONFLICT, "TRIAGE-008", "Intake session is already completed");
        }
        if (session.getStatus() == IntakeStatus.FAILED) {
            throw new TriageException(HttpStatus.CONFLICT, "TRIAGE-008", "Intake session has failed");
        }

        if (answersAlreadyApplied(canonical)) {
            return toConversationResponse(previous, session);
        }

        java.util.Set<String> allowedQuestionKeys = outstandingQuestionKeys(previous);
        normalizedAnswers.entrySet().removeIf(entry -> !allowedQuestionKeys.contains(entry.getKey()));
        if (normalizedAnswers.isEmpty()) {
            throw new TriageException(HttpStatus.BAD_REQUEST, "TRIAGE-010",
                    "newAnswers must answer a currently requested question");
        }

        // CB-TRIAGE-IMP-003 C1: pre-screen BEFORE the AI call, AFTER the TRIAGE-010 answer filter.
        PreScreenResult preScreen = preScreenPolicy == null ? null : preScreenPolicy.screen(
                preScreenText(null, normalizedAnswers, castToMap(canonical.get("currentIntake"))));
        if (preScreen != null && preScreen.outcome() == PreScreenOutcome.ESCALATE_RED) {
            recordPreScreenShortCircuit("conversation_continue", preScreen);
            Map<String, Object> merged = new LinkedHashMap<>(castToMap(canonical.get("currentIntake")));
            merged.putAll(normalizedAnswers);
            Map<String, Object> redEnvelope = buildPreScreenRedEnvelope(
                    session, preScreen, number(previous.get("round"), 1), merged);
            persistConversationEnvelope(session, redEnvelope, userId);
            return toConversationResponse(redEnvelope, session);
        }
        if (preScreen != null && preScreen.outcome() == PreScreenOutcome.ANNOTATE_ONLY) {
            recordPreScreenAnnotation("conversation_continue", preScreen);
            canonical.put("preScreenFlags", preScreen.matchedKeywords());
        }
        Map<String, Object> envelope;
        try {
            envelope = readJsonObject(childTriageAiClient.continueIntake(canonical));
        } catch (Exception exception) {
            triageFallbackMetrics.record(fallbackReason(exception), "conversation_continue");
            log.warn("AI triage conversation continue unavailable; using Java fallback reason={}",
                    exception.getClass().getSimpleName());
            envelope = fallbackConversation(canonical, false);
        }
        envelope = ensureSafeEnvelope(envelope, canonical, false);
        envelope.put("intakeSessionId", session.getId().toString());
        envelope.put("stage", stage.name());
        persistConversationEnvelope(session, envelope, userId);
        return toConversationResponse(envelope, session);
    }

    @Override
    public IntakeSessionResponse runIntake(RunIntakeRequest request, UUID userId) {
        // CB-TRIAGE-CONSENT-IMP-001 C3: disclaimer consent gate — FIRST statement, before any
        // validation or persistence (TDC-TC-06: no session row may leak before rejection).
        ensureDisclaimerConsent(userId);
        TriageStage stage = resolveStage(request.getStage(), Map.of(), request.getBabyProfileId(), request.getMotherProfileId());
        request.setStage(stage);
        ensurePostpartumEligible(stage, userId);
        validateStageProfile(stage, request.getBabyProfileId(), request.getMotherProfileId(), false);
        IntakeSession session = IntakeSession.builder()
                .userId(userId)
                .symptoms(snapshotRequest(request))
                .babyProfileId(request.getBabyProfileId())
                .motherProfileId(request.getMotherProfileId())
                .stage(stage)
                .status(IntakeStatus.PROCESSING)
                .createdAt(Instant.now())
                .createdBy(userId)
                .build();
        stampDisclaimerVersion(session);   // ADR-TDC-003 — post-gate session stamping
        session = intakeSessionRepository.save(session);
        log.info("Triage intake processing started flow=ONE_SHOT");

        try {
            String aiResponse;
            // CB-TRIAGE-IMP-003 C1: pre-screen BEFORE the AI call; on ESCALATE_RED the AI is
            // never invoked and the session completes through the existing statements below (C2).
            PreScreenResult preScreen = preScreenPolicy == null ? null : preScreenPolicy.screen(request);
            if (preScreen != null && preScreen.outcome() == PreScreenOutcome.ESCALATE_RED) {
                recordPreScreenShortCircuit("one_shot", preScreen);
                aiResponse = objectMapper.writeValueAsString(buildPreScreenRedResult(session, preScreen));
            } else {
                if (preScreen != null && preScreen.outcome() == PreScreenOutcome.ANNOTATE_ONLY) {
                    // One-shot annotation is metadata-only in v1 (ADR-002).
                    recordPreScreenAnnotation("one_shot", preScreen);
                }
                // CB-TRIAGE-THMC-IMP-001: memory context is loaded only when proceeding to
                // AI/fallback (pre-screen RED short-circuits above never read memories).
                List<HealthMemoryContextItem> healthContext = loadHealthContextFailOpen(
                        userId, stage, request.getBabyProfileId(), request.getMotherProfileId());
                aiResponse = triageWithAiServiceOrFallback(request, healthContext);
            }
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
            applyCanonicalSnapshot(session, aiResponse);
            session = intakeSessionRepository.save(session);

            if (session.getStatus() == IntakeStatus.COMPLETED && session.getRiskLevel() != null) {
                persistValidatedEvidence(session);
                publishCompletionEvents(session, userId);
            }

            log.info("Triage intake processing completed status={}", session.getStatus());
        } catch (Exception e) {
            log.warn("Triage intake processing failed reason={}", e.getClass().getSimpleName());
            session.setStatus(IntakeStatus.FAILED);
            intakeSessionRepository.save(session);
            eventPublisher.publishEvent(new IntakeSessionFailed(
                    UUID.randomUUID(), session.getId(), userId,
                    "Triage processing failed", Instant.now()));
            throw new TriageException(HttpStatus.SERVICE_UNAVAILABLE, "TRIAGE-005", "Triage processing failed");
        }

        return toResponse(session);
    }

    /**
     * CB-TRIAGE-THMC-IMP-001 (BR-THMC-004): an empty/absent healthContext preserves the
     * pre-feature one-arg calls byte-for-byte (an empty context list is wire-identical to
     * omitting the additive field — HttpChildTriageAiClient omits it either way, and the
     * legacy contract stays observable for pre-existing collaborator expectations). Only a
     * NON-EMPTY server-loaded context switches to the two-arg overloads, on BOTH engines.
     */
    private String triageWithAiServiceOrFallback(
            RunIntakeRequest request, List<HealthMemoryContextItem> healthContext)
            throws JsonProcessingException {
        boolean hasContext = healthContext != null && !healthContext.isEmpty();
        try {
            String response = hasContext
                    ? childTriageAiClient.triageChild(request, healthContext)
                    : childTriageAiClient.triageChild(request);
            return validateAndCanonicalizeOneShotResponse(response, request.getStage());
        } catch (Exception e) {
            triageFallbackMetrics.record(fallbackReason(e), "one_shot");
            log.warn("AI triage service unavailable or unsafe; using Java fallback reason={}",
                    e.getClass().getSimpleName());
            // Same context on the fallback path (THMC-TC-11) — no context loss on degradation
            ChildTriageResult graphResult = hasContext
                    ? triageGraphService.run(request, healthContext)
                    : triageGraphService.run(request);
            Map<String, Object> result = objectMapper.convertValue(
                    graphResult, new TypeReference<Map<String, Object>>() {});
            addJavaFallbackMetadata(result);
            result.put("stage", request.getStage().name());
            return objectMapper.writeValueAsString(result);
        }
    }

    private String validateAndCanonicalizeOneShotResponse(String response, TriageStage canonicalStage)
            throws JsonProcessingException {
        Map<String, Object> result = readJsonObject(response);
        requireNoExplicitStageMismatch(result.get("stage"), canonicalStage, "result");

        String risk = nonBlank(result.get("riskLevel"));
        String status = nonBlank(result.get("status"));
        if (status == null) {
            status = "NEED_MORE_INFO".equals(risk) ? "NEED_MORE_INFO" : "COMPLETED";
        }
        boolean needsMore = "NEED_MORE_INFO".equals(status);
        boolean validNeedMore = needsMore && (risk == null || "NEED_MORE_INFO".equals(risk));
        boolean validCompleted = "COMPLETED".equals(status) && isPersistableRiskLevel(risk);
        if (!validNeedMore && !validCompleted) {
            throw new IllegalStateException("AI triage returned invalid status/risk contract");
        }
        if ("RED".equals(risk) && !hasCanonicalRedContract(result)) {
            throw new IllegalStateException("AI triage returned unsafe RED contract");
        }

        result.put("status", status);
        result.put("stage", canonicalStage.name());
        return objectMapper.writeValueAsString(result);
    }

    private TriageFallbackMetrics.Reason fallbackReason(Exception exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof HttpTimeoutException || cause instanceof java.util.concurrent.TimeoutException) {
                return TriageFallbackMetrics.Reason.TIMEOUT;
            }
            if (cause instanceof IOException) {
                return TriageFallbackMetrics.Reason.NETWORK_ERROR;
            }
        }
        String message = String.valueOf(exception.getMessage()).toLowerCase(java.util.Locale.ROOT);
        if (message.contains("http 5")) {
            return TriageFallbackMetrics.Reason.PYTHON_5XX;
        }
        if (message.contains("invalid risk contract") || exception instanceof JsonProcessingException) {
            return TriageFallbackMetrics.Reason.MALFORMED_RESPONSE;
        }
        return TriageFallbackMetrics.Reason.OTHER;
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

        boolean exposeContinuation = hasActiveTerminalContinuation(session);
        return TriageResultResponse.builder()
                .sessionId(session.getId())
                .stage(sessionStage(session).name())
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
                .claims(readValidatedClaims(session, citations))
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
                .journeyId(session.getJourneyId())
                .originDashboard(session.getOriginDashboard() == null ? null : session.getOriginDashboard().name())
                .originReferenceId(session.getOriginReferenceId())
                .continuationToken(exposeContinuation ? session.getContinuationToken() : null)
                .continuationExpiresAt(exposeContinuation ? session.getContinuationExpiresAt() : null)
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
        boolean exposeContinuation = hasActiveTerminalContinuation(session);
        return IntakeSessionResponse.builder()
                .sessionId(session.getId())
                .stage(sessionStage(session).name())
                .status(session.getStatus().name())
                .riskLevel(session.getRiskLevel() != null ? session.getRiskLevel().name() : null)
                .disclaimer(session.getDisclaimer())
                .createdAt(session.getCreatedAt())
                .completedAt(session.getCompletedAt())
                .journeyId(session.getJourneyId())
                .originDashboard(session.getOriginDashboard() == null ? null : session.getOriginDashboard().name())
                .originReferenceId(session.getOriginReferenceId())
                .continuationToken(exposeContinuation ? session.getContinuationToken() : null)
                .continuationExpiresAt(exposeContinuation ? session.getContinuationExpiresAt() : null)
                .build();
    }

    private boolean hasActiveTerminalContinuation(IntakeSession session) {
        return session.getStatus() == IntakeStatus.COMPLETED
                && session.getRiskLevel() != null
                && session.getContinuationToken() != null
                && session.getContinuationAcknowledgedAt() == null
                && session.getContinuationExpiresAt() != null
                && session.getContinuationExpiresAt().isAfter(Instant.now());
    }

    /**
     * Sessions created before the multi-stage migration have no persisted stage.
     * They are pediatric sessions, so preserve the legacy contract as INFANT.
     */
    private TriageStage sessionStage(IntakeSession session) {
        return session.getStage() == null ? TriageStage.INFANT : session.getStage();
    }

    private String snapshotRequest(RunIntakeRequest request) {
        try {
            Map<String, Object> replaySafe = new LinkedHashMap<>();
            replaySafe.put("stage", request.getStage() == null ? TriageStage.INFANT.name() : request.getStage().name());
            replaySafe.put("babyProfileId", request.getBabyProfileId());
            replaySafe.put("motherProfileId", request.getMotherProfileId());
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
        sanitizeEnvelope(envelope, requireCanonicalSessionStage(session));
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
        if (complete && session.getJourneyId() != null) {
            if (lifecycleBindingService == null) {
                throw new IllegalStateException("Lifecycle intake binding service is unavailable");
            }
            lifecycleBindingService.renewForTerminal(session);
        }
        session.setStatus(complete ? IntakeStatus.COMPLETED : IntakeStatus.NEED_MORE_INFO);
        session.setCompletedAt(complete ? Instant.now() : null);
        applyCanonicalSnapshot(session, session.getRawAiResponse());
        intakeSessionRepository.save(session);
        if (complete && session.getRiskLevel() != null) {
            persistValidatedEvidence(session);
            publishCompletionEvents(session, userId);
        }
    }

    private void applyCanonicalSnapshot(IntakeSession session, String responseJson) {
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            String canonical = objectMapper.writeValueAsString(root);
            JsonNode result = root.path("triageResult").isObject()
                    ? root.path("triageResult") : root;
            String responseSchemaVersion = result.path("responseSchemaVersion").asText(null);
            session.setResultJson(canonical);
            session.setSchemaVersion(responseSchemaVersion == null
                    || responseSchemaVersion.isBlank() ? "1" : responseSchemaVersion);
            session.setContentHash(sha256(canonical));
            session.setEmergency(session.getRiskLevel() == RiskLevel.RED);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to persist canonical triage snapshot", exception);
        }
    }

    private void persistValidatedEvidence(IntakeSession session) {
        if (triageSessionEvidenceWriter == null) {
            return;
        }
        List<Map<String, Object>> citations = readValidatedCitations(session);
        List<Map<String, Object>> claims = readValidatedClaims(session, citations);
        triageSessionEvidenceWriter.writeValidated(session.getId(), citations, claims);
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    /**
     * One-shot RED result map for a pre-screen short-circuit (CB-TRIAGE-IMP-003 §8.3).
     * MUST satisfy hasCanonicalRedContract (C8): emergencyActionRequired=true,
     * recommendationCode=SEEK_EMERGENCY_CARE, non-empty matchedRules.
     */
    private Map<String, Object> buildPreScreenRedResult(IntakeSession session, PreScreenResult preScreen) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "COMPLETED");
        result.put("riskLevel", "RED");
        result.put("riskColor", "#EF4444");
        result.put("summary", "Thông tin nhập vào khớp quy tắc cảnh báo khẩn cấp do quản trị viên cấu hình.");
        result.put("recommendedAction", TriageRedFlagPreScreenPolicy.EMERGENCY_GUIDANCE);
        result.put("emergencyActionRequired", true);
        result.put("recommendationCode", TriageRecommendationCode.forRisk("RED"));
        result.put("matchedRules", List.of("RED_FLAG_RULE_PRESCREEN"));
        result.put("redFlags", preScreen.matchedKeywords());
        result.put("disclaimer", TriageGraphService.DISCLAIMER);
        result.put("stage", sessionStage(session).name());
        result.put("citations", List.of());
        result.put("claims", List.of());
        result.put("evidenceIds", List.of());
        return result;
    }

    /**
     * Conversation TRIAGE_COMPLETE envelope for a pre-screen short-circuit (CB-TRIAGE-IMP-003 §8.3).
     * Uses only keys from CONVERSATION_RESPONSE_FIELDS so sanitizeEnvelope/toConversationResponse
     * accept it unchanged (C8); completion side effects stay inside persistConversationEnvelope (C2).
     */
    private Map<String, Object> buildPreScreenRedEnvelope(
            IntakeSession session, PreScreenResult preScreen, int round, Map<String, Object> mergedIntake) {
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("status", "TRIAGE_COMPLETE");
        envelope.put("intakeSessionId", session.getId().toString());
        envelope.put("stage", sessionStage(session).name());
        envelope.put("mergedIntake", new LinkedHashMap<>(mergedIntake));
        envelope.put("round", round);
        envelope.put("assistantMessage", TriageRedFlagPreScreenPolicy.EMERGENCY_GUIDANCE);
        envelope.put("triageResult", buildPreScreenRedResult(session, preScreen));
        return envelope;
    }

    /** Aggregates the free-text inputs a conversation flow exposes to the pre-screen (TDS §8.1). */
    @SafeVarargs
    private String preScreenText(String lead, Map<String, Object>... maps) {
        List<String> parts = new ArrayList<>();
        if (lead != null && !lead.isBlank()) {
            parts.add(lead);
        }
        for (Map<String, Object> map : maps) {
            if (map == null) {
                continue;
            }
            for (Object value : map.values()) {
                if (value instanceof String text && !text.isBlank()) {
                    parts.add(text);
                }
            }
        }
        return String.join(" ", parts);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castToMap(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : new LinkedHashMap<>();
    }

    /**
     * CB-TRIAGE-THMC-IMP-001 (BR-THMC-002/004/006, ADR-THMC-003 Option B): loads the caller's
     * active memories for the resolved subject. Fail-open — any error degrades to an empty
     * context with a WARN (no summary text, no user identifiers); intake is NEVER blocked.
     * Returns null when the memory feature is not wired (legacy test constructors).
     */
    private List<HealthMemoryContextItem> loadHealthContextFailOpen(
            UUID userId, TriageStage stage, UUID babyProfileId, UUID motherProfileId) {
        if (healthMemoryService == null) {
            return null;
        }
        UUID profileId = stage.isMaternal() ? motherProfileId : babyProfileId;
        try {
            List<HealthMemoryContextItem> context =
                    healthMemoryService.loadContextForIntake(userId, stage, profileId);
            return context == null ? List.of() : context;
        } catch (RuntimeException exception) {
            log.warn("Health memory context unavailable reason={}",
                    exception.getClass().getSimpleName());
            return List.of();
        }
    }

    /** Serialization-safe map form of the context for the canonical start payload (§9.2). */
    private List<Map<String, Object>> healthContextPayload(List<HealthMemoryContextItem> healthContext) {
        return healthContext.stream()
                .map(item -> {
                    Map<String, Object> entry = new LinkedHashMap<String, Object>();
                    entry.put("summaryText", item.summaryText());
                    entry.put("relatedStage", item.relatedStage());
                    entry.put("createdAt", item.createdAt() == null ? null : item.createdAt().toString());
                    entry.put("expiresAt", item.expiresAt() == null ? null : item.expiresAt().toString());
                    return entry;
                })
                .toList();
    }

    /** Rehydrates canonical-map context ("healthContext" key) for the fallback graph run. */
    private List<HealthMemoryContextItem> contextItemsFromCanonical(Object value) {
        if (!(value instanceof List<?> list) || list.isEmpty()) {
            return List.of();
        }
        return list.stream()
                .filter(Map.class::isInstance)
                .map(item -> {
                    Map<?, ?> map = (Map<?, ?>) item;
                    return new HealthMemoryContextItem(
                            map.get("summaryText") == null ? null : String.valueOf(map.get("summaryText")),
                            map.get("relatedStage") == null ? null : String.valueOf(map.get("relatedStage")),
                            null, null);
                })
                .toList();
    }

    // No symptom free text in the pre-screen log lines (PDPA hygiene, TDS §4.3/§14.2).
    private void recordPreScreenShortCircuit(String flow, PreScreenResult preScreen) {
        if (preScreenMetrics != null) {
            preScreenMetrics.recordShortCircuit(flow);
        }
        log.info("Triage pre-screen short-circuit flow={} ruleCount={}",
                flow, preScreen.matchedRuleIds().size());
    }

    private void recordPreScreenAnnotation(String flow, PreScreenResult preScreen) {
        if (preScreenMetrics != null) {
            preScreenMetrics.recordAnnotation(flow);
        }
        log.info("Triage pre-screen annotation flow={} ruleCount={}",
                flow, preScreen.matchedRuleIds().size());
    }

    private void publishCompletionEvents(IntakeSession session, UUID userId) {
        if (session.getRiskLevel() == RiskLevel.RED) {
            eventPublisher.publishEvent(new EmergencyEscalationTriggered(
                    UUID.randomUUID(), session.getId(), userId,
                    "AUTO_TRIAGE", session.getCompletedAt()));
        }
        eventPublisher.publishEvent(new IntakeSessionCompleted(
                UUID.randomUUID(), session.getId(), userId,
                session.getRiskLevel(), session.getCompletedAt()));
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
        // CB-TRIAGE-THMC-IMP-001: the conversation fallback receives the same server-loaded
        // context that was placed on the canonical request (narrative-only, BR-THMC-004).
        List<HealthMemoryContextItem> fallbackContext =
                contextItemsFromCanonical(request.get("healthContext"));
        ChildTriageResult graphResult = fallbackContext.isEmpty()
                ? triageGraphService.run(intake)
                : triageGraphService.run(intake, fallbackContext);
        Map<String, Object> result = objectMapper.convertValue(graphResult, new TypeReference<Map<String, Object>>() {});
        addJavaFallbackMetadata(result);
        String fallbackRisk = result.get("riskLevel") == null ? null : String.valueOf(result.get("riskLevel"));
        result.putIfAbsent("recommendationCode", TriageRecommendationCode.forRisk(fallbackRisk));
        Map<String, Object> envelope = new LinkedHashMap<>();
        TriageStage stage = intake.getStage() == null ? TriageStage.INFANT : intake.getStage();
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
            if (stage == TriageStage.PRECONCEPTION || stage == TriageStage.PREGNANCY) {
                result.put("summary", "Thông tin hiện có chưa đầy đủ; kết quả được phân loại thận trọng.");
                result.put("possibleConcern",
                        "Dấu hiệu hiện tại cần được nhân viên y tế đánh giá thêm.");
                result.put("recommendedAction",
                        "Liên hệ nhân viên y tế để được đánh giá và theo dõi. Gọi 115 nếu xuất hiện dấu hiệu nguy hiểm.");
                result.put("disclaimer",
                        "CareBridge không chẩn đoán bệnh, không kê thuốc và không thay thế nhân viên y tế.");
            }
        }
        result.put("stage", stage.name());
        sanitizeMergedIntake(current, result, stage);
        envelope.put("status", needMore ? "ASK_MORE" : "TRIAGE_COMPLETE");
        envelope.put("intakeSessionId", request.get("intakeSessionId"));
        envelope.put("stage", stage.name());
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
        if (intake.getStage() == TriageStage.POSTPARTUM) {
            return postpartumFallbackQuestions(intake);
        }
        if (intake.getStage() != null && intake.getStage().isMaternal()) {
            return maternalFallbackQuestions(intake);
        }
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

    private List<Map<String, Object>> maternalFallbackQuestions(RunIntakeRequest intake) {
        List<Map<String, Object>> questions = new ArrayList<>();
        if (intake.getDuration() == null || intake.getDuration().isBlank()) {
            questions.add(fallbackQuestion(
                    "duration",
                    "Dấu hiệu đã xuất hiện bao lâu và có tăng nhanh không?",
                    "TEXT",
                    List.of()));
        }
        if (intake.getBreathingStatus() == null || intake.getBreathingStatus().isBlank()) {
            questions.add(fallbackQuestion(
                    "breathingStatus",
                    "Bạn có khó thở, không thở được hoặc tím tái không?",
                    "SINGLE_CHOICE",
                    List.of("Không", "Khó thở", "Không thở được", "Tím tái", "Không chắc")));
        }
        if (intake.getConsciousnessStatus() == null || intake.getConsciousnessStatus().isBlank()) {
            questions.add(fallbackQuestion(
                    "consciousnessStatus",
                    "Bạn có lơ mơ, ngất hoặc khó giữ tỉnh táo không?",
                    "SINGLE_CHOICE",
                    List.of("Tỉnh táo", "Lơ mơ", "Ngất", "Khó giữ tỉnh táo", "Không chắc")));
        }
        if (questions.isEmpty()) {
            questions.add(fallbackQuestion(
                    "parentFreeText",
                    "Vui lòng mô tả thêm dấu hiệu sức khỏe bạn đang gặp.",
                    "TEXT",
                    List.of()));
        }
        return questions.stream().limit(3).toList();
    }

    private List<Map<String, Object>> postpartumFallbackQuestions(RunIntakeRequest intake) {
        List<Map<String, Object>> questions = new ArrayList<>();
        if (intake.getDuration() == null || intake.getDuration().isBlank()) {
            questions.add(fallbackQuestion(
                    "duration",
                    "Dấu hiệu đã xuất hiện bao lâu và có tăng nhanh không?",
                    "TEXT",
                    List.of()));
        }
        if (intake.getBreathingStatus() == null || intake.getBreathingStatus().isBlank()) {
            questions.add(fallbackQuestion(
                    "breathingStatus",
                    "Bạn có khó thở, đau ngực hoặc tím tái không?",
                    "SINGLE_CHOICE",
                    List.of("Không", "Khó thở", "Đau ngực", "Tím tái", "Không chắc")));
        }
        if (intake.getConsciousnessStatus() == null || intake.getConsciousnessStatus().isBlank()) {
            questions.add(fallbackQuestion(
                    "consciousnessStatus",
                    "Bạn có lơ mơ, ngất hoặc khó giữ tỉnh táo không?",
                    "SINGLE_CHOICE",
                    List.of("Tỉnh táo", "Lơ mơ", "Ngất", "Khó giữ tỉnh táo", "Không chắc")));
        }
        if (questions.isEmpty()) {
            questions.add(fallbackQuestion(
                    "parentFreeText",
                    "Vui lòng mô tả thêm dấu hiệu hồi phục bạn đang gặp.",
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
        result.putIfAbsent("ruleSetVersion", "java-stage-risk-rules-1.0");
        result.putIfAbsent("ontologyVersion", "java-stage-symptoms-1.0");
        result.putIfAbsent("responseSchemaVersion", "2.0");
        result.put("fallbackUsed", true);
        result.putIfAbsent("normalizedSymptoms", List.of());
        result.putIfAbsent("evidenceIds", List.of());
        result.putIfAbsent("claims", List.of());
        result.putIfAbsent("recommendationCode", TriageRecommendationCode.forRisk(fallbackRisk));
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

    @SuppressWarnings("unchecked")
    private Map<String, Object> withCanonicalStage(Object intakeValue, TriageStage stage, UUID babyProfileId, UUID motherProfileId) {
        Map<String, Object> intake = intakeValue instanceof Map<?, ?> map
                ? new LinkedHashMap<>((Map<String, Object>) map) : new LinkedHashMap<>();
        intake.put("stage", stage.name());
        if (stage.isPediatric()) {
            intake.put("babyProfileId", babyProfileId == null ? null : babyProfileId.toString());
            intake.remove("motherProfileId");
        } else {
            intake.put("motherProfileId", motherProfileId == null ? null : motherProfileId.toString());
            intake.remove("babyProfileId");
            if (stage.isMaternal()) {
                removePediatricIntakeFields(intake);
            }
        }
        return intake;
    }

    private TriageStage resolveStage(TriageStage requested, Map<String, Object> currentIntake, UUID babyProfileId, UUID motherProfileId) {
        if (requested != null) {
            return requested;
        }
        Object stageValue = currentIntake == null ? null : currentIntake.get("stage");
        if (stageValue != null) {
            try {
                return TriageStage.valueOf(String.valueOf(stageValue));
            } catch (IllegalArgumentException ignored) {
                throw new TriageException(HttpStatus.BAD_REQUEST, "TRIAGE-011", "Invalid triage stage");
            }
        }
        return motherProfileId != null && babyProfileId == null ? TriageStage.PREGNANCY : TriageStage.INFANT;
    }

    private TriageStage resolveStartStage(
            TriageStage requested, Map<String, Object> currentIntake, UUID babyProfileId, UUID motherProfileId, UUID userId) {
        TriageStage stage = resolveStage(requested, currentIntake, babyProfileId, motherProfileId);
        boolean hasIntakeStage = currentIntake != null && currentIntake.get("stage") != null;
        if (requested == null && !hasIntakeStage && babyProfileId == null && motherProfileId == null) {
            // LEGACY: default INFANT when stage is absent. Observe triage_stage_legacy_default_total;
            // reject this in the next API version after the Flutter stage selector is fully deployed and the metric is zero.
            triageStageLegacyDefaultMetrics.record(userId, false, false);
        }
        return stage;
    }

    private TriageStage requireCanonicalSessionStage(IntakeSession session) {
        if (session.getStage() == null) {
            throw new TriageException(HttpStatus.CONFLICT, "TRIAGE-014",
                    "Active intake session is missing its canonical triage stage");
        }
        return session.getStage();
    }

    private void ensurePostpartumEligible(TriageStage stage, UUID userId) {
        if (stage == TriageStage.POSTPARTUM) {
            postpartumEligibilityCheck.accept(userId);
        }
    }

    private void validateStageProfile(TriageStage stage, UUID babyProfileId, UUID motherProfileId, boolean requireProfile) {
        if (babyProfileId != null && motherProfileId != null) {
            throw new TriageException(HttpStatus.BAD_REQUEST, "TRIAGE-012", "Only one profile type may be linked to a triage session");
        }
        if (stage.isPediatric() && motherProfileId != null) {
            throw new TriageException(HttpStatus.BAD_REQUEST, "TRIAGE-012", "Pediatric triage stages require a baby profile, not a mother profile");
        }
        if (stage.isMaternal() && babyProfileId != null) {
            throw new TriageException(HttpStatus.BAD_REQUEST, "TRIAGE-012", "Maternal triage stages require a mother profile, not a baby profile");
        }
        if (requireProfile && ((stage.isPediatric() && babyProfileId == null) || (stage.isMaternal() && motherProfileId == null))) {
            throw new TriageException(HttpStatus.BAD_REQUEST, "TRIAGE-012", "A matching profile is required for the selected triage stage");
        }
    }

    private Map<String, Object> ensureSafeEnvelope(
            Map<String, Object> envelope, Map<String, Object> canonicalRequest, boolean start) {
        TriageStage stage = TriageStage.valueOf(String.valueOf(canonicalRequest.get("stage")));
        String status = String.valueOf(envelope.get("status"));
        if (hasExplicitStageMismatch(envelope.get("stage"), stage)
                || hasNestedStageMismatch(envelope.get("mergedIntake"), stage)) {
            log.warn("AI conversation returned mismatched canonical stage; using Java fallback");
            return fallbackConversation(canonicalRequest, start);
        }
        if ("ASK_MORE".equals(status)) {
            boolean valid = envelope.get("mergedIntake") instanceof Map<?, ?>
                    && envelope.get("triageResult") == null
                    && envelope.get("questions") instanceof List<?> questions && !questions.isEmpty()
                    && questions.stream().allMatch(this::isRenderableQuestion)
                    && questions.stream().allMatch(question -> isQuestionAllowedForStage(question, stage))
                    && envelope.get("round") instanceof Number round && round.intValue() >= 1 && round.intValue() <= 3;
            if (valid) {
                sanitizeEnvelope(envelope, stage);
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
        if (result != null && hasExplicitStageMismatch(result.get("stage"), stage)) {
            log.warn("AI conversation returned mismatched result stage; using Java fallback");
            return fallbackConversation(canonicalRequest, start);
        }
        String risk = result == null || result.get("riskLevel") == null
                ? null : String.valueOf(result.get("riskLevel"));
        String resultStatus = result == null ? null : nonBlank(result.get("status"));
        boolean terminalResult = resultStatus == null || "COMPLETED".equals(resultStatus);
        if (isPersistableRiskLevel(risk) && terminalResult
                && (!"RED".equals(risk) || hasCanonicalRedContract(result))) {
            sanitizeEnvelope(envelope, stage);
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

    private boolean hasCanonicalRedContract(Map<String, Object> result) {
        return Boolean.TRUE.equals(result.get("emergencyActionRequired"))
                && "SEEK_EMERGENCY_CARE".equals(result.get("recommendationCode"))
                && result.get("matchedRules") instanceof List<?> rules && !rules.isEmpty();
    }

    private boolean hasNestedStageMismatch(Object value, TriageStage canonicalStage) {
        return value instanceof Map<?, ?> map && hasExplicitStageMismatch(map.get("stage"), canonicalStage);
    }

    private boolean hasExplicitStageMismatch(Object value, TriageStage canonicalStage) {
        return value != null && !canonicalStage.name().equals(String.valueOf(value));
    }

    private void requireNoExplicitStageMismatch(Object value, TriageStage canonicalStage, String boundary) {
        if (hasExplicitStageMismatch(value, canonicalStage)) {
            throw new IllegalStateException("AI triage returned mismatched " + boundary + " stage");
        }
    }

    private boolean isQuestionAllowedForStage(Object value, TriageStage stage) {
        if (!stage.isMaternal()) {
            return true;
        }
        if (!(value instanceof Map<?, ?> question)) return false;
        String questionKey = nonBlank(question.get("questionKey"));
        return questionKey != null && java.util.Set.of(
                "duration",
                "parentFreeText",
                "symptomList",
                "breathingStatus",
                "consciousnessStatus",
                "seizure",
                "temperatureC").contains(questionKey);
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
            return evidenceSourceService.isApprovedDeepLink(uri);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private List<Map<String, Object>> readValidatedClaims(
            IntakeSession session, List<Map<String, Object>> citations) {
        java.util.Set<String> validEvidenceIds = citations.stream()
                .map(citation -> nonBlank(citation.get("sourceId")))
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        List<Map<String, Object>> valid = new ArrayList<>();
        for (Map<String, Object> claim : readObjectList(session, "claims")) {
            String claimId = nonBlank(claim.get("claimId"));
            String text = nonBlank(claim.get("text"));
            Object evidenceValue = claim.get("evidenceIds");
            if (claimId == null || text == null || !(evidenceValue instanceof List<?> evidenceIds)) {
                continue;
            }
            List<String> verified = evidenceIds.stream()
                    .map(this::nonBlank)
                    .filter(java.util.Objects::nonNull)
                    .filter(validEvidenceIds::contains)
                    .distinct()
                    .toList();
            if (verified.isEmpty()) {
                continue;
            }
            Map<String, Object> safe = new LinkedHashMap<>();
            safe.put("claimId", claimId);
            safe.put("text", text);
            safe.put("evidenceIds", verified);
            valid.add(safe);
        }
        return valid;
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
    private void sanitizeEnvelope(Map<String, Object> envelope, TriageStage stage) {
        envelope.put("stage", stage.name());
        Object intakeValue = envelope.get("mergedIntake");
        Object resultValue = envelope.get("triageResult");
        Map<String, Object> result = resultValue instanceof Map<?, ?> map
                ? objectMapper.convertValue(map, new TypeReference<Map<String, Object>>() {}) : Map.of();
        if (intakeValue instanceof Map<?, ?> map) {
            Map<String, Object> intake = new LinkedHashMap<>((Map<String, Object>) map);
            intake.put("stage", stage.name());
            sanitizeMergedIntake(intake, result, stage);
            envelope.put("mergedIntake", intake);
        }
        if (!result.isEmpty()) {
            result.put("stage", stage.name());
            envelope.put("triageResult", result);
        }
    }

    private void sanitizeMergedIntake(
            Map<String, Object> intake, Map<String, Object> result, TriageStage stage) {
        intake.put("parentFreeText", null);
        if (stage.isMaternal()) {
            removePediatricIntakeFields(intake);
        }
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

    private void removePediatricIntakeFields(Map<String, Object> intake) {
        java.util.Set.of(
                "babyProfileId",
                "childAgeMonths",
                "feedingStatus",
                "vomiting",
                "diarrhea",
                "rash",
                "dehydrationSigns").forEach(intake::remove);
    }

    private int number(Object value, int fallback) {
        return value instanceof Number number ? number.intValue() : fallback;
    }

    private IntakeConversationResponse toConversationResponse(
            Map<String, Object> envelope, IntakeSession session) {
        Map<String, Object> responseFields = new LinkedHashMap<>(envelope);
        responseFields.keySet().removeAll(CONVERSATION_RESPONSE_METADATA_FIELDS);
        List<String> unknownFields = responseFields.keySet().stream()
                .filter(field -> !CONVERSATION_RESPONSE_FIELDS.contains(field))
                .sorted()
                .toList();
        if (!unknownFields.isEmpty()) {
            throw new IllegalArgumentException(
                    "Unrecognized conversation response field(s): " + unknownFields);
        }
        IntakeConversationResponse response = objectMapper.convertValue(
                responseFields, IntakeConversationResponse.class);
        if (session.getJourneyId() != null) {
            response.setJourneyId(session.getJourneyId());
            response.setOriginDashboard(session.getOriginDashboard());
            response.setOriginReferenceId(session.getOriginReferenceId());
            response.setOriginAction(com.carebridge.backend.triage.OriginAction.forDashboard(
                    session.getOriginDashboard()));
            boolean exposeContinuation = hasActiveTerminalContinuation(session);
            response.setContinuationToken(exposeContinuation ? session.getContinuationToken() : null);
            response.setContinuationExpiresAt(exposeContinuation ? session.getContinuationExpiresAt() : null);
        }
        return response;
    }

    private LifecycleBinding bindLifecycle(
            StartIntakeConversationRequest request, TriageStage stage, UUID userId) {
        boolean requested = request.getJourneyId() != null || request.getOriginDashboard() != null
                || request.getOriginReferenceId() != null;
        if (!requested) return null;
        if (lifecycleBindingService == null) {
            throw new IllegalStateException("Lifecycle intake binding service is unavailable");
        }
        return lifecycleBindingService.bindForStart(request, stage, userId);
    }

    private void validateReplay(IntakeSession session, LifecycleBinding requestedBinding) {
        if (session.getJourneyId() == null && requestedBinding == null) return;
        if (lifecycleBindingService == null) {
            throw new IllegalStateException("Lifecycle intake binding service is unavailable");
        }
        lifecycleBindingService.validateReplay(session, requestedBinding);
    }

    private void validateStartReplay(
            IntakeSession session,
            LifecycleBinding requestedBinding,
            TriageStage requestedStage,
            UUID requestedBabyProfileId,
            UUID requestedMotherProfileId) {
        boolean sameIntent = sessionStage(session) == requestedStage
                && java.util.Objects.equals(session.getBabyProfileId(), requestedBabyProfileId)
                && java.util.Objects.equals(session.getMotherProfileId(), requestedMotherProfileId);
        if (!sameIntent) {
            throw new TriageException(HttpStatus.CONFLICT, "TRIAGE-016", "Intake context conflict");
        }
        validateReplay(session, requestedBinding);
    }

    private void applyBinding(IntakeSession session, LifecycleBinding binding) {
        if (binding == null) return;
        session.setJourneyId(binding.journeyId());
        session.setOriginDashboard(binding.originDashboard());
        session.setOriginReferenceId(binding.originReferenceId());
        session.setContinuationToken(binding.continuationToken());
        session.setContinuationExpiresAt(binding.continuationExpiresAt());
    }
}
