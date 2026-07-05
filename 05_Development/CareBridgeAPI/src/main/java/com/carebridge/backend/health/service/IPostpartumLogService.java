package com.carebridge.backend.health.service;

import com.carebridge.backend.health.dto.AddPostpartumLogRequest;
import com.carebridge.backend.health.dto.PostpartumLogResponse;

import java.util.UUID;

public interface IPostpartumLogService {

    /** UC28: Add a postpartum recovery log to a POSTPARTUM + ACTIVE journey. Async AI (fail-open).
     * @throws com.carebridge.backend.common.exception.BusinessException
     *   (POST-001/404, POST-002/400, POST-003/400, POST-006/403)
     */
    PostpartumLogResponse addLog(UUID userId, UUID journeyId, AddPostpartumLogRequest request);
}
