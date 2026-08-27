package com.carebridge.backend.content.service;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.content.dto.request.ContentDecisionRequest;
import com.carebridge.backend.content.dto.request.ReassignContentRequest;
import com.carebridge.backend.content.dto.response.ChecklistTemplateDecisionResponse;
import com.carebridge.backend.content.dto.response.ContentDecisionResponse;
import com.carebridge.backend.content.dto.response.ExpertContentApprovalQueueItem;
import com.carebridge.backend.content.entity.ChecklistTemplate;
import com.carebridge.backend.content.entity.ChecklistTemplateStatus;
import com.carebridge.backend.content.entity.ContentItem;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.entity.ContentStatus;
import com.carebridge.backend.content.entity.ContentType;
import com.carebridge.backend.content.exception.ContentException;
import com.carebridge.backend.content.repository.ChecklistItemRepository;
import com.carebridge.backend.content.repository.ChecklistTemplateRepository;
import com.carebridge.backend.content.repository.ContentRepository;
import com.carebridge.backend.expert.entity.ExpertProfile;
import com.carebridge.backend.expert.repository.ExpertProfileRepository;
import java.security.Principal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpertContentApprovalServiceImpl implements ExpertContentApprovalService {

    private final ContentRepository contentRepository;
    private final ChecklistTemplateRepository checklistTemplateRepository;
    private final ChecklistItemRepository checklistItemRepository;
    private final ContentApprovalService contentApprovalService;
    private final ChecklistTemplateApprovalService checklistTemplateApprovalService;
    private final ExpertProfileRepository expertProfileRepository;
    private final AuditService auditService;

    @Override
    @Transactional(readOnly = true)
    public Page<ExpertContentApprovalQueueItem> getAssignedQueue(
            ContentType type, ContentStage stage, String keyword, Pageable pageable, Principal principal) {
        UUID expertId = SecurityUtils.requireCurrentUserId(principal);
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();

        List<ExpertContentApprovalQueueItem> allItems = new ArrayList<>();

        if (type == null || type == ContentType.ARTICLE || type == ContentType.FAQ) {
            Page<ContentItem> contentItems = contentRepository.findByExpertFilters(
                    expertId, ContentStatus.PENDING_REVIEW, type, stage, normalizedKeyword, Pageable.unpaged());
            for (ContentItem item : contentItems) {
                allItems.add(ExpertContentApprovalQueueItem.builder()
                        .id(item.getId())
                        .kind("CONTENT")
                        .type(item.getType())
                        .title(item.getTitle())
                        .stage(item.getStage())
                        .status(item.getStatus().name())
                        .versionNo(item.getVersionNo())
                        .summary(item.getSummary())
                        .sourceLabel(item.getSourceLabel())
                        .assignedAt(item.getAssignedAt())
                        .updatedAt(item.getUpdatedAt())
                        .createdAt(item.getCreatedAt())
                        .build());
            }
        }

        if (type == null || type == ContentType.CHECKLIST) {
            Page<ChecklistTemplate> templates = checklistTemplateRepository.findByExpertFilters(
                    expertId, ChecklistTemplateStatus.PENDING_REVIEW, stage, normalizedKeyword, Pageable.unpaged());
            for (ChecklistTemplate t : templates) {
                long count = checklistItemRepository.countActiveByTemplateId(t.getId());
                allItems.add(ExpertContentApprovalQueueItem.builder()
                        .id(t.getId())
                        .kind("CHECKLIST")
                        .type(ContentType.CHECKLIST)
                        .title(t.getName())
                        .stage(t.getStage())
                        .status(t.getStatus().name())
                        .versionNo(t.getVersionNo())
                        .itemCount(count)
                        .summary(t.getDescription())
                        .assignedAt(t.getAssignedAt())
                        .updatedAt(t.getUpdatedAt())
                        .createdAt(t.getCreatedAt())
                        .build());
            }
        }

        allItems.sort(Comparator.comparing(ExpertContentApprovalQueueItem::getAssignedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ExpertContentApprovalQueueItem::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())));

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), allItems.size());
        List<ExpertContentApprovalQueueItem> pagedList = start >= allItems.size() ? List.of() : allItems.subList(start, end);

        return new PageImpl<>(pagedList, pageable, allItems.size());
    }

    @Override
    @Transactional
    public ContentDecisionResponse decideContent(UUID id, ContentDecisionRequest request, Principal principal) {
        return contentApprovalService.decide(id, request, principal);
    }

    @Override
    @Transactional
    public ChecklistTemplateDecisionResponse decideChecklist(UUID id, ContentDecisionRequest request, Principal principal) {
        return checklistTemplateApprovalService.decide(id, request, principal);
    }

    @Override
    @Transactional
    public void reassignContent(UUID id, ReassignContentRequest request, Principal principal) {
        UUID adminUserId = SecurityUtils.requireCurrentUserId(principal);

        ContentItem item = contentRepository.findById(id)
                .orElseThrow(ContentException::contentNotFound);

        validateContractedExpert(request.expertId());

        UUID previousExpertId = item.getAssignedExpertId();
        item.setAssignedExpertId(request.expertId());
        item.setAssignedAt(Instant.now());
        contentRepository.save(item);

        auditService.log(AuditAction.CONTENT_UPDATED, adminUserId, "ContentItem", item.getId().toString(),
                "Reassigned from expert " + previousExpertId + " to " + request.expertId() + (request.reason() != null ? " reason=" + request.reason() : ""));
        log.info("Content {} reassigned from {} to {} by admin {}", id, previousExpertId, request.expertId(), adminUserId);
    }

    @Override
    @Transactional
    public void reassignChecklist(UUID id, ReassignContentRequest request, Principal principal) {
        UUID adminUserId = SecurityUtils.requireCurrentUserId(principal);

        ChecklistTemplate template = checklistTemplateRepository.findById(id)
                .orElseThrow(ContentException::checklistTemplateNotFound);

        validateContractedExpert(request.expertId());

        UUID previousExpertId = template.getAssignedExpertId();
        template.setAssignedExpertId(request.expertId());
        template.setAssignedAt(Instant.now());
        checklistTemplateRepository.save(template);

        auditService.log(AuditAction.CHECKLIST_TEMPLATE_UPDATED, adminUserId, "ChecklistTemplate", template.getId().toString(),
                "Reassigned from expert " + previousExpertId + " to " + request.expertId() + (request.reason() != null ? " reason=" + request.reason() : ""));
        log.info("ChecklistTemplate {} reassigned from {} to {} by admin {}", id, previousExpertId, request.expertId(), adminUserId);
    }

    private void validateContractedExpert(UUID expertId) {
        ExpertProfile profile = expertProfileRepository.findById(expertId)
                .orElseThrow(() -> new IllegalArgumentException("Expert profile not found: " + expertId));
        if (!profile.isContracted() || !profile.isEligibleForConsultation()) {
            throw new IllegalArgumentException("Expert must be an active CONTRACTED expert");
        }
    }
}
