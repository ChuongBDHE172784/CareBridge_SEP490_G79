package com.carebridge.backend.content.service;

import com.carebridge.backend.content.dto.request.CreateChecklistTemplateRequest;
import com.carebridge.backend.content.dto.request.HideChecklistTemplateRequest;
import com.carebridge.backend.content.dto.request.UpdateChecklistTemplateRequest;
import com.carebridge.backend.content.dto.response.ChecklistTemplateResponse;
import com.carebridge.backend.content.dto.response.HideChecklistTemplateResponse;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.entity.ContentStatus;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * UC-243 (CB-CONTENT-IMP-011) — Content Admin CRUD for {@code checklist_templates}/{@code checklist_items}.
 * Reuses existing entities; downstream consumer is UC-50 (importFromTemplate) — archive must never
 * delete rows, only flip status (ADR-CHK-002).
 */
public interface AdminChecklistTemplateService {

    Page<ChecklistTemplateResponse> list(ContentStatus status, ContentStage stage, Pageable pageable);

    ChecklistTemplateResponse getById(UUID id);

    ChecklistTemplateResponse create(CreateChecklistTemplateRequest request, UUID adminUserId);

    ChecklistTemplateResponse update(UUID id, UpdateChecklistTemplateRequest request, UUID adminUserId);

    HideChecklistTemplateResponse archive(UUID id, HideChecklistTemplateRequest request, UUID adminUserId);
}
