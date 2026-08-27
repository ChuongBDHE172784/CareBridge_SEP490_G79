package com.carebridge.backend.content.service;

import com.carebridge.backend.content.dto.request.ContentDecisionRequest;
import com.carebridge.backend.content.dto.request.ReassignContentRequest;
import com.carebridge.backend.content.dto.response.ChecklistTemplateDecisionResponse;
import com.carebridge.backend.content.dto.response.ContentDecisionResponse;
import com.carebridge.backend.content.dto.response.ExpertContentApprovalQueueItem;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.entity.ContentType;
import java.security.Principal;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ExpertContentApprovalService {

    Page<ExpertContentApprovalQueueItem> getAssignedQueue(
            ContentType type, ContentStage stage, String keyword, Pageable pageable, Principal principal);

    ContentDecisionResponse decideContent(UUID id, ContentDecisionRequest request, Principal principal);

    ChecklistTemplateDecisionResponse decideChecklist(UUID id, ContentDecisionRequest request, Principal principal);

    void reassignContent(UUID id, ReassignContentRequest request, Principal principal);

    void reassignChecklist(UUID id, ReassignContentRequest request, Principal principal);
}
