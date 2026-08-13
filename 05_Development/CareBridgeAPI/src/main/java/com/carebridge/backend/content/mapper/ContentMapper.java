package com.carebridge.backend.content.mapper;

import com.carebridge.backend.checklist.model.ChecklistRecipientRole;
import com.carebridge.backend.content.dto.response.AdminChecklistTemplateResponse;
import com.carebridge.backend.content.dto.request.CreateContentRequest;
import com.carebridge.backend.content.dto.response.AdminChecklistTemplateDetailResponse;
import com.carebridge.backend.content.dto.response.ChecklistItemResponse;
import com.carebridge.backend.content.dto.response.ChecklistProvenanceResponse;
import com.carebridge.backend.content.dto.response.ChecklistSubstageResponse;
import com.carebridge.backend.content.dto.response.ChecklistTemplateResponse;
import com.carebridge.backend.content.dto.response.ContentDetailResponse;
import com.carebridge.backend.content.dto.response.ContentListResponse;
import com.carebridge.backend.content.dto.response.ContentSearchResponse;
import com.carebridge.backend.content.dto.response.CreateContentResponse;
import com.carebridge.backend.content.dto.response.ReviewFeedbackResponse;
import com.carebridge.backend.content.dto.response.StaffContentDetailResponse;
import com.carebridge.backend.content.entity.ChecklistItem;
import com.carebridge.backend.content.entity.ChecklistTemplate;
import com.carebridge.backend.content.entity.ContentItem;
import com.carebridge.backend.content.entity.ContentStatus;
import com.carebridge.backend.content.entity.ContentSource;
import com.carebridge.backend.community.entity.CommunityTopic;
import com.carebridge.backend.community.repository.CommunityTopicRepository;
import com.carebridge.backend.recommendation.RecommendationConstants;
import com.carebridge.backend.checklist.model.ChecklistCareContextType;
import com.carebridge.backend.checklist.model.ChecklistMaterializationPolicy;
import com.carebridge.backend.checklist.model.ChecklistScheduleEndMode;
import com.carebridge.backend.checklist.model.ChecklistScheduleType;
import com.carebridge.backend.checklist.model.ChecklistWeekBoundaryRule;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Set;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ContentMapper {

    private final CommunityTopicRepository communityTopicRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private volatile Set<UUID> discoveredRecommendationTagIds;
    private volatile boolean recommendationLookupFailed;

    /** Compatibility constructor used by mapper/unit tests without Spring. */
    public ContentMapper() {
        this(null);
    }

    @Autowired
    public ContentMapper(CommunityTopicRepository communityTopicRepository) {
        this.communityTopicRepository = communityTopicRepository;
    }

    private static final Set<java.util.UUID> RECOMMENDATION_TAG_IDS =
            RecommendationConstants.ALL_TAG_SLUGS.stream()
                    .map(RecommendationConstants::catalogIdFor)
                    .collect(Collectors.toUnmodifiableSet());

    public ContentItem toEntity(CreateContentRequest request, java.util.UUID authorUserId) {
        return ContentItem.builder()
                .type(request.getType())
                .title(request.getTitle())
                .body(request.getBody())
                .summary(request.getSummary())
                .stage(request.getStage())
                .topicId(request.getTopicId())
                .eligibleFromWeek(request.getEligibleFromWeek() == null ? null : request.getEligibleFromWeek().shortValue())
                .eligibleToWeek(request.getEligibleToWeek() == null ? null : request.getEligibleToWeek().shortValue())
                .recommendationPriority(request.getRecommendationPriority() == null ? (short) 0 : request.getRecommendationPriority().shortValue())
                .tagIds(new ArrayList<>())
                .status(ContentStatus.DRAFT)
                .versionNo(1)
                .authorUserId(authorUserId)
                .sources(request.getSources() == null ? new ArrayList<>() : request.getSources().stream()
                        .map(s -> new ContentSource(s.title(), s.url(), s.publisher()))
                        .collect(Collectors.toCollection(ArrayList::new)))
                .build();
    }

    public CreateContentResponse toCreateResponse(ContentItem entity) {
        return CreateContentResponse.builder()
                .id(entity.getId())
                .type(entity.getType())
                .title(entity.getTitle())
                .stage(entity.getStage())
                .status(entity.getStatus().name())
                .version(entity.getVersionNo())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    // BR-PRIVACY: authorId excluded
    public ContentListResponse toListResponse(ContentItem item) {
        return ContentListResponse.builder()
                .id(item.getId())
                .type(item.getType())
                .title(item.getTitle())
                .summary(item.getSummary())
                .stage(item.getStage())
                .topicId(item.getTopicId())
                // Controlled recommendation identities are editorial metadata;
                // keep ordinary community tags available to the existing browse
                // filters without exposing rec-* UUIDs to public clients.
                .tagIds(item.getTagIds() == null ? List.of() : item.getTagIds().stream()
                        .filter(id -> !isRecommendationTag(id))
                        .toList())
                .publishedAt(item.getPublishedAt())
                .build();
    }

    /**
     * Public projections must hide every recommendation namespace row, including
     * retired/malformed rows whose UUID is not present in the active V1 catalog.
     * The static catalog set avoids a lookup for the normal path; the repository
     * fallback closes the privacy gap for unknown rec-* UUIDs and is cached for
     * the lifetime of this mapper instance.
     */
    private boolean isRecommendationTag(UUID id) {
        if (id == null || RECOMMENDATION_TAG_IDS.contains(id)) return true;
        if (communityTopicRepository == null) return false;
        if (recommendationLookupFailed) return true;
        Set<UUID> discovered = discoveredRecommendationTagIds;
        if (discovered == null) {
            synchronized (this) {
                discovered = discoveredRecommendationTagIds;
                if (discovered == null) {
                    try {
                        discovered = communityTopicRepository.findAllBySlugStartingWith("rec-").stream()
                                .filter(topic -> topic.getSlug() != null && topic.getSlug().startsWith("rec-"))
                                .map(CommunityTopic::getId)
                                .collect(Collectors.toUnmodifiableSet());
                        discoveredRecommendationTagIds = discovered;
                    } catch (RuntimeException ex) {
                        recommendationLookupFailed = true;
                        return true;
                    }
                }
            }
        }
        return discovered.contains(id);
    }

    // BR-PRIVACY: authorId excluded
    public ContentDetailResponse toDetailResponse(ContentItem item) {
        return toDetailResponse(item, false);
    }

    private ContentDetailResponse toDetailResponse(ContentItem item, boolean includeRecommendationMetadata) {
        Instant updatedAt = item.getUpdatedAt();
        boolean contentStale = updatedAt != null
                && updatedAt.isBefore(Instant.now().minus(365, ChronoUnit.DAYS));
        return ContentDetailResponse.builder()
                .id(item.getId())
                .type(item.getType())
                .title(item.getTitle())
                .body(item.getBody())
                .summary(item.getSummary())
                .stage(item.getStage())
                .topicId(item.getTopicId())
                .eligibleFromWeek(includeRecommendationMetadata ? item.getEligibleFromWeek() : null)
                .eligibleToWeek(includeRecommendationMetadata ? item.getEligibleToWeek() : null)
                .recommendationPriority(includeRecommendationMetadata ? item.getRecommendationPriority() : null)
                .tagIds(includeRecommendationMetadata
                        ? (item.getTagIds() == null ? List.of() : List.copyOf(item.getTagIds()))
                        : null)
                .version(item.getVersionNo())
                .status(item.getStatus())
                .sourceLabel(item.getSourceLabel())
                .publishedAt(item.getPublishedAt())
                .updatedAt(updatedAt)
                .createdAt(item.getCreatedAt())
                .sources(item.getSources() == null ? List.of() : item.getSources().stream()
                        .map(s -> new com.carebridge.backend.content.dto.response.ContentSourceResponse(
                                s.getTitle(), s.getUrl(), s.getPublisher()))
                        .toList())
                .contentStale(contentStale)
                .build();
    }

    public StaffContentDetailResponse toStaffDetailResponse(ContentItem item) {
        return new StaffContentDetailResponse(
                toDetailResponse(item, true),
                toReviewFeedback(
                        item.getRevisionReason(), item.getRevisionRequestedAt(),
                        item.getRevisionRequestedBy(), item.getRevisionRequestedVersion()));
    }

    public ChecklistTemplateResponse toChecklistTemplateResponse(
            ChecklistTemplate template, List<ChecklistItem> items) {
        List<ChecklistItemResponse> itemResponses = items.stream()
                .map(this::toChecklistItemResponse)
                .toList();
        CadenceProjection cadence = cadenceProjection(template);
        return ChecklistTemplateResponse.builder()
                .id(template.getId())
                .name(template.getName())
                .stage(template.getStage())
                .description(template.getDescription())
                .templateType(template.getTemplateType())
                .checklistContractVersion(template.getChecklistContractVersion())
                .planNumber(cadence.planNumber())
                .section(cadence.section())
                .scheduleType(template.getScheduleType())
                .materializationPolicy(template.getMaterializationPolicy())
                .scheduleGroupKey(template.getScheduleGroupKey())
                .scheduleContextType(template.getScheduleContextType())
                .scheduleEndMode(template.getScheduleEndMode())
                .weekBoundaryRule(template.getWeekBoundaryRule())
                .eligibilityStartInclusive(template.getEligibilityStartInclusive())
                .eligibilityEndInclusive(template.getEligibilityEndInclusive())
                .items(itemResponses)
                .build();
    }

    /** Maps the metadata-only admin list projection without exposing item bodies. */
    public AdminChecklistTemplateResponse toAdminChecklistTemplateResponse(
            ChecklistTemplate template, long itemCount) {
        CadenceProjection cadence = cadenceProjection(template);
        return new AdminChecklistTemplateResponse(
                template.getId(), template.getName(), template.getStage(), template.getTemplateType(),
                template.getStatus(), template.getDescription(), template.getVersionNo(), template.getUpdatedAt(),
                itemCount,
                toReviewFeedback(template.getRevisionReason(), template.getRevisionRequestedAt(),
                        template.getRevisionRequestedBy(), template.getRevisionRequestedVersion()),
                template.getSequencePosition(), toRecipientRoles(template), template.getChecklistContractVersion(),
                cadence.planNumber(), cadence.section(), template.getScheduleType(),
                template.getMaterializationPolicy(), template.getScheduleGroupKey(),
                template.getScheduleContextType(), template.getScheduleEndMode(), template.getWeekBoundaryRule(),
                template.getEligibilityStartInclusive(), template.getEligibilityEndInclusive(),
                template.getChecklistQuarantineReasonCode(), provenanceStatus(template));
    }

    public AdminChecklistTemplateDetailResponse toAdminChecklistTemplateDetailResponse(
            ChecklistTemplate template, List<ChecklistItem> items) {
        List<ChecklistItemResponse> itemResponses = items.stream()
                .map(this::toChecklistItemResponse)
                .toList();
        CadenceProjection cadence = cadenceProjection(template);
        return AdminChecklistTemplateDetailResponse.builder()
                .id(template.getId())
                .name(template.getName())
                .stage(template.getStage())
                .displayOrder(template.getSequencePosition())
                .status(template.getStatus())
                .description(template.getDescription())
                .versionNo(template.getVersionNo())
                .lineageId(template.getTemplateLineageId())
                .versionId(template.getTemplateVersionId())
                .recipientRoles(toRecipientRoles(template))
                .substage(toChecklistSubstageResponse(template))
                .migrationReviewRequired(template.getMigrationReviewRequired())
                .distributionEnabled(template.getDistributionEnabled())
                .templateType(template.getTemplateType())
                .approvedAt(template.getApprovedAt())
                .approvedBy(template.getApprovedBy())
                .migrationReviewedAt(template.getMigrationReviewedAt())
                .migrationReviewedBy(template.getMigrationReviewedBy())
                .provenance(toChecklistProvenance(template))
                .checklistContractVersion(template.getChecklistContractVersion())
                .planNumber(cadence.planNumber())
                .section(cadence.section())
                .scheduleType(template.getScheduleType())
                .materializationPolicy(template.getMaterializationPolicy())
                .scheduleGroupKey(template.getScheduleGroupKey())
                .scheduleContextType(template.getScheduleContextType())
                .scheduleEndMode(template.getScheduleEndMode())
                .weekBoundaryRule(template.getWeekBoundaryRule())
                .eligibilityStartInclusive(template.getEligibilityStartInclusive())
                .eligibilityEndInclusive(template.getEligibilityEndInclusive())
                .checklistQuarantineReasonCode(template.getChecklistQuarantineReasonCode())
                .items(itemResponses)
                .latestReviewFeedback(toReviewFeedback(
                        template.getRevisionReason(), template.getRevisionRequestedAt(),
                        template.getRevisionRequestedBy(), template.getRevisionRequestedVersion()))
                .build();
    }

    // BR-PRIVACY: authorId excluded; topicName null for MVP (C5 — deferred cross-module resolution)
    public ContentSearchResponse toSearchResponse(ContentItem item) {
        return ContentSearchResponse.builder()
                .id(item.getId())
                .type(item.getType())
                .title(item.getTitle())
                .stage(item.getStage())
                .topicName(null)
                .publishedAt(item.getPublishedAt())
                .build();
    }

    private ChecklistItemResponse toChecklistItemResponse(ChecklistItem item) {
        boolean repeatWeekly = false;
        boolean repeatDaily = false;
        try {
            JsonNode configuration = item.getConfigurationJson() == null
                    ? null : objectMapper.readTree(item.getConfigurationJson());
            repeatWeekly = configuration != null && configuration.path("repeatWeekly").asBoolean(false);
            repeatDaily = configuration != null && configuration.path("repeatDaily").asBoolean(false);
        } catch (Exception ignored) {
            // Malformed optional authoring metadata must not hide the checklist.
        }
        return ChecklistItemResponse.builder()
                .id(item.getId())
                .itemText(item.getItemText())
                .description(item.getDescription())
                .order(item.getOrder())
                .isRequired(item.getIsRequired())
                .targetSubject(item.getTargetSubject())
                .supportFunction(item.getSupportFunction())
                .repeatWeekly(repeatWeekly)
                .repeatDaily(repeatDaily)
                .build();
    }

    private List<ChecklistRecipientRole> toRecipientRoles(ChecklistTemplate template) {
        if (template.getRecipientScope() == null) {
            return List.of();
        }
        return switch (template.getRecipientScope()) {
            case MOTHER -> List.of(ChecklistRecipientRole.MOTHER);
            case FAMILY -> List.of(ChecklistRecipientRole.FAMILY);
            case BOTH -> List.of(ChecklistRecipientRole.MOTHER, ChecklistRecipientRole.FAMILY);
        };
    }

    private CadenceProjection cadenceProjection(ChecklistTemplate template) {
        if (template == null || template.getChecklistMetadataJson() == null
                || template.getChecklistMetadataJson().isBlank()) {
            return CadenceProjection.EMPTY;
        }
        try {
            JsonNode metadata = objectMapper.readTree(template.getChecklistMetadataJson());
            Integer plan = null;
            JsonNode planNode = metadata == null ? null : metadata.get("plan");
            if (planNode != null && planNode.canConvertToInt() && planNode.asInt() > 0) {
                plan = planNode.asInt();
            }
            String section = null;
            JsonNode sectionNode = metadata == null ? null : metadata.get("section");
            if (sectionNode != null && sectionNode.isTextual()) {
                String candidate = sectionNode.asText().trim();
                if (candidate.equals("COMMON") || candidate.equals("WEEKLY")) {
                    section = candidate;
                }
            }
            return new CadenceProjection(plan, section);
        } catch (Exception ignored) {
            // Metadata is provenance/preview data. A malformed legacy value must
            // not make an otherwise valid checklist unreadable.
            return CadenceProjection.EMPTY;
        }
    }

    private record CadenceProjection(Integer planNumber, String section) {
        private static final CadenceProjection EMPTY = new CadenceProjection(null, null);
    }

    private ChecklistProvenanceResponse toChecklistProvenance(ChecklistTemplate template) {
        if (template == null || template.getChecklistMetadataJson() == null
                || template.getChecklistMetadataJson().isBlank()) {
            return null;
        }
        try {
            JsonNode m = objectMapper.readTree(template.getChecklistMetadataJson());
            if (m == null || !m.isObject()) return null;
            return new ChecklistProvenanceResponse(
                    text(m, "schema"), text(m, "sourceArtifactPath"), text(m, "sourceArtifactSha256"),
                    text(m, "importBatchId"), uuid(m, "importCorrelationId"), text(m, "normalizerId"),
                    text(m, "copyReviewPolicy"), text(m, "provenanceStatus"), text(m, "cadenceReviewStatus"),
                    uuid(m, "cadenceReviewerUserId"), instant(m, "cadenceReviewedAt"),
                    text(m, "reviewAuthorityId"), uuid(m, "copyReviewerUserId"),
                    text(m, "qualificationEvidenceRef"), instant(m, "credentialVerifiedAt"),
                    uuid(m, "contentOwnerUserId"), instant(m, "contentOwnerApprovedAt"),
                    instant(m, "copyReviewedAt"), text(m, "sourceTitle"), text(m, "sourceRelationship"),
                    text(m, "sourceOrganization"), text(m, "sourceVersionOrPublicationDate"),
                    text(m, "sourceUrl"), text(m, "sourceLanguage"), text(m, "renderedLanguage"),
                    text(m, "translationProvenance"), text(m, "priorityNarrative"),
                    text(m, "priorityNarrativeMode"), text(m, "sourceLocator"),
                    text(m, "renderedManifestSchema"), text(m, "renderedManifestCanonicalization"),
                    text(m, "renderedManifestHash"), text(m, "validityMode"), text(m, "validUntil"),
                    text(m, "revokedAt"));
        } catch (Exception ignored) {
            return null;
        }
    }

    private String provenanceStatus(ChecklistTemplate template) {
        ChecklistProvenanceResponse provenance = toChecklistProvenance(template);
        return provenance == null ? null : provenance.provenanceStatus();
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && value.isTextual() ? value.asText() : null;
    }

    private static UUID uuid(JsonNode node, String field) {
        String value = text(node, field);
        try { return value == null ? null : UUID.fromString(value); }
        catch (IllegalArgumentException ignored) { return null; }
    }

    private static Instant instant(JsonNode node, String field) {
        String value = text(node, field);
        if (value == null) return null;
        try { return Instant.parse(value); }
        catch (RuntimeException ignored) {
            try { return OffsetDateTime.parse(value).toInstant(); }
            catch (RuntimeException ignoredAgain) { return null; }
        }
    }

    private ChecklistSubstageResponse toChecklistSubstageResponse(ChecklistTemplate template) {
        if (template.getStage() == null
                || template.getEligibilityAnchorType() == null
                || template.getEligibilityRangeUnit() == null
                || template.getEligibilityStartInclusive() == null
                || template.getEligibilityEndInclusive() == null) {
            return null;
        }
        String code = template.getStage().name()
                + "_" + template.getEligibilityAnchorType().name()
                + "_" + template.getEligibilityRangeUnit().name()
                + "_" + template.getEligibilityStartInclusive()
                + "_" + template.getEligibilityEndInclusive();
        return new ChecklistSubstageResponse(
                code,
                template.getEligibilityAnchorType(),
                template.getEligibilityStartInclusive(),
                template.getEligibilityEndInclusive(),
                template.getEligibilityRangeUnit());
    }

    public ReviewFeedbackResponse toReviewFeedback(
            String reason, Instant requestedAt, java.util.UUID requestedBy, Integer versionNo) {
        if (reason == null || reason.isBlank()) {
            return null;
        }
        return new ReviewFeedbackResponse(reason, requestedAt, requestedBy, versionNo);
    }
}
