package com.carebridge.backend.content.service;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.content.dto.request.ContentDecisionRequest;
import com.carebridge.backend.content.dto.response.ContentDecisionResponse;
import com.carebridge.backend.content.entity.ContentDecision;
import com.carebridge.backend.content.entity.ContentItem;
import com.carebridge.backend.content.entity.ContentStatus;
import com.carebridge.backend.content.exception.ContentException;
import com.carebridge.backend.content.repository.ContentRepository;
import com.carebridge.backend.community.repository.CommunityTopicRepository;
import com.carebridge.backend.community.entity.TopicType;
import com.carebridge.backend.recommendation.RecommendationConstants;
import com.carebridge.backend.recommendation.service.RecommendationMetadataPolicy;
import com.carebridge.backend.notification.service.ContentReviewNotificationService;
import java.security.Principal;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

@Service
@RequiredArgsConstructor
public class ContentApprovalServiceImpl implements ContentApprovalService {

    private final ContentRepository contentRepository;
    private final AuditService auditService;
    private final com.carebridge.backend.aimoderation.service.AiScanEnqueueService aiScanEnqueueService;
    private final ContentReviewNotificationService contentReviewNotificationService;

    @Autowired(required = false)
    private CommunityTopicRepository communityTopicRepository;

    @Override
    @Transactional
    public ContentDecisionResponse decide(UUID id, ContentDecisionRequest request, Principal principal) {
        UUID adminUserId = SecurityUtils.requireCurrentUserId(principal);

        ContentItem item = contentRepository.findById(id)
                .orElseThrow(ContentException::contentNotFound);

        // ADR-003: only PENDING_REVIEW items can be decided
        if (item.getStatus() != ContentStatus.PENDING_REVIEW) {
            throw ContentException.notPendingReview();
        }

        // ADR-005: REJECT requires a non-blank reason; APPROVE reason is optional
        if (request.decision() == ContentDecision.REJECT
                && (request.reason() == null || request.reason().isBlank())) {
            throw ContentException.decisionReasonRequired();
        }

        if (request.decision() == ContentDecision.APPROVE) {
            boolean recommendationMetadataPresent = item.getTagIds() != null && item.getTagIds().stream()
                    .anyMatch(tagId -> RecommendationConstants.ALL_TAG_SLUGS.stream()
                            .map(RecommendationConstants::catalogIdFor)
                            .anyMatch(tagId::equals))
                    || item.getEligibleFromWeek() != null
                    || item.getEligibleToWeek() != null
                    || item.getRecommendationPriority() != null && item.getRecommendationPriority() != 0;
            if (communityTopicRepository == null && recommendationMetadataPresent) {
                throw ContentException.validationFailed(
                        "recommendationMetadata", "Recommendation catalog is unavailable; approval is fail-closed");
            }
            if (communityTopicRepository != null) {
                var tagIds = item.getTagIds() == null ? java.util.List.<UUID>of() : item.getTagIds();
                var uniqueTagIds = new java.util.LinkedHashSet<>(tagIds);
                var tags = communityTopicRepository.findAllById(uniqueTagIds);
                if (tags.size() != uniqueTagIds.size()
                        || tags.stream().anyMatch(tag -> tag.getType() != TopicType.TAG || tag.isHidden())) {
                    throw ContentException.validationFailed(
                            "tagIds", "All tags must exist, be visible, and have type TAG");
                }
                RecommendationMetadataPolicy.validate(item.getType(), item.getStage(),
                        item.getEligibleFromWeek() == null ? null : item.getEligibleFromWeek().intValue(),
                        item.getEligibleToWeek() == null ? null : item.getEligibleToWeek().intValue(),
                        item.getRecommendationPriority() == null ? 0 : item.getRecommendationPriority().intValue(), tags);
            }
        }

        ContentStatus previousStatus = item.getStatus();
        ContentStatus newStatus = request.decision() == ContentDecision.APPROVE
                ? ContentStatus.APPROVED
                : ContentStatus.DRAFT;
        item.setStatus(newStatus);

        Instant decidedAt = Instant.now();
        if (request.decision() == ContentDecision.REJECT) {
            item.setRevisionReason(request.reason().trim());
            item.setRevisionRequestedAt(decidedAt);
            item.setRevisionRequestedBy(adminUserId);
            item.setRevisionRequestedVersion(item.getVersionNo());
        } else {
            clearReviewFeedback(item);
        }

        // TDS §6.1: on APPROVE, set publishedAt if not already set — searchByFilters() orders by
        // publishedAt DESC NULLS LAST, so a null value here would sink newly-approved content to the
        // bottom of the public feed instead of surfacing it.
        if (request.decision() == ContentDecision.APPROVE && item.getPublishedAt() == null) {
            item.setPublishedAt(Instant.now());
        }

        ContentItem saved = contentRepository.save(item);

        // CB-MOD-IMP-016: published library content enters the AI moderation scan queue in the
        // same transaction — publication itself never waits on Gemini.
        if (request.decision() == ContentDecision.APPROVE) {
            aiScanEnqueueService.enqueueScan(com.carebridge.backend.content.entity.ReportTargetType.CONTENT,
                    saved.getId(),
                    com.carebridge.backend.aimoderation.service.AiScanTargetResolver
                            .joinTitleAndBody(saved.getTitle(), saved.getBody()));
        }

        String auditDetail = "decision=" + request.decision() + " versionNo=" + saved.getVersionNo()
                + (request.reason() != null ? " reason=" + request.reason() : "");
        auditService.log(AuditAction.CONTENT_DECIDED, adminUserId,
                "ContentItem", saved.getId().toString(), auditDetail);

        if (request.decision() == ContentDecision.REJECT) {
            contentReviewNotificationService.notifyReturned(
                    saved.getAuthorUserId(), saved.getId(), saved.getType().name(), saved.getTitle(),
                    request.reason().trim(), "/content/" + saved.getId() + "/edit");
        }

        return new ContentDecisionResponse(
                saved.getId(), previousStatus, saved.getStatus(), saved.getVersionNo(),
                adminUserId, request.reason(), decidedAt);
    }

    private void clearReviewFeedback(ContentItem item) {
        item.setRevisionReason(null);
        item.setRevisionRequestedAt(null);
        item.setRevisionRequestedBy(null);
        item.setRevisionRequestedVersion(null);
    }
}
