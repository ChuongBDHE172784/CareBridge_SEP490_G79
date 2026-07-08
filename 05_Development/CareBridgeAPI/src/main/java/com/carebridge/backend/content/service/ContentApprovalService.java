package com.carebridge.backend.content.service;

import com.carebridge.backend.content.dto.request.ContentDecisionRequest;
import com.carebridge.backend.content.dto.response.ContentDecisionResponse;
import java.security.Principal;
import java.util.UUID;

public interface ContentApprovalService {

    /**
     * Approves or rejects a content item currently in PENDING_REVIEW status (ADR-001 Option A —
     * approves the CURRENT edit, not a historical snapshot; no version-history table exists).
     *
     * @throws com.carebridge.backend.content.exception.ContentException (CNT-003) if id not found (reused)
     * @throws com.carebridge.backend.content.exception.ContentException (CNT-008) if status is not PENDING_REVIEW
     * @throws com.carebridge.backend.content.exception.ContentException (CNT-009) if reason is blank for REJECT
     */
    ContentDecisionResponse decide(UUID id, ContentDecisionRequest request, Principal principal);
}
