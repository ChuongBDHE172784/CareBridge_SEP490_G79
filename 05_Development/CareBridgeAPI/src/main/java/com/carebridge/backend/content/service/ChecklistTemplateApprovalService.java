package com.carebridge.backend.content.service;

import com.carebridge.backend.content.dto.request.ContentDecisionRequest;
import com.carebridge.backend.content.dto.response.ChecklistTemplateDecisionResponse;
import java.security.Principal;
import java.util.UUID;

/**
 * UC-243 §14 addendum — SYSTEM_ADMIN approval flow for checklist templates, mirroring
 * {@link ContentApprovalService} but kept as a separate service/controller because
 * {@code ChecklistTemplate} has neither {@code versionNo} nor {@code publishedAt}
 * (both read/written by {@code ContentApprovalServiceImpl.decide()}).
 */
public interface ChecklistTemplateApprovalService {

    /**
     * @throws com.carebridge.backend.content.exception.ContentException (CHKTPL-003) if id not found
     * @throws com.carebridge.backend.content.exception.ContentException (CHKTPL-007) if status is not PENDING_REVIEW
     * @throws com.carebridge.backend.content.exception.ContentException (CHKTPL-008) if reason is blank for REJECT
     */
    ChecklistTemplateDecisionResponse decide(UUID id, ContentDecisionRequest request, Principal principal);

    ChecklistTemplateDecisionResponse decideInLineage(
            UUID lineageId, UUID id, ContentDecisionRequest request, Principal principal);

    ChecklistTemplateDecisionResponse reviewImported(UUID id, Principal principal);

    ChecklistTemplateDecisionResponse reviewImportedInLineage(
            UUID lineageId, UUID id, Principal principal);

    ChecklistTemplateDecisionResponse activateImported(UUID id, Principal principal);

    ChecklistTemplateDecisionResponse activateImportedInLineage(
            UUID lineageId, UUID id, Principal principal);
}
