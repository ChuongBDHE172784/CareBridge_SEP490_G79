package com.carebridge.backend.content.service;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.audit.entity.AuditLog;
import com.carebridge.backend.audit.repository.AuditLogRepository;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.community.repository.CommunityTopicRepository;
import com.carebridge.backend.community.entity.TopicType;
import com.carebridge.backend.content.dto.request.CreateContentRequest;
import com.carebridge.backend.content.dto.request.HideContentRequest;
import com.carebridge.backend.content.dto.request.UpdateContentRequest;
import com.carebridge.backend.content.dto.response.CreateContentResponse;
import com.carebridge.backend.content.dto.response.HideContentResponse;
import com.carebridge.backend.content.dto.response.UpdateContentResponse;
import com.carebridge.backend.content.entity.ContentItem;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.entity.ContentStatus;
import com.carebridge.backend.content.entity.ContentType;
import com.carebridge.backend.content.entity.ContentSource;
import com.carebridge.backend.content.exception.ContentException;
import com.carebridge.backend.content.mapper.ContentMapper;
import com.carebridge.backend.content.policy.HtmlContentSanitizer;
import com.carebridge.backend.content.repository.ContentRepository;
import java.security.Principal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.carebridge.backend.content.dto.response.ContentDetailResponse;
import com.carebridge.backend.content.dto.response.StaffContentDetailResponse;
import com.carebridge.backend.content.dto.response.ContentVersionSnapshotResponse;
import java.util.List;
import java.util.Set;
import java.util.LinkedHashSet;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class AdminContentServiceImpl implements AdminContentService {

    private final ContentRepository contentRepository;
    private final CommunityTopicRepository communityTopicRepository;
    private final ContentMapper contentMapper;
    private final AuditService auditService;
    private final HtmlContentSanitizer htmlContentSanitizer;
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<StaffContentDetailResponse> getStaffContents(
            ContentStatus status, ContentType type, ContentStage stage, String keyword, Pageable pageable) {
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        Page<ContentItem> items = contentRepository.findByAdminFilters(type, stage, status, normalizedKeyword, pageable);
        return items.map(contentMapper::toStaffDetailResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public StaffContentDetailResponse getStaffContent(UUID id) {
        return contentMapper.toStaffDetailResponse(contentRepository.findById(id)
                .orElseThrow(ContentException::contentNotFound));
    }

    @Override
    @Transactional
    public CreateContentResponse createContent(CreateContentRequest request, java.util.UUID authorUserId) {
        if (request.getTopicId() != null
                && !communityTopicRepository.existsById(request.getTopicId())) {
            throw ContentException.topicNotFound(request.getTopicId().toString());
        }

        contentRepository.findByTitleIgnoreCaseAndStageAndType(
                        request.getTitle(), request.getStage(), request.getType())
                .ifPresent(existing -> {
                    throw ContentException.duplicateContent();
                });

        ContentItem entity = contentMapper.toEntity(request, authorUserId);
        entity.setTagIds(validateTagIds(request.getTagIds()));
        // ADR-RTE-005: body is rendered unescaped (web dangerouslySetInnerHTML, mobile
        // flutter_html) — must be sanitized before persisting, not just at render time.
        entity.setBody(htmlContentSanitizer.sanitize(request.getBody()));
        entity = contentRepository.save(entity);

        auditService.log(AuditAction.CONTENT_CREATED, authorUserId,
                "ContentItem", entity.getId().toString(), snapshotOf(entity));

        return contentMapper.toCreateResponse(entity);
    }

    @Override
    @Transactional
    public UpdateContentResponse updateContent(UUID id, UpdateContentRequest request, Principal principal) {
        UUID adminUserId = SecurityUtils.requireCurrentUserId(principal);

        ContentItem item = contentRepository.findById(id)
                .orElseThrow(ContentException::contentNotFound);

        // Publication is a System Admin review decision.  Content authors may only work on
        // an unpublished draft or submit that draft for review; accepting APPROVED here
        // would let a Content Admin bypass the separation-of-duties gate.
        if (item.getStatus() != ContentStatus.DRAFT && item.getStatus() != ContentStatus.PENDING_REVIEW) {
            throw ContentException.invalidContentStatusTransition();
        }
        if (request.status() != ContentStatus.DRAFT && request.status() != ContentStatus.PENDING_REVIEW) {
            throw ContentException.invalidContentStatusTransition();
        }

        if (request.topicId() != null && !communityTopicRepository.existsById(request.topicId())) {
            throw ContentException.topicNotFound(request.topicId().toString());
        }

        // ADR-004: duplicate check (CNT-002) only re-applied when title/stage actually changed —
        // type is immutable via this endpoint, so it is always the existing entity's type
        boolean titleOrStageChanged = !request.title().equalsIgnoreCase(item.getTitle())
                || request.stage() != item.getStage();
        if (titleOrStageChanged) {
            contentRepository.findByTitleIgnoreCaseAndStageAndType(request.title(), request.stage(), item.getType())
                    .filter(other -> !other.getId().equals(id))
                    .ifPresent(other -> {
                        throw ContentException.duplicateContent();
                    });
        }

        // BR-CNT-006: only title/body/stage/status/topicId/sourceLabel editable — type/authorUserId immutable
        item.setTitle(request.title());
        // ADR-RTE-005: see note in createContent() above.
        item.setBody(htmlContentSanitizer.sanitize(request.body()));
        item.setStage(request.stage());
        item.setTopicId(request.topicId());
        if (request.tagIds() != null) {
            item.setTagIds(validateTagIds(request.tagIds()));
        }
        item.setStatus(request.status());
        if (request.status() == ContentStatus.PENDING_REVIEW) {
            clearReviewFeedback(item);
        }
        item.setSourceLabel(request.sourceLabel());
        // Omitted sources mean the client did not edit them. An explicit [] intentionally clears them.
        if (request.sources() != null) {
            // Must stay mutable: Hibernate manages @ElementCollection fields in place and
            // throws UnsupportedOperationException on the next flush/merge if handed an
            // immutable list (Stream.toList()) instead of a real ArrayList.
            item.setSources(request.sources().stream()
                    .map(s -> new ContentSource(s.title(), s.url(), s.publisher()))
                    .collect(Collectors.toCollection(ArrayList::new)));
        }

        // ADR-002: versionNo += 1 on every successful update; null (legacy row) treated as starting at 1
        int currentVersion = item.getVersionNo() == null ? 1 : item.getVersionNo();
        item.setVersionNo(currentVersion + 1);

        ContentItem saved = contentRepository.save(item);

        auditService.log(AuditAction.CONTENT_UPDATED, adminUserId,
                "ContentItem", saved.getId().toString(), snapshotOf(saved));

        return new UpdateContentResponse(
                saved.getId(), saved.getType(), saved.getTitle(), saved.getBody(), saved.getStage(),
                saved.getTopicId(), saved.getStatus(), saved.getVersionNo(), saved.getUpdatedAt());
    }

    private List<UUID> validateTagIds(List<UUID> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return new ArrayList<>();
        }
        LinkedHashSet<UUID> uniqueTagIds = new LinkedHashSet<>(tagIds);
        List<UUID> validatedTagIds = communityTopicRepository
                .findAllByIdInAndTypeAndIsHiddenFalse(uniqueTagIds, TopicType.TAG).stream()
                .map(tag -> tag.getId())
                .toList();
        if (validatedTagIds.size() != uniqueTagIds.size()) {
            throw ContentException.validationFailed("tagIds", "All tags must exist, be visible, and have type TAG");
        }
        return new ArrayList<>(uniqueTagIds);
    }

    private void clearReviewFeedback(ContentItem item) {
        item.setRevisionReason(null);
        item.setRevisionRequestedAt(null);
        item.setRevisionRequestedBy(null);
        item.setRevisionRequestedVersion(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContentVersionSnapshotResponse> getVersionHistory(UUID id) {
        if (!contentRepository.existsById(id)) {
            throw ContentException.contentNotFound();
        }
        return auditLogRepository.findByEntityIdAndEntityTypeAndActionInOrderByCreatedAtDesc(
                        id, "ContentItem", Set.of(AuditAction.CONTENT_CREATED, AuditAction.CONTENT_UPDATED)).stream()
                .map(this::toVersionResponse)
                .flatMap(java.util.Optional::stream)
                .toList();
    }

    private ContentVersionSnapshotResponse snapshotOf(ContentItem item) {
        return new ContentVersionSnapshotResponse(item.getVersionNo(), item.getTitle(),
                item.getStage() == null ? null : item.getStage().name(), item.getStatus().name(),
                item.getSources().isEmpty() ? item.getSourceLabel()
                        : item.getSources().stream().map(ContentSource::getTitle).collect(Collectors.joining(", ")),
                null, Instant.now());
    }

    private java.util.Optional<ContentVersionSnapshotResponse> toVersionResponse(AuditLog auditLog) {
        try {
            ContentVersionSnapshotResponse snapshot = objectMapper.readValue(
                    auditLog.getNewValueJson(), ContentVersionSnapshotResponse.class);
            return java.util.Optional.of(new ContentVersionSnapshotResponse(snapshot.versionNo(), snapshot.title(),
                    snapshot.stage(), snapshot.status(), snapshot.sourceSummary(), auditLog.getActorUserId(), auditLog.getCreatedAt()));
        } catch (Exception ignored) {
            return java.util.Optional.empty();
        }
    }

    @Override
    @Transactional
    public HideContentResponse hideContent(UUID id, HideContentRequest request, Principal principal) {
        UUID adminUserId = SecurityUtils.requireCurrentUserId(principal);

        ContentItem item = contentRepository.findById(id)
                .orElseThrow(ContentException::contentNotFound);

        // ADR-002: idempotency guard — already ARCHIVED is rejected, not silently re-applied
        if (item.getStatus() == ContentStatus.ARCHIVED) {
            throw ContentException.alreadyArchived();
        }

        // ADR-005: reason required (accountability for soft-delete)
        if (request.reason() == null || request.reason().isBlank()) {
            throw ContentException.hideReasonRequired();
        }

        ContentStatus previousStatus = item.getStatus();
        // ADR-001: only status changes — soft-delete via existing ARCHIVED value, no hard delete,
        // no other field touched
        item.setStatus(ContentStatus.ARCHIVED);
        clearReviewFeedback(item);
        ContentItem saved = contentRepository.save(item);

        Instant hiddenAt = Instant.now();
        auditService.log(AuditAction.CONTENT_HIDDEN, adminUserId,
                "ContentItem", saved.getId().toString(), "reason=" + request.reason() + " previousStatus=" + previousStatus);

        return new HideContentResponse(
                saved.getId(), previousStatus, saved.getStatus(), request.reason(), adminUserId, hiddenAt);
    }
}
