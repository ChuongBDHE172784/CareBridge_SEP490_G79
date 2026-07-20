package com.carebridge.backend.health.service;

import com.carebridge.backend.health.dto.AddPostpartumLogRequest;
import com.carebridge.backend.health.dto.PostpartumLogResponse;
import com.carebridge.backend.health.dto.UpdatePostpartumLogRequest;

import java.util.List;
import org.springframework.data.domain.Page;
import java.util.UUID;

public interface IPostpartumLogService {

    /** UC189: Lists ACTIVE postpartum logs for a journey owned by the caller. */
    Page<PostpartumLogResponse> listLogs(UUID journeyId, UUID callerId, int page, int size);

    /** UC189: Returns detail for one ACTIVE postpartum log owned by the caller. */
    PostpartumLogResponse getLogDetail(UUID logId, UUID callerId);

    /** UC190: Partially updates mutable content fields on an ACTIVE postpartum log. */
    PostpartumLogResponse updateLog(UUID logId, UUID callerId, UpdatePostpartumLogRequest request);

    /** UC191: Soft-deletes an ACTIVE postpartum log owned by the caller. */
    void deleteLog(UUID logId, UUID callerId);

    /** UC28: Add a postpartum recovery log to a POSTPARTUM + ACTIVE journey. Async AI (fail-open).
     * @throws com.carebridge.backend.common.exception.BusinessException
     *   (POST-001/404, POST-002/400, POST-003/400, POST-006/403)
     */
    PostpartumLogResponse addLog(UUID userId, UUID journeyId, AddPostpartumLogRequest request);
}
