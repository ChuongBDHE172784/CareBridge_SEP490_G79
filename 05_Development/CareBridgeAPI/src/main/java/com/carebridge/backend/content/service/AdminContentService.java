package com.carebridge.backend.content.service;

import com.carebridge.backend.content.dto.request.CreateContentRequest;
import com.carebridge.backend.content.dto.request.HideContentRequest;
import com.carebridge.backend.content.dto.request.UpdateContentRequest;
import com.carebridge.backend.content.dto.response.CreateContentResponse;
import com.carebridge.backend.content.dto.response.HideContentResponse;
import com.carebridge.backend.content.dto.response.UpdateContentResponse;
import com.carebridge.backend.content.dto.response.ContentVersionSnapshotResponse;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.carebridge.backend.content.dto.response.ContentDetailResponse;
import com.carebridge.backend.content.dto.response.StaffContentDetailResponse;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.entity.ContentStatus;
import com.carebridge.backend.content.entity.ContentType;

public interface AdminContentService {

    Page<StaffContentDetailResponse> getStaffContents(
            ContentStatus status, ContentType type, ContentStage stage, String keyword, Pageable pageable);

    StaffContentDetailResponse getStaffContent(UUID id);

    CreateContentResponse createContent(CreateContentRequest request, java.util.UUID authorUserId);

    /**
     * Updates an existing content item; increments versionNo (ADR-002).
     *
     * @throws com.carebridge.backend.content.exception.ContentException (CNT-003) if id not found
     * @throws com.carebridge.backend.content.exception.ContentException (CNT-002) if title/stage change
     *         collides with another item's title+stage+type (ADR-004)
     */
    UpdateContentResponse updateContent(UUID id, UpdateContentRequest request, Principal principal);

    List<ContentVersionSnapshotResponse> getVersionHistory(UUID id);

    /**
     * Hides/soft-deletes a content item by transitioning status to ARCHIVED (ADR-001 — reuses
     * the existing enum value, no hard delete). Allowed from any non-ARCHIVED status (ADR-002).
     *
     * @throws com.carebridge.backend.content.exception.ContentException (CNT-003) if id not found (reused)
     * @throws com.carebridge.backend.content.exception.ContentException (CNT-006) if already ARCHIVED
     * @throws com.carebridge.backend.content.exception.ContentException (CNT-007) if reason is blank (ADR-005)
     */
    HideContentResponse hideContent(UUID id, HideContentRequest request, Principal principal);

    com.carebridge.backend.content.dto.response.BulkImportResponse importContentBatch(
            com.carebridge.backend.content.dto.request.BulkImportContentRequest request, UUID authorUserId);
}
