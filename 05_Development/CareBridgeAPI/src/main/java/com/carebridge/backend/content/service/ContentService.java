package com.carebridge.backend.content.service;

import com.carebridge.backend.content.dto.request.ContentFilterRequest;
import com.carebridge.backend.content.dto.response.ChecklistTemplateResponse;
import com.carebridge.backend.content.dto.response.ContentDetailResponse;
import com.carebridge.backend.content.dto.response.ContentListResponse;
import com.carebridge.backend.content.entity.ContentStage;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ContentService {

    /**
     * Returns paginated APPROVED content filtered by type, stage, topicId.
     * Always enforces status=APPROVED (BR-RBAC). authorId never exposed (BR-PRIVACY).
     */
    Page<ContentListResponse> getContents(ContentFilterRequest filter, Pageable pageable);

    /**
     * Returns detail of a single APPROVED content item.
     *
     * @throws ContentException(CNT-003) if not found or not APPROVED
     */
    ContentDetailResponse getContentById(UUID id);

    /**
     * Returns all checklist templates for a given stage.
     *
     * @param stage nullable — if null, returns templates for all stages
     */
    List<ChecklistTemplateResponse> getChecklists(ContentStage stage);
}
