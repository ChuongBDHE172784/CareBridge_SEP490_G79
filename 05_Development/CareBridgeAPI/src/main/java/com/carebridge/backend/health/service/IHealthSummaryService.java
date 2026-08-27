package com.carebridge.backend.health.service;

import com.carebridge.backend.health.dto.GenerateHealthSummaryRequest;
import com.carebridge.backend.health.dto.HealthSummaryResponse;
import com.carebridge.backend.health.dto.ListHealthSummaryFilter;

import java.util.List;
import java.util.UUID;

public interface IHealthSummaryService {

    HealthSummaryResponse generateSummary(GenerateHealthSummaryRequest request, UUID userId);

    HealthSummaryResponse getSummary(UUID summaryId, UUID userId);

    List<HealthSummaryResponse> listSummaries(UUID userId, ListHealthSummaryFilter filter);
}
