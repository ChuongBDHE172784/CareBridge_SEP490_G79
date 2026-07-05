package com.carebridge.backend.carejourney.service;

import com.carebridge.backend.carejourney.dto.AddBabyDailyLogRequest;
import com.carebridge.backend.carejourney.dto.AddBabyDailyLogResponse;
import com.carebridge.backend.carejourney.dto.BabyDailyLogResponse;
import com.carebridge.backend.carejourney.dto.UpdateBabyDailyLogRequest;

import java.security.Principal;
import java.util.UUID;

public interface IBabyDailyLogService {

    /**
     * Adds a daily log entry for a baby.
     * recorded_by is set from userId (JWT), not from request body.
     *
     * @throws com.carebridge.backend.common.exception.ResourceNotFoundException (BABY-030) when baby not found
     * @throws com.carebridge.backend.common.exception.AccessDeniedBusinessException (BABY-031) when baby not owned
     * @throws com.carebridge.backend.common.exception.BusinessException (BABY-032) when baby is ARCHIVED
     */
    AddBabyDailyLogResponse addDailyLog(UUID babyId, AddBabyDailyLogRequest request, UUID userId);

    /**
     * Updates a baby daily log within the 24-hour edit window.
     * log_type is immutable and silently ignored if sent.
     *
     * @throws com.carebridge.backend.common.exception.ResourceNotFoundException (BABY-040) when log not found
     * @throws com.carebridge.backend.common.exception.ResourceNotFoundException (BABY-041) when log not in baby
     * @throws com.carebridge.backend.common.exception.BusinessException (BABY-042) when edit window expired
     * @throws com.carebridge.backend.common.exception.AccessDeniedBusinessException (BABY-043) when baby not owned
     */
    BabyDailyLogResponse updateLog(UUID babyId, UUID logId, UpdateBabyDailyLogRequest request, Principal principal);

    /**
     * Hard-deletes a baby daily log within the 24-hour edit window.
     * Emits BABY_LOG_DELETED audit event with log snapshot before deletion.
     *
     * @throws com.carebridge.backend.common.exception.ResourceNotFoundException (BABY-040) when log not found
     * @throws com.carebridge.backend.common.exception.ResourceNotFoundException (BABY-041) when log not in baby
     * @throws com.carebridge.backend.common.exception.BusinessException (BABY-042) when edit window expired
     * @throws com.carebridge.backend.common.exception.AccessDeniedBusinessException (BABY-043) when baby not owned
     */
    void deleteLog(UUID babyId, UUID logId, Principal principal);
}
