package com.carebridge.backend.content.service;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.content.dto.request.ContentDecisionRequest;
import com.carebridge.backend.content.dto.response.ChecklistTemplateDecisionResponse;
import com.carebridge.backend.content.entity.ChecklistTemplate;
import com.carebridge.backend.content.entity.ChecklistTemplateStatus;
import com.carebridge.backend.content.entity.ContentDecision;
import com.carebridge.backend.content.exception.ContentException;
import com.carebridge.backend.content.repository.ChecklistTemplateRepository;
import java.security.Principal;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// UC-243 §14 addendum — mirrors ContentApprovalServiceImpl, kept separate because ChecklistTemplate
// has neither versionNo nor publishedAt (see ChecklistTemplateApprovalService javadoc).
@Service
@RequiredArgsConstructor
public class ChecklistTemplateApprovalServiceImpl implements ChecklistTemplateApprovalService {

    private final ChecklistTemplateRepository checklistTemplateRepository;
    private final AuditService auditService;

    @Override
    @Transactional
    public ChecklistTemplateDecisionResponse decide(UUID id, ContentDecisionRequest request, Principal principal) {
        UUID adminUserId = SecurityUtils.requireCurrentUserId(principal);

        ChecklistTemplate template = checklistTemplateRepository.findById(id)
                .orElseThrow(ContentException::checklistTemplateNotFound);

        if (template.getStatus() != ChecklistTemplateStatus.PENDING_REVIEW) {
            throw ContentException.checklistTemplateNotPendingReview();
        }

        if (request.decision() == ContentDecision.REJECT
                && (request.reason() == null || request.reason().isBlank())) {
            throw ContentException.checklistTemplateDecisionReasonRequired();
        }

        ChecklistTemplateStatus previousStatus = template.getStatus();
        ChecklistTemplateStatus newStatus = request.decision() == ContentDecision.APPROVE
                ? ChecklistTemplateStatus.APPROVED
                : ChecklistTemplateStatus.DRAFT;
        template.setStatus(newStatus);
        ChecklistTemplate saved = checklistTemplateRepository.save(template);

        Instant decidedAt = Instant.now();
        String auditDetail = "decision=" + request.decision()
                + (request.reason() != null ? " reason=" + request.reason() : "");
        auditService.log(AuditAction.CHECKLIST_TEMPLATE_DECIDED, adminUserId,
                "ChecklistTemplate", saved.getId().toString(), auditDetail);

        return new ChecklistTemplateDecisionResponse(
                saved.getId(), previousStatus, saved.getStatus(), adminUserId, request.reason(), decidedAt);
    }
}
