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
import com.carebridge.backend.notification.service.ContentReviewNotificationService;
import java.security.Principal;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContentApprovalServiceImpl implements ContentApprovalService {

    private final ContentRepository contentRepository;
    private final AuditService auditService;
    private final com.carebridge.backend.aimoderation.service.AiScanEnqueueService aiScanEnqueueService;
    private final ContentReviewNotificationService contentReviewNotificationService;

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
