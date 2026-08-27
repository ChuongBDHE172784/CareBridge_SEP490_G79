package com.carebridge.backend.health.service;

import com.carebridge.backend.health.dto.*;

import java.util.UUID;

public interface IHealthRecordService {

    AddHealthRecordResponse addHealthRecord(AddHealthRecordRequest request, UUID callerId);

    /** @throws com.carebridge.backend.common.exception.BusinessException (HEALTH-004/403) if not owner */
    HealthRecordDetailResponse getHealthRecord(UUID recordId, UUID callerId);

    /**
     * Partially update an existing health record (PATCH semantics).
     * Only non-null fields in request are applied.
     *
     * @throws com.carebridge.backend.common.exception.BusinessException (HEALTH-007/404) when not found
     * @throws com.carebridge.backend.common.exception.BusinessException (HEALTH-004/403) when not owner
     * @throws com.carebridge.backend.common.exception.BusinessException (HEALTH-006/409) when ARCHIVED
     */
    UpdateHealthRecordResponse updateHealthRecord(UUID id, UpdateHealthRecordRequest request, UUID ownerUserId);

    /**
     * Soft-delete a health record by setting status to ARCHIVED.
     * Idempotent: ARCHIVED → ARCHIVED returns 200 without calling save() again.
     *
     * @throws com.carebridge.backend.common.exception.BusinessException (HEALTH-007/404) when not found
     * @throws com.carebridge.backend.common.exception.BusinessException (HEALTH-004/403) when not owner
     */
    ArchiveHealthRecordResponse archiveRecord(UUID id, UUID ownerUserId);

    /**
     * Returns paginated ACTIVE health records for the given owner, with optional filters.
     * ownerUserId is always from JWT.
     */
    TimelineResponse getTimeline(UUID ownerUserId, TimelineFilter filter);
}
