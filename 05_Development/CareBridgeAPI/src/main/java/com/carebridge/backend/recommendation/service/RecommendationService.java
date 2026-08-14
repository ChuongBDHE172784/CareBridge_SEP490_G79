package com.carebridge.backend.recommendation.service;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.community.entity.CommunityTopic;
import com.carebridge.backend.community.entity.TopicType;
import com.carebridge.backend.community.repository.CommunityTopicRepository;
import com.carebridge.backend.common.constants.ConsentConstants;
import com.carebridge.backend.consent.entity.ConsentDataType;
import com.carebridge.backend.consent.entity.ConsentGrant;
import com.carebridge.backend.consent.entity.ConsentPurpose;
import com.carebridge.backend.consent.repository.ConsentGrantRepository;
import com.carebridge.backend.content.entity.ContentItem;
import com.carebridge.backend.content.repository.ContentRepository;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.MotherJourney;
import com.carebridge.backend.journey.entity.PregnancyOutcomeEvidence;
import com.carebridge.backend.journey.entity.PregnancyOutcomeType;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import com.carebridge.backend.journey.repository.MotherJourneyTransitionRepository;
import com.carebridge.backend.journey.repository.PregnancyOutcomeEvidenceRepository;
import com.carebridge.backend.recommendation.RecommendationConstants;
import com.carebridge.backend.recommendation.dto.RecommendationContentResponse;
import com.carebridge.backend.recommendation.dto.RecommendationContentResponse.ContentSummary;
import com.carebridge.backend.recommendation.dto.RecommendationEnums.CoverageStatus;
import com.carebridge.backend.recommendation.dto.RecommendationEnums.ReasonCode;
import com.carebridge.backend.recommendation.dto.RecommendationEnums.SelectionMode;
import com.carebridge.backend.recommendation.dto.RecommendationEnums.SelectionType;
import com.carebridge.backend.recommendation.dto.RecommendationProfileResponse;
import com.carebridge.backend.recommendation.dto.RecommendationTagCatalogResponse;
import com.carebridge.backend.recommendation.entity.RecommendationProfileStatus;
import com.carebridge.backend.recommendation.exception.RecommendationException;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.carebridge.backend.family.repository.CareGroupMemberRepository;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.health.service.RecommendationBmiObservationSynchronizer;

@Service
@Slf4j
public class RecommendationService implements RecommendationConsentCleanup {

    public static final String CONSENT_SCOPE = "MOTHER_PERSONALIZED_CONTENT";
    private final MotherJourneyRepository journeyRepository;
    private final MotherJourneyTransitionRepository transitionRepository;
    private final PregnancyOutcomeEvidenceRepository outcomeEvidenceRepository;
    private final UserRepository userRepository;
    private final ContentRepository contentRepository;
    private final CommunityTopicRepository topicRepository;
    private final ConsentGrantRepository consentGrantRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;
    private final RecommendationProfileValidator validator;
    private final RecommendationContextResolver contextResolver;
    private final RecommendationEligibilityPolicy eligibilityPolicy;
    private final RecommendationRanker ranker;
    private final RecommendationBmiObservationSynchronizer bmiObservationSynchronizer;
    private final Clock clock;
    private final boolean enabled;
    @Autowired(required = false)
    private CareGroupMemberRepository careGroupMemberRepository;
    @Autowired(required = false)
    private CareGroupRepository careGroupRepository;

    @Autowired
    public RecommendationService(
            MotherJourneyRepository journeyRepository,
            PregnancyOutcomeEvidenceRepository outcomeEvidenceRepository,
            MotherJourneyTransitionRepository transitionRepository,
            UserRepository userRepository,
            ContentRepository contentRepository,
            CommunityTopicRepository topicRepository,
            ConsentGrantRepository consentGrantRepository,
            AuditService auditService,
            ObjectMapper objectMapper,
            RecommendationProfileValidator validator,
            RecommendationContextResolver contextResolver,
            RecommendationEligibilityPolicy eligibilityPolicy,
            RecommendationRanker ranker,
            RecommendationBmiObservationSynchronizer bmiObservationSynchronizer,
            @Value("${carebridge.recommendation.enabled:true}") boolean enabled) {
        this(journeyRepository, outcomeEvidenceRepository, transitionRepository, userRepository, contentRepository, topicRepository,
                consentGrantRepository, auditService, objectMapper, validator, contextResolver,
                eligibilityPolicy, ranker, bmiObservationSynchronizer, Clock.systemUTC(), enabled);
    }

    /** Compatibility constructor for focused unit tests that do not exercise epoch lookup. */
    public RecommendationService(
            MotherJourneyRepository journeyRepository,
            PregnancyOutcomeEvidenceRepository outcomeEvidenceRepository,
            UserRepository userRepository,
            ContentRepository contentRepository,
            CommunityTopicRepository topicRepository,
            ConsentGrantRepository consentGrantRepository,
            AuditService auditService,
            ObjectMapper objectMapper,
            RecommendationProfileValidator validator,
            RecommendationContextResolver contextResolver,
            RecommendationEligibilityPolicy eligibilityPolicy,
            RecommendationRanker ranker,
            Clock clock) {
        this(journeyRepository, outcomeEvidenceRepository, null, userRepository, contentRepository, topicRepository,
                consentGrantRepository, auditService, objectMapper, validator, contextResolver,
                eligibilityPolicy, ranker, null, clock, true);
    }

    public RecommendationService(
            MotherJourneyRepository journeyRepository,
            PregnancyOutcomeEvidenceRepository outcomeEvidenceRepository,
            UserRepository userRepository,
            ContentRepository contentRepository,
            CommunityTopicRepository topicRepository,
            ConsentGrantRepository consentGrantRepository,
            AuditService auditService,
            ObjectMapper objectMapper,
            RecommendationProfileValidator validator,
            RecommendationContextResolver contextResolver,
            RecommendationEligibilityPolicy eligibilityPolicy,
            RecommendationRanker ranker) {
        this(journeyRepository, outcomeEvidenceRepository, null, userRepository, contentRepository, topicRepository,
                consentGrantRepository, auditService, objectMapper, validator, contextResolver,
                eligibilityPolicy, ranker, null, Clock.systemUTC(), true);
    }

    public RecommendationService(
            MotherJourneyRepository journeyRepository,
            PregnancyOutcomeEvidenceRepository outcomeEvidenceRepository,
            MotherJourneyTransitionRepository transitionRepository,
            UserRepository userRepository,
            ContentRepository contentRepository,
            CommunityTopicRepository topicRepository,
            ConsentGrantRepository consentGrantRepository,
            AuditService auditService,
            ObjectMapper objectMapper,
            RecommendationProfileValidator validator,
            RecommendationContextResolver contextResolver,
            RecommendationEligibilityPolicy eligibilityPolicy,
            RecommendationRanker ranker,
            Clock clock) {
        this(journeyRepository, outcomeEvidenceRepository, transitionRepository, userRepository, contentRepository,
                topicRepository, consentGrantRepository, auditService, objectMapper, validator, contextResolver,
                eligibilityPolicy, ranker, null, clock, true);
    }

    public RecommendationService(
            MotherJourneyRepository journeyRepository,
            PregnancyOutcomeEvidenceRepository outcomeEvidenceRepository,
            MotherJourneyTransitionRepository transitionRepository,
            UserRepository userRepository,
            ContentRepository contentRepository,
            CommunityTopicRepository topicRepository,
            ConsentGrantRepository consentGrantRepository,
            AuditService auditService,
            ObjectMapper objectMapper,
            RecommendationProfileValidator validator,
            RecommendationContextResolver contextResolver,
            RecommendationEligibilityPolicy eligibilityPolicy,
            RecommendationRanker ranker,
            Clock clock,
            boolean enabled) {
        this(journeyRepository, outcomeEvidenceRepository, transitionRepository, userRepository, contentRepository,
                topicRepository, consentGrantRepository, auditService, objectMapper, validator, contextResolver,
                eligibilityPolicy, ranker, null, clock, enabled);
    }

    /** Explicit collaborator constructor for focused tests that exercise profile mutation. */
    public RecommendationService(
            MotherJourneyRepository journeyRepository,
            PregnancyOutcomeEvidenceRepository outcomeEvidenceRepository,
            MotherJourneyTransitionRepository transitionRepository,
            UserRepository userRepository,
            ContentRepository contentRepository,
            CommunityTopicRepository topicRepository,
            ConsentGrantRepository consentGrantRepository,
            AuditService auditService,
            ObjectMapper objectMapper,
            RecommendationProfileValidator validator,
            RecommendationContextResolver contextResolver,
            RecommendationEligibilityPolicy eligibilityPolicy,
            RecommendationRanker ranker,
            RecommendationBmiObservationSynchronizer bmiObservationSynchronizer,
            Clock clock,
            boolean enabled) {
        this.journeyRepository = journeyRepository;
        this.outcomeEvidenceRepository = outcomeEvidenceRepository;
        this.transitionRepository = transitionRepository;
        this.userRepository = userRepository;
        this.contentRepository = contentRepository;
        this.topicRepository = topicRepository;
        this.consentGrantRepository = consentGrantRepository;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.contextResolver = contextResolver;
        this.eligibilityPolicy = eligibilityPolicy;
        this.ranker = ranker;
        this.bmiObservationSynchronizer = bmiObservationSynchronizer;
        this.clock = clock;
        this.enabled = enabled;
    }

    @Transactional
    public RecommendationProfileResponse getProfile(UUID ownerUserId) {
        MotherJourney journey = canonical(ownerUserId, true);
        ConsentAssessment assessment = assessConsent(journey, ownerUserId, Instant.now(clock));
        return toProfileResponse(journey, assessment);
    }

    @Transactional
    public RecommendationProfileResponse putProfile(UUID ownerUserId, JsonNode request) {
        MotherJourney journey = canonical(ownerUserId, true);
        User user = userRepository.findById(ownerUserId)
                .orElseThrow(RecommendationException::contextUnavailable);
        JsonNode accepted = request == null ? null : request.get("consentAccepted");
        if (accepted != null && accepted.isBoolean() && !accepted.asBoolean()) {
            UUID submissionId = validator.validateDecline(request);
            revokeActiveRecommendationGrants(ownerUserId);
            clearProfile(journey, RecommendationProfileStatus.DECLINED);
            journeyRepository.saveAndFlush(journey);
            auditProfile("DECLINED", journey, 0, null);
            return toProfileResponse(journey, assessConsent(journey, ownerUserId, Instant.now(clock)));
        }

        ValidatedRecommendationProfile validated = validator.validateAccept(
                request, journey.getJourneyType(), user.getDateOfBirth());
        ensureReproductiveHistoryConsistency(journey, validated);
        Map<String, Object> currentEnvelope = safeMap(journey.getRecommendationProfileJson());
        UUID currentSubmission = uuidFrom(currentEnvelope.get("submissionId"));
        if (currentSubmission != null && currentSubmission.equals(validated.submissionId())) {
            String currentCanonical = canonicalJson(currentEnvelope.get("profile"));
            if (!Objects.equals(currentCanonical, validated.canonicalProfileJson())) {
                throw RecommendationException.conflict("RECOMMENDATION_SUBMISSION_CONFLICT",
                        "Submission identity was already used with different profile data");
            }
            synchronizeBmi(journey, validated);
            return toProfileResponse(journey, assessConsent(journey, ownerUserId, Instant.now(clock)));
        }
        Optional<ConsentGrant> oldSubmission = consentGrantRepository.findRecommendationGrantByEvidence(
                ownerUserId, validated.submissionId(), CONSENT_SCOPE);
        if (oldSubmission.isPresent() && (currentSubmission == null || !currentSubmission.equals(validated.submissionId()))) {
            throw RecommendationException.conflict("RECOMMENDATION_SUBMISSION_CONFLICT",
                    "Submission identity was already used");
        }

        Instant now = Instant.now(clock);
        // Retire any prior recommendation evidence before issuing a replacement.
        // This keeps consent state bounded and prevents an old revoke callback
        // from clearing the newly accepted profile.
        revokeActiveRecommendationGrants(ownerUserId);
        ConsentGrant grant = ConsentGrant.builder()
                .userId(ownerUserId)
                .dataType(ConsentDataType.SENSITIVE_DATA)
                .purpose(ConsentPurpose.PERSONALIZE)
                .scope(CONSENT_SCOPE)
                .policyVersion(RecommendationConstants.POLICY_VERSION)
                .evidenceKey(validated.submissionId())
                .consentGivenAt(now)
                .expiryAt(now.plus(ConsentConstants.DEFAULT_EXPIRY_DAYS, ChronoUnit.DAYS))
                .status("ACTIVE")
                .build();
        grant = consentGrantRepository.saveAndFlush(grant);
        int revision = number(currentEnvelope.get("revision")) + 1;
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("schemaVersion", RecommendationConstants.SCHEMA_VERSION);
        envelope.put("revision", revision);
        envelope.put("submissionId", validated.submissionId().toString());
        envelope.put("completedAt", now.toString());
        envelope.put("updatedAt", now.toString());
        envelope.put("profile", validated.profile());
        envelope.put("derived", validated.derived());
        envelope.put("consentGrantId", grant.getId());
        journey.setRecommendationProfileJson(envelope);
        journey.setRecommendationProfileVersion((short) RecommendationConstants.SCHEMA_VERSION);
        journey.setRecommendationProfileCompletedAt(now);
        journey.setRecommendationProfileStatus(RecommendationProfileStatus.ACTIVE);
        synchronizeBmi(journey, validated);
        journeyRepository.saveAndFlush(journey);
        auditProfile("UPDATED", journey, revision, grant.getId());
        auditConsent("GRANTED", journey, revision, grant.getId());
        return toProfileResponse(journey, assessConsent(journey, ownerUserId, now));
    }

    @Transactional
    public RecommendationContentResponse getContent(UUID ownerUserId, int limit) {
        return getContent(ownerUserId, null, limit);
    }

    @Transactional
    public RecommendationContentResponse getContent(UUID ownerUserId, UUID careGroupId, int limit) {
        if (limit < 1 || limit > RecommendationConstants.MAX_LIMIT) {
            throw new RecommendationException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "RECOMMENDATION_LIMIT_INVALID", "limit must be between 1 and 3");
        }
        MotherJourney journey = canonical(ownerUserId, careGroupId, true);
        ConsentAssessment assessment = assessConsent(journey, ownerUserId, Instant.now(clock));
        RecommendationContext context;
        try {
            context = contextResolver.resolve(journey);
        } catch (IllegalArgumentException ex) {
            throw RecommendationException.journeyRequired();
        }

        Set<String> signals = new LinkedHashSet<>();
        if (enabled
                && journey.getRecommendationProfileStatus() == RecommendationProfileStatus.ACTIVE
                && assessment.active()) {
            signals.addAll(resolveStoredSignals(journey, ownerUserId));
            signals.addAll(resolveSupportPreferenceSignals(journey));
        }
        // Fetch the two pools independently. A high-volume fallback pool cannot
        // consume the targeted page before Java applies the frozen comparator.
        // The SQL predicates are only an optimization; hard eligibility and
        // catalog validation remain authoritative below.
        List<ContentItem> targetedItems = signals.isEmpty()
                ? List.of()
                : contentRepository.findApprovedTargetedArticlesForRecommendation(
                        context.stage().name(), context.pregnancyWeek(), signals,
                        PageRequest.of(0, RecommendationConstants.MAX_POOL_SCAN));
        List<ContentItem> fallbackItems = contentRepository.findApprovedFallbackArticlesForRecommendation(
                context.stage().name(), context.pregnancyWeek(),
                PageRequest.of(0, RecommendationConstants.MAX_POOL_SCAN));
        Map<UUID, ContentItem> itemById = new LinkedHashMap<>();
        targetedItems.forEach(item -> itemById.put(item.getId(), item));
        fallbackItems.forEach(item -> itemById.putIfAbsent(item.getId(), item));
        List<ContentItem> items = new ArrayList<>(itemById.values());
        Map<UUID, List<UUID>> tagIdsByContentId = loadRecommendationTagIds(itemById.keySet());
        Set<UUID> tagIds = tagIdsByContentId.values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
        Map<UUID, CommunityTopic> topics = loadTopics(tagIds);
        List<RecommendationRanker.Candidate> targeted = new ArrayList<>();
        List<RecommendationRanker.Candidate> fallback = new ArrayList<>();
        for (ContentItem item : items) {
            if (!eligibilityPolicy.isHardEligible(item, context)) continue;
            List<UUID> associatedTagIds = tagIdsByContentId.getOrDefault(item.getId(), List.of());
            // A missing topic row means we cannot prove that the article has no
            // controlled rec-* tag.  Fail closed instead of accidentally
            // treating a partially-resolved article as lifecycle fallback.
            if (associatedTagIds.stream().anyMatch(id -> !topics.containsKey(id))) {
                continue;
            }
            Set<CommunityTopic> associated = associatedTagIds.stream()
                    .map(topics::get).filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
            boolean invalidControlled = associated.stream()
                    .filter(RecommendationMetadataPolicy::isControlled)
                    .anyMatch(topic -> !RecommendationMetadataPolicy.isCatalogTopic(topic));
            if (invalidControlled) continue;
            Set<String> allRec = associated.stream().map(CommunityTopic::getSlug)
                    .filter(Objects::nonNull).filter(slug -> slug.startsWith("rec-")).collect(Collectors.toSet());
            Set<String> matched = associated.stream()
                    .filter(topic -> topic.getType() == TopicType.TAG && !topic.isHidden())
                    .map(CommunityTopic::getSlug)
                    .filter(RecommendationConstants.ALL_TAG_SLUGS::contains)
                    .filter(signals::contains)
                    .collect(Collectors.toSet());
            boolean validTargeted = !allRec.isEmpty()
                    && allRec.stream().allMatch(RecommendationConstants.ALL_TAG_SLUGS::contains)
                    && associated.stream().filter(RecommendationMetadataPolicy::isControlled)
                            .allMatch(RecommendationMetadataPolicy::isCatalogTopic);
            if (validTargeted && !matched.isEmpty()) {
                targeted.add(candidate(item, matched.size()));
            } else if (allRec.isEmpty()) {
                fallback.add(candidate(item, 0));
            }
        }
        Comparator<RecommendationRanker.Candidate> comparator = ranker.comparator();
        targeted.sort(comparator);
        fallback.sort(comparator);
        List<RecommendationRanker.Candidate> selected = new ArrayList<>(targeted.stream().limit(limit).toList());
        if (selected.size() < limit) {
            Set<UUID> ids = selected.stream().map(value -> value.item().getId()).collect(Collectors.toSet());
            fallback.stream().filter(value -> !ids.contains(value.item().getId()))
                    .limit(limit - selected.size()).forEach(selected::add);
        }
        List<RecommendationContentResponse.Item> responseItems = new ArrayList<>();
        for (int i = 0; i < selected.size(); i++) {
            RecommendationRanker.Candidate candidate = selected.get(i);
            boolean isTargeted = candidate.matchedCount() > 0;
            responseItems.add(new RecommendationContentResponse.Item(
                    i + 1,
                    isTargeted ? SelectionType.TARGETED : SelectionType.FALLBACK,
                    isTargeted ? ReasonCode.PERSONALIZED_CONTEXT : ReasonCode.LIFECYCLE_FALLBACK,
                    isTargeted ? "Phù hợp với ngữ cảnh chăm sóc của bạn" : "Hữu ích cho giai đoạn hiện tại của bạn",
                    new ContentSummary(candidate.item().getId(), candidate.item().getType().name(), candidate.item().getTitle(),
                            candidate.item().getSummary(), candidate.item().getStage().name(), candidate.item().getTopicId(), candidate.item().getPublishedAt())));
        }
        boolean fallbackUsed = responseItems.stream().anyMatch(item -> item.selectionType() == SelectionType.FALLBACK);
        SelectionMode mode = responseItems.isEmpty() ? SelectionMode.EMPTY
                : responseItems.stream().allMatch(item -> item.selectionType() == SelectionType.FALLBACK) ? SelectionMode.FALLBACK_ONLY
                : fallbackUsed ? SelectionMode.TARGETED_WITH_FALLBACK : SelectionMode.TARGETED_ONLY;
        CoverageStatus coverage = responseItems.size() == limit ? CoverageStatus.COMPLETE
                : responseItems.isEmpty() ? CoverageStatus.EMPTY : CoverageStatus.PARTIAL;
        return new RecommendationContentResponse(context.stage().name(), context.pregnancyWeek(),
                context.weekEligibilityMode(), journey.getRecommendationProfileStatus(), mode, coverage, fallbackUsed, responseItems);
    }

    @Transactional(readOnly = true)
    public RecommendationTagCatalogResponse getCatalog() {
        List<CommunityTopic> rows = topicRepository.findAllBySlugIn(RecommendationConstants.ALL_TAG_SLUGS).stream()
                .filter(RecommendationMetadataPolicy::isCatalogTopic)
                .sorted(Comparator.comparing(CommunityTopic::getSlug))
                .toList();
        Set<String> resolvedSlugs = rows.stream().map(CommunityTopic::getSlug).collect(Collectors.toSet());
        if (resolvedSlugs.size() != RecommendationConstants.ALL_TAG_SLUGS.size()
                || !resolvedSlugs.containsAll(RecommendationConstants.ALL_TAG_SLUGS)) {
            // A partial/retired catalog must never be presented to editors as a
            // complete V1 catalog.  Fail closed so authoring can retry after
            // the forward seed/migration has been repaired.
            throw RecommendationException.contextUnavailable();
        }
        List<RecommendationTagCatalogResponse.Item> items = rows.stream().map(topic -> {
            String description = topic.getDescription() == null ? "" : topic.getDescription();
            String domain = description.startsWith(RecommendationConstants.CATALOG_VERSION + "|")
                    ? description.substring((RecommendationConstants.CATALOG_VERSION + "|").length()) : "UNKNOWN";
            return new RecommendationTagCatalogResponse.Item(topic.getId(), topic.getSlug(), domain, topic.getName());
        }).sorted(Comparator.comparing(RecommendationTagCatalogResponse.Item::domain)
                .thenComparing(RecommendationTagCatalogResponse.Item::slug)).toList();
        return new RecommendationTagCatalogResponse(RecommendationConstants.CATALOG_VERSION, items);
    }

    @Override
    @Transactional
    public void onRevoked(UUID ownerUserId, ConsentGrant grant) {
        if (grant == null
                || !CONSENT_SCOPE.equals(grant.getScope())
                || grant.getDataType() != ConsentDataType.SENSITIVE_DATA
                || grant.getPurpose() != ConsentPurpose.PERSONALIZE
                || !RecommendationConstants.POLICY_VERSION.equals(grant.getPolicyVersion())) return;
        List<MotherJourney> journeys = journeyRepository.findLatestMaternalForUpdate(
                ownerUserId, PageRequest.of(0, 1));
        if (!journeys.isEmpty()) {
            MotherJourney journey = journeys.get(0);
            Map<String, Object> envelope = safeMap(journey.getRecommendationProfileJson());
            Long currentGrantId = longId(envelope.get("consentGrantId"));
            if (currentGrantId == null || currentGrantId.equals(grant.getId())) {
                clearProfile(journey, RecommendationProfileStatus.REVOKED);
                journeyRepository.saveAndFlush(journey);
                auditProfile("REVOKED", journey, 0, grant.getId());
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markStageReview(UUID ownerUserId, UUID journeyId, JourneyType stage) {
        journeyRepository.findByIdAndOwnerUserIdForUpdate(journeyId, ownerUserId).ifPresent(journey -> {
            if (journey.getRecommendationProfileStatus() == RecommendationProfileStatus.ACTIVE) {
                journey.setRecommendationProfileStatus(RecommendationProfileStatus.REVIEW_REQUIRED);
                journeyRepository.saveAndFlush(journey);
                auditProfile("REVIEW_REQUIRED", journey, number(safeMap(journey.getRecommendationProfileJson()).get("revision")), null);
            }
        });
    }

    private RecommendationRanker.Candidate candidate(ContentItem item, int matchedCount) {
        return new RecommendationRanker.Candidate(item, matchedCount, eligibilityPolicy.isStageWide(item),
                eligibilityPolicy.inclusiveWidth(item), item.getRecommendationPriority() == null ? 0 : item.getRecommendationPriority());
    }

    private void ensureReproductiveHistoryConsistency(
            MotherJourney journey, ValidatedRecommendationProfile validated) {
        Object rawDomain = validated.profile().get("reproductiveHistory");
        if (!(rawDomain instanceof Map<?, ?> domain)) return;
        Object rawCodes = domain.get("codes");
        if (!(rawCodes instanceof List<?> codes) || !codes.contains("NO_PRIOR_PREGNANCY")) return;
        boolean completedOutcome = journey.getPregnancyOutcome() != null
                && journey.getPregnancyOutcome().transitionsToPostpartum();
        if (!completedOutcome) {
            try {
                completedOutcome = outcomeEvidenceRepository
                        .findFirstByJourneyIdAfterEpochVersionOrderByRevisionNumberDesc(
                                journey.getId(), currentPregnancyEpochVersion(journey))
                        .map(evidence -> evidence.getOutcomeType() != null
                                && evidence.getOutcomeType().transitionsToPostpartum())
                        .orElse(false);
            } catch (RuntimeException ex) {
                log.warn("Recommendation reproductive history context unavailable for journeyId={}", journey.getId());
                throw RecommendationException.contextUnavailable();
            }
        }
        if (completedOutcome) {
            throw RecommendationException.conflict(
                    "RECOMMENDATION_REPRODUCTIVE_HISTORY_CONFLICT",
                    "Reproductive history requires review before personalization can be enabled");
        }
    }

    private Map<UUID, List<UUID>> loadRecommendationTagIds(Collection<UUID> contentItemIds) {
        if (contentItemIds == null || contentItemIds.isEmpty()) return Map.of();
        Map<UUID, List<UUID>> result = new HashMap<>();
        List<Object[]> rows = contentRepository.findRecommendationTagRows(contentItemIds);
        if (rows == null || rows.isEmpty()) return result;
        for (Object[] row : rows) {
            if (row == null || row.length < 2 || !(row[0] instanceof UUID contentItemId)
                    || !(row[1] instanceof UUID tagId)) continue;
            result.computeIfAbsent(contentItemId, ignored -> new ArrayList<>()).add(tagId);
        }
        return result;
    }

    private Map<UUID, CommunityTopic> loadTopics(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) return Map.of();
        return topicRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(CommunityTopic::getId, value -> value));
    }

    private Set<String> resolveStoredSignals(MotherJourney journey, UUID ownerUserId) {
        try {
            Map<String, Object> envelope = safeMap(journey.getRecommendationProfileJson());
            Object profile = envelope.get("profile");
            JsonNode root = objectMapper.createObjectNode()
                    .deepCopy();
            com.fasterxml.jackson.databind.node.ObjectNode request = (com.fasterxml.jackson.databind.node.ObjectNode) root;
            request.put("submissionId", String.valueOf(envelope.get("submissionId")))
                    .put("schemaVersion", RecommendationConstants.SCHEMA_VERSION)
                    .put("policyVersion", RecommendationConstants.POLICY_VERSION)
                    .put("consentAccepted", true)
                    .set("profile", objectMapper.valueToTree(profile));
            User user = userRepository.findById(ownerUserId).orElseThrow();
            Set<String> signals = new LinkedHashSet<>(
                    validator.validateAccept(request, journey.getJourneyType(), user.getDateOfBirth()).signalSlugs());
            PregnancyOutcomeType outcome = journey.getPregnancyOutcome();
            if (outcome == null || !outcome.transitionsToPostpartum()) {
                outcome = latestOutcomeForCurrentEpoch(journey)
                        .map(PregnancyOutcomeEvidence::getOutcomeType)
                        .orElse(null);
            }
            String outcomeSignal = outcome == null ? null : switch (outcome) {
                case LIVE_BIRTH -> RecommendationConstants.REPRODUCTIVE_SIGNALS.get("PRIOR_LIVE_BIRTH");
                case PREGNANCY_LOSS -> RecommendationConstants.REPRODUCTIVE_SIGNALS.get("PRIOR_PREGNANCY_LOSS");
                case STILLBIRTH -> RecommendationConstants.REPRODUCTIVE_SIGNALS.get("PRIOR_STILLBIRTH");
                default -> null;
            };
            if (outcomeSignal != null) signals.add(outcomeSignal);
            return signals;
        } catch (RuntimeException ex) {
            log.warn("Recommendation signal resolution failed for journeyId={}", journey.getId());
            return Set.of();
        }
    }

    private Set<String> resolveSupportPreferenceSignals(MotherJourney journey) {
        if (journey.getBaselinePreferences() == null) return Set.of();
        Set<String> result = new LinkedHashSet<>();
        for (String token : journey.getBaselinePreferences().split(",")) {
            String slug = RecommendationConstants.SUPPORT_PREFERENCE_SIGNALS.get(token.trim());
            if (slug != null) result.add(slug);
        }
        return result;
    }

    private Optional<PregnancyOutcomeEvidence> latestOutcomeForCurrentEpoch(MotherJourney journey) {
        long epoch = currentPregnancyEpochVersion(journey);
        return outcomeEvidenceRepository.findFirstByJourneyIdAfterEpochVersionOrderByRevisionNumberDesc(
                journey.getId(), epoch);
    }

    private long currentPregnancyEpochVersion(MotherJourney journey) {
        if (transitionRepository == null) return 0L;
        return transitionRepository.findLatestPostpartumToPregnancyEpochVersion(journey.getId()).orElse(0L);
    }

    private MotherJourney canonical(UUID ownerUserId, boolean lock) {
        return canonical(ownerUserId, null, lock);
    }

    private MotherJourney canonical(UUID ownerUserId, UUID careGroupId, boolean lock) {
        Optional<MotherJourney> journey = lock ? journeyRepository.findCanonicalForUpdate(ownerUserId)
                : journeyRepository.findCanonical(ownerUserId);
        if (journey.isEmpty() && careGroupMemberRepository != null && careGroupRepository != null) {
            if (careGroupId != null) {
                boolean isMember = careGroupMemberRepository.findByUserIdAndInviteStatus(
                        ownerUserId, com.carebridge.backend.family.entity.InviteStatus.ACCEPTED)
                        .stream()
                        .anyMatch(m -> m.getCareGroupId().equals(careGroupId));
                if (isMember) {
                    var groupOpt = careGroupRepository.findById(careGroupId);
                    if (groupOpt.isPresent()) {
                        UUID groupOwnerId = groupOpt.get().getOwnerUserId();
                        journey = lock ? journeyRepository.findCanonicalForUpdate(groupOwnerId)
                                : journeyRepository.findCanonical(groupOwnerId);
                    }
                }
            }
            if (journey.isEmpty()) {
                var memberships = careGroupMemberRepository.findByUserIdAndInviteStatus(
                        ownerUserId, com.carebridge.backend.family.entity.InviteStatus.ACCEPTED);
                for (var member : memberships) {
                    var groupOpt = careGroupRepository.findById(member.getCareGroupId());
                    if (groupOpt.isPresent()) {
                        UUID groupOwnerId = groupOpt.get().getOwnerUserId();
                        var ownerJourney = lock ? journeyRepository.findCanonicalForUpdate(groupOwnerId)
                                : journeyRepository.findCanonical(groupOwnerId);
                        if (ownerJourney.isPresent()) {
                            journey = ownerJourney;
                            break;
                        }
                    }
                }
            }
        }
        MotherJourney value = journey.orElseThrow(RecommendationException::journeyRequired);
        if (value.getJourneyType() == JourneyType.BABY_CARE) throw RecommendationException.journeyRequired();
        return value;
    }

    private ConsentAssessment assessConsent(MotherJourney journey, UUID ownerUserId, Instant now) {
        List<ConsentGrant> latestRows = consentGrantRepository.findLatestRecommendationGrant(
                ownerUserId, CONSENT_SCOPE, PageRequest.of(0, 1));
        ConsentGrant latest = latestRows.isEmpty() ? null : latestRows.get(0);
        boolean consentValid = latest != null
                && latest.getRevokedAt() == null
                && RecommendationConstants.POLICY_VERSION.equals(latest.getPolicyVersion())
                && latest.getExpiryAt() != null
                && latest.getExpiryAt().isAfter(now)
                && "ACTIVE".equalsIgnoreCase(latest.getStatus());
        if ((journey.getRecommendationProfileStatus() == RecommendationProfileStatus.ACTIVE
                || journey.getRecommendationProfileStatus() == RecommendationProfileStatus.REVIEW_REQUIRED)
                && !consentValid) {
                RecommendationProfileStatus next = latest != null && latest.getRevokedAt() != null
                        ? RecommendationProfileStatus.REVOKED : RecommendationProfileStatus.RECONSENT_REQUIRED;
                clearProfile(journey, next);
                journeyRepository.saveAndFlush(journey);
                auditProfile(next == RecommendationProfileStatus.REVOKED ? "REVOKED" : "EXPIRED", journey, 0,
                        latest == null ? null : latest.getId());
        }
        boolean active = consentValid;
        String state = latest == null ? "NONE" : latest.getRevokedAt() != null ? "REVOKED"
                : !RecommendationConstants.POLICY_VERSION.equals(latest.getPolicyVersion()) ? "POLICY_MISMATCH"
                : latest.getExpiryAt() == null || !latest.getExpiryAt().isAfter(now) ? "EXPIRED" : active ? "ACTIVE" : "NONE";
        return new ConsentAssessment(active, state, latest);
    }

    private RecommendationProfileResponse toProfileResponse(MotherJourney journey, ConsentAssessment assessment) {
        Map<String, Object> envelope = safeMap(journey.getRecommendationProfileJson());
        boolean stored = journey.getRecommendationProfileStatus() == RecommendationProfileStatus.ACTIVE
                || journey.getRecommendationProfileStatus() == RecommendationProfileStatus.REVIEW_REQUIRED;
        int revision = number(envelope.get("revision"));
        Map<String, Object> profile = stored ? safeMap(envelope.get("profile")) : null;
        Map<String, Object> derived = stored ? safeMap(envelope.get("derived")) : null;
        return new RecommendationProfileResponse(
                journey.getRecommendationProfileStatus(),
                enabled && (journey.getRecommendationProfileStatus() == RecommendationProfileStatus.NOT_STARTED
                        || journey.getRecommendationProfileStatus() == RecommendationProfileStatus.REVIEW_REQUIRED
                        || journey.getRecommendationProfileStatus() == RecommendationProfileStatus.RECONSENT_REQUIRED),
                journey.getRecommendationProfileStatus() == RecommendationProfileStatus.ACTIVE,
                stored ? journey.getRecommendationProfileVersion() : 0,
                stored ? revision : 0,
                stored ? journey.getRecommendationProfileCompletedAt() : null,
                new RecommendationProfileResponse.ConsentSummary(assessment.state(), assessment.grant() == null ? null : assessment.grant().getId(),
                        assessment.grant() == null ? null : assessment.grant().getPolicyVersion(),
                        assessment.grant() == null ? null : assessment.grant().getConsentGivenAt(),
                        assessment.grant() == null ? null : assessment.grant().getExpiryAt()),
                profile, derived);
    }

    private void clearProfile(MotherJourney journey, RecommendationProfileStatus status) {
        journey.setRecommendationProfileJson(new LinkedHashMap<>());
        journey.setRecommendationProfileVersion((short) 0);
        journey.setRecommendationProfileCompletedAt(null);
        journey.setRecommendationProfileStatus(status);
    }

    private void synchronizeBmi(MotherJourney journey, ValidatedRecommendationProfile validated) {
        if (bmiObservationSynchronizer == null) {
            throw new IllegalStateException("Recommendation BMI observation synchronizer is required");
        }
        bmiObservationSynchronizer.synchronize(
                journey, validated.submissionId(), validated.profile());
    }

    private void revokeActiveRecommendationGrants(UUID ownerUserId) {
        for (ConsentGrant grant : consentGrantRepository.findRecommendationGrants(ownerUserId, CONSENT_SCOPE)) {
            if (grant.getRevokedAt() == null) {
                Instant now = Instant.now(clock);
                grant.setRevokedAt(now);
                grant.setRevokedBy(ownerUserId);
                grant.setStatus("REVOKED");
                consentGrantRepository.save(grant);
            }
        }
    }

    private void auditProfile(String eventKind, MotherJourney journey, int revision, Long consentGrantId) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("eventKind", eventKind);
        details.put("profileSchemaVersion", (int) journey.getRecommendationProfileVersion());
        details.put("profileRevision", revision);
        details.put("policyVersion", RecommendationConstants.POLICY_VERSION);
        details.put("journeyId", journey.getId().toString());
        details.put("profileStatus", journey.getRecommendationProfileStatus().name());
        details.put("occurredAt", Instant.now(clock).toString());
        details.put("correlationId", UUID.randomUUID().toString());
        if (consentGrantId != null) details.put("consentGrantId", consentGrantId);
        auditService.log(AuditAction.PROFILE_UPDATED, journey.getOwnerUserId(), "MotherJourney", journey.getId().toString(), details);
    }

    private void auditConsent(String eventKind, MotherJourney journey, int revision, Long consentGrantId) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("eventKind", eventKind);
        details.put("profileSchemaVersion", (int) journey.getRecommendationProfileVersion());
        details.put("profileRevision", revision);
        details.put("policyVersion", RecommendationConstants.POLICY_VERSION);
        details.put("consentGrantId", consentGrantId);
        details.put("journeyId", journey.getId().toString());
        details.put("profileStatus", journey.getRecommendationProfileStatus().name());
        details.put("occurredAt", Instant.now(clock).toString());
        details.put("correlationId", UUID.randomUUID().toString());
        auditService.log(AuditAction.CONSENT_GRANTED, journey.getOwnerUserId(), "MotherJourney", journey.getId().toString(), details);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> safeMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((key, item) -> result.put(String.valueOf(key), item));
            return result;
        }
        return new LinkedHashMap<>();
    }

    private String canonicalJson(Object value) {
        try { return objectMapper.writeValueAsString(canonicalize(value)); } catch (Exception ex) { return ""; }
    }

    private Object canonicalize(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> sorted = new TreeMap<>();
            map.forEach((key, item) -> sorted.put(String.valueOf(key), canonicalize(item)));
            return sorted;
        }
        if (value instanceof List<?> list) {
            return list.stream().map(this::canonicalize).toList();
        }
        return value;
    }

    private int number(Object value) { return value instanceof Number n ? n.intValue() : value == null ? 0 : Integer.parseInt(value.toString()); }
    private Long longId(Object value) {
        try { return value == null ? null : value instanceof Number n ? n.longValue() : Long.parseLong(value.toString()); }
        catch (RuntimeException ex) { return null; }
    }
    private UUID uuidFrom(Object value) { try { return value == null ? null : UUID.fromString(value.toString()); } catch (RuntimeException ex) { return null; } }

    private record ConsentAssessment(boolean active, String state, ConsentGrant grant) {}
}
