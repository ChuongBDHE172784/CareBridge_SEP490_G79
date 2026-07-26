package com.carebridge.backend.content.service;

import com.carebridge.backend.content.dto.request.CreateChecklistTemplateRequest;
import com.carebridge.backend.content.dto.request.HideChecklistTemplateRequest;
import com.carebridge.backend.content.dto.request.UpdateChecklistTemplateRequest;
import com.carebridge.backend.content.dto.response.AdminChecklistTemplateDetailResponse;
import com.carebridge.backend.content.dto.response.HideChecklistTemplateResponse;
import com.carebridge.backend.content.entity.ChecklistTemplateStatus;
import com.carebridge.backend.content.entity.ContentStage;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * UC-243 (CB-CONTENT-IMP-011) — Content Admin CRUD for canonical template roots and checklist
 * entries in {@code care_item_templates}. Downstream consumer is UC-50 (importFromTemplate) —
 * archive must never delete rows, only flip status (ADR-CHK-002).
 */
public interface AdminChecklistTemplateService {

    Page<AdminChecklistTemplateDetailResponse> list(
            ChecklistTemplateStatus status, ContentStage stage, Pageable pageable);

    AdminChecklistTemplateDetailResponse getById(UUID id);

    AdminChecklistTemplateDetailResponse create(CreateChecklistTemplateRequest request, UUID adminUserId);

    AdminChecklistTemplateDetailResponse update(
            UUID id, UpdateChecklistTemplateRequest request, UUID adminUserId);

    HideChecklistTemplateResponse archive(UUID id, HideChecklistTemplateRequest request, UUID adminUserId);
}
