package com.carebridge.backend.content.service;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.audit.entity.AuditLog;
import com.carebridge.backend.audit.repository.AuditLogRepository;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.community.repository.CommunityTopicRepository;
import com.carebridge.backend.community.entity.TopicType;
import com.carebridge.backend.community.entity.CommunityTopic;
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
import com.carebridge.backend.recommendation.service.RecommendationMetadataPolicy;
import java.security.Principal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.carebridge.backend.content.dto.response.StaffContentDetailResponse;
import com.carebridge.backend.content.dto.response.ContentVersionSnapshotResponse;
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
        List<CommunityTopic> tags = validateTagEntities(request.getTagIds());
        short priority = RecommendationMetadataPolicy.validate(request.getType(), request.getStage(),
                request.getEligibleFromWeek(), request.getEligibleToWeek(), request.getRecommendationPriority(), tags);
        entity.setTagIds(tags.stream().map(CommunityTopic::getId).collect(Collectors.toCollection(ArrayList::new)));
        entity.setEligibleFromWeek(request.getEligibleFromWeek() == null ? null : request.getEligibleFromWeek().shortValue());
        entity.setEligibleToWeek(request.getEligibleToWeek() == null ? null : request.getEligibleToWeek().shortValue());
        entity.setRecommendationPriority(priority);
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

        // BR-CNT-006: only title/body/summary/stage/status/topicId/sourceLabel editable — type/authorUserId immutable
        item.setTitle(request.title());
        // ADR-RTE-005: see note in createContent() above.
        item.setBody(htmlContentSanitizer.sanitize(request.body()));
        item.setSummary(request.summary());
        item.setStage(request.stage());
        item.setTopicId(request.topicId());
        List<CommunityTopic> tags = request.tagIds() == null
                ? validateTagEntities(item.getTagIds()) : validateTagEntities(request.tagIds());
        // Recommendation metadata is a full replacement once any recommendation
        // field is supplied.  This lets the editor intentionally clear an old
        // pregnancy window by sending both bounds as null (the normal UI always
        // sends priority), while legacy ordinary-tag-only updates retain the
        // existing recommendation configuration.
        boolean recommendationMetadataSupplied = request.eligibleFromWeek() != null
                || request.eligibleToWeek() != null
                || request.recommendationPriority() != null
                || (request.tagIds() != null
                    && tags.stream().anyMatch(RecommendationMetadataPolicy::isControlled))
                || (request.stage() != ContentStage.PREGNANCY
                    && (item.getEligibleFromWeek() != null || item.getEligibleToWeek() != null));
        Integer fromWeek = request.eligibleFromWeek() != null
                ? request.eligibleFromWeek()
                : recommendationMetadataSupplied ? null
                : item.getEligibleFromWeek() == null ? null : item.getEligibleFromWeek().intValue();
        Integer toWeek = request.eligibleToWeek() != null
                ? request.eligibleToWeek()
                : recommendationMetadataSupplied ? null
                : item.getEligibleToWeek() == null ? null : item.getEligibleToWeek().intValue();
        if (request.stage() != ContentStage.PREGNANCY) {
            fromWeek = null;
            toWeek = null;
        }
        Integer priorityValue = recommendationMetadataSupplied
                ? request.recommendationPriority() == null ? item.getRecommendationPriority() == null ? 0 : item.getRecommendationPriority().intValue() : request.recommendationPriority()
                : item.getRecommendationPriority() == null ? 0 : item.getRecommendationPriority().intValue();
        short priority = RecommendationMetadataPolicy.validate(item.getType(), request.stage(),
                fromWeek, toWeek, priorityValue, tags);
        if (request.tagIds() != null) item.setTagIds(tags.stream().map(CommunityTopic::getId).collect(Collectors.toCollection(ArrayList::new)));
        if (recommendationMetadataSupplied) {
            item.setEligibleFromWeek(fromWeek == null ? null : fromWeek.shortValue());
            item.setEligibleToWeek(toWeek == null ? null : toWeek.shortValue());
            item.setRecommendationPriority(priority);
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
                saved.getTopicId(), saved.getEligibleFromWeek(), saved.getEligibleToWeek(), saved.getRecommendationPriority(),
                saved.getStatus(), saved.getVersionNo(), saved.getUpdatedAt());
    }

    private List<CommunityTopic> validateTagEntities(List<UUID> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<UUID> uniqueTagIds = new LinkedHashSet<>(tagIds);
        List<CommunityTopic> rows = communityTopicRepository.findAllByIdInAndTypeAndIsHiddenFalse(
                uniqueTagIds, TopicType.TAG);
        if (rows.size() != uniqueTagIds.size()) {
            throw ContentException.validationFailed("tagIds", "All tags must exist, be visible, and have type TAG");
        }
        return uniqueTagIds.stream().map(id -> rows.stream().filter(tag -> tag.getId().equals(id)).findFirst().orElseThrow()).toList();
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
                item.getTagIds() == null ? List.of() : new ArrayList<>(item.getTagIds()),
                item.getEligibleFromWeek(), item.getEligibleToWeek(), item.getRecommendationPriority(),
                null, Instant.now());
    }

    private java.util.Optional<ContentVersionSnapshotResponse> toVersionResponse(AuditLog auditLog) {
        try {
            ContentVersionSnapshotResponse snapshot = objectMapper.readValue(
                    auditLog.getNewValueJson(), ContentVersionSnapshotResponse.class);
            return java.util.Optional.of(new ContentVersionSnapshotResponse(snapshot.versionNo(), snapshot.title(),
                    snapshot.stage(), snapshot.status(), snapshot.sourceSummary(), snapshot.tagIds(),
                    snapshot.eligibleFromWeek(), snapshot.eligibleToWeek(), snapshot.recommendationPriority(),
                    auditLog.getActorUserId(), auditLog.getCreatedAt()));
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

    @Override
    @Transactional
    public com.carebridge.backend.content.dto.response.BulkImportResponse importContentBatch(
            com.carebridge.backend.content.dto.request.BulkImportContentRequest request, UUID authorUserId) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            return com.carebridge.backend.content.dto.response.BulkImportResponse.builder()
                    .totalRows(0)
                    .successCount(0)
                    .failedCount(0)
                    .errors(List.of("Danh sách bài viết import không được để trống."))
                    .createdIds(List.of())
                    .build();
        }

        List<CommunityTopic> allTopics = communityTopicRepository.findAll();
        Map<String, UUID> topicNameMap = new HashMap<>();
        for (CommunityTopic t : allTopics) {
            if (t.getName() != null) {
                topicNameMap.put(t.getName().toLowerCase().trim(), t.getId());
            }
            topicNameMap.put(t.getId().toString(), t.getId());
        }

        List<String> errors = new ArrayList<>();
        List<ContentItem> itemsToSave = new ArrayList<>();
        List<UUID> createdIds = new ArrayList<>();
        Set<String> processedTitlesInBatch = new HashSet<>();

        for (com.carebridge.backend.content.dto.request.BulkImportContentRequest.BulkImportItemRequest itemReq : request.getItems()) {
            int rowIdx = itemReq.getRowIndex() > 0 ? itemReq.getRowIndex() : (itemsToSave.size() + errors.size() + 1);
            String rawTitle = itemReq.getTitle() != null ? itemReq.getTitle().trim() : "";
            String rawBody = itemReq.getBody() != null ? itemReq.getBody().trim() : "";
            String rawStage = itemReq.getStage() != null ? itemReq.getStage().trim() : "";

            if (rawTitle.isEmpty()) {
                errors.add("Dòng " + rowIdx + ": Tiêu đề không được để trống.");
                continue;
            }

            if (rawBody.isEmpty()) {
                errors.add("Dòng " + rowIdx + ": Nội dung không được để trống.");
                continue;
            }

            ContentStage stage = parseContentStage(rawStage);
            if (stage == null) {
                errors.add("Dòng " + rowIdx + ": Giai đoạn không hợp lệ. Phải là PRE_PREGNANCY, PREGNANCY hoặc POSTPARTUM.");
                continue;
            }

            // Check duplicate in batch
            String batchKey = rawTitle.toLowerCase() + "||" + stage + "||" + request.getType();
            if (processedTitlesInBatch.contains(batchKey)) {
                errors.add("Dòng " + rowIdx + ": Tiêu đề \"" + rawTitle + "\" bị trùng lặp trong file import.");
                continue;
            }

            // Check duplicate in DB
            if (contentRepository.findByTitleIgnoreCaseAndStageAndType(rawTitle, stage, request.getType()).isPresent()) {
                errors.add("Dòng " + rowIdx + ": Tiêu đề \"" + rawTitle + "\" đã tồn tại trong hệ thống.");
                continue;
            }

            // Unescape HTML entities if CSV parser escaped them once (e.g., &lt;h2&gt; -> <h2>)
            if (rawBody.contains("&lt;") || rawBody.contains("&gt;")) {
                rawBody = org.springframework.web.util.HtmlUtils.htmlUnescape(rawBody);
            }

            String sanitizedBody = htmlContentSanitizer.sanitize(rawBody);

            UUID topicId = itemReq.getTopicId();
            if (topicId == null && itemReq.getCategoryName() != null && !itemReq.getCategoryName().isBlank()) {
                topicId = topicNameMap.get(itemReq.getCategoryName().toLowerCase().trim());
            }
            if (topicId != null && !communityTopicRepository.existsById(topicId)) {
                topicId = null; // if category specified does not exist, do not map to wrong category
            }

            ContentItem entity = new ContentItem();
            entity.setTitle(rawTitle);
            entity.setBody(sanitizedBody);
            entity.setSummary(itemReq.getSummary() != null ? itemReq.getSummary().trim() : null);
            entity.setType(request.getType());
            entity.setStage(stage);
            entity.setStatus(ContentStatus.DRAFT);
            entity.setAuthorUserId(authorUserId);
            entity.setTopicId(topicId);
            entity.setVersionNo(1);
            entity.setCreatedAt(Instant.now());
            entity.setUpdatedAt(Instant.now());

            if (itemReq.getSourceLabel() != null && !itemReq.getSourceLabel().isBlank()) {
                String srcTitle = itemReq.getSourceLabel().trim();
                String srcUrl = itemReq.getSourceUrl() != null && !itemReq.getSourceUrl().isBlank() ? itemReq.getSourceUrl().trim() : null;
                String srcPub = itemReq.getSourcePublisher() != null && !itemReq.getSourcePublisher().isBlank() ? itemReq.getSourcePublisher().trim() : null;
                entity.setSourceLabel(srcTitle);
                entity.getSources().add(new ContentSource(srcTitle, srcUrl, srcPub));
            }

            itemsToSave.add(entity);
            processedTitlesInBatch.add(batchKey);
        }

        if (errors.isEmpty() && !itemsToSave.isEmpty()) {
            List<ContentItem> savedList = contentRepository.saveAll(itemsToSave);
            for (ContentItem saved : savedList) {
                createdIds.add(saved.getId());
                auditService.log(AuditAction.CONTENT_CREATED, authorUserId,
                        "ContentItem", saved.getId().toString(), snapshotOf(saved));
            }
        }

        return com.carebridge.backend.content.dto.response.BulkImportResponse.builder()
                .totalRows(request.getItems().size())
                .successCount(createdIds.size())
                .failedCount(request.getItems().size() - createdIds.size())
                .errors(errors)
                .createdIds(createdIds)
                .build();
    }

    private ContentStage parseContentStage(String rawStage) {
        if (rawStage == null || rawStage.isBlank()) return null;
        String s = rawStage.toUpperCase().replaceAll("\\s+", "_");
        if (s.equals("PRE_PREGNANCY") || s.equals("CHUẨN_BỊ_MANG_THAI") || s.equals("CHUA_BI_MANG_THAI")) {
            return ContentStage.PRE_PREGNANCY;
        }
        if (s.equals("PREGNANCY") || s.equals("MANG_THAI")) {
            return ContentStage.PREGNANCY;
        }
        if (s.equals("POSTPARTUM") || s.equals("SAU_SINH")) {
            return ContentStage.POSTPARTUM;
        }
        try {
            return ContentStage.valueOf(s);
        } catch (Exception e) {
            return null;
        }
    }
}
