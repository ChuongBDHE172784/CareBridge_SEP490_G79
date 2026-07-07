package com.carebridge.backend.health.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.common.exception.ResourceNotFoundException;
import com.carebridge.backend.health.dto.GenerateHealthSummaryRequest;
import com.carebridge.backend.health.dto.HealthSummaryResponse;
import com.carebridge.backend.health.dto.ListHealthSummaryFilter;
import com.carebridge.backend.health.entity.HealthSummary;
import com.carebridge.backend.health.repository.HealthSummaryRepository;
import com.carebridge.backend.health.service.IHealthSummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class HealthSummaryServiceImpl implements IHealthSummaryService {

    // C5: BR-SAFETY — these terms must never appear in summaryJson
    private static final Set<String> FORBIDDEN_KEYS = Set.of(
            "diagnosis", "prescription", "medication", "treatment", "disease"
    );

    private final HealthSummaryRepository summaryRepository;
    private final AuditService auditService;

    @Override
    public HealthSummaryResponse generateSummary(GenerateHealthSummaryRequest request, UUID userId) {
        // C5: Validate summaryJson does not contain forbidden clinical keys
        validateSummaryJson(request.summaryJson());

        var summary = HealthSummary.builder()
                .ownerUserId(userId)
                .journeyId(request.journeyId())
                .babyId(request.babyId())
                .summaryPeriod(request.summaryPeriod())
                .periodStart(request.periodStart())
                .periodEnd(request.periodEnd())
                .summaryJson(request.summaryJson())
                .generatedBy("MOTHER")
                .status("ACTIVE")
                .build();

        var saved = summaryRepository.save(summary);

        // C6: Emit audit event
        auditService.log(AuditAction.HEALTH_SUMMARY_GENERATED, userId,
                "HealthSummary", saved.getId().toString(), "generated");

        log.info("Health summary generated: summaryId={}, userId={}", saved.getId(), userId);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public HealthSummaryResponse getSummary(UUID summaryId, UUID userId) {
        var summary = summaryRepository.findByIdAndOwnerUserId(summaryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("HEALTH-004: Summary not found"));
        return toResponse(summary);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HealthSummaryResponse> listSummaries(UUID userId, ListHealthSummaryFilter filter) {
        return summaryRepository.findActiveByOwnerFiltered(
                        userId,
                        filter.summaryPeriod(),
                        filter.from(),
                        filter.to())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ── Private ────────────────────────────────────────────────────

    private void validateSummaryJson(String summaryJson) {
        if (summaryJson == null) return;
        String lower = summaryJson.toLowerCase();
        FORBIDDEN_KEYS.forEach(key -> {
            if (lower.contains("\"" + key + "\"")) {
                throw new BusinessException(HttpStatus.valueOf(422), "HEALTH-005",
                        "summaryJson must not contain clinical terms: " + key);
            }
        });
    }

    private HealthSummaryResponse toResponse(HealthSummary s) {
        return new HealthSummaryResponse(
                s.getId(),
                s.getOwnerUserId(),
                s.getJourneyId(),
                s.getBabyId(),
                s.getSummaryPeriod(),
                s.getPeriodStart(),
                s.getPeriodEnd(),
                s.getSummaryJson(),
                s.getGeneratedBy(),
                s.getStatus(),
                s.getCreatedAt()
        );
    }
}
