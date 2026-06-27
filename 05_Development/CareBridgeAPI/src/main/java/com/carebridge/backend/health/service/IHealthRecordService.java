package com.carebridge.backend.health.service;

import com.carebridge.backend.health.dto.AddHealthRecordRequest;
import com.carebridge.backend.health.dto.AddHealthRecordResponse;
import com.carebridge.backend.health.dto.HealthRecordDetailResponse;

import java.util.UUID;

public interface IHealthRecordService {

    AddHealthRecordResponse addHealthRecord(AddHealthRecordRequest request, UUID callerId);

    /** @throws com.carebridge.backend.common.exception.BusinessException (HEALTH-004/403) if not owner */
    HealthRecordDetailResponse getHealthRecord(UUID recordId, UUID callerId);
}
