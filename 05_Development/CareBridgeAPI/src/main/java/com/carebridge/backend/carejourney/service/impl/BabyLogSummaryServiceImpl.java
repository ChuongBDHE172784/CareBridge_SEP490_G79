package com.carebridge.backend.carejourney.service.impl;

import com.carebridge.backend.baby.entity.BabyProfile;
import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.carejourney.dto.BabyLogSummaryResponse;
import com.carebridge.backend.carejourney.dto.LogTypeSummary;
import com.carebridge.backend.carejourney.repository.BabyDailyLogRepository;
import com.carebridge.backend.carejourney.repository.LogTypeAggregateRow;
import com.carebridge.backend.carejourney.service.GeminiInsightService;
import com.carebridge.backend.carejourney.service.IBabyLogSummaryService;
import com.carebridge.backend.common.exception.AccessDeniedBusinessException;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.common.exception.ResourceNotFoundException;
import com.carebridge.backend.common.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BabyLogSummaryServiceImpl implements IBabyLogSummaryService {

    private static final Set<String> VALID_PERIODS = Set.of("24h", "7d");
    private static final Set<String> ALL_LOG_TYPES =
            Set.of("FEEDING", "SLEEP", "DIAPER", "FEVER", "VOMITING", "MEDICINE");

    private final BabyDailyLogRepository babyDailyLogRepository;
    private final BabyProfileRepository babyProfileRepository;
    private final GeminiInsightService geminiInsightService;

    @Override
    public BabyLogSummaryResponse getSummary(UUID babyId, String period, Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);

        BabyProfile baby = babyProfileRepository.findById(babyId)
                .orElseThrow(() -> new ResourceNotFoundException("Baby profile not found: " + babyId));

        // C1: ownership check (BABY-051)
        if (!baby.getOwnerUserId().equals(userId)) {
            throw new AccessDeniedBusinessException("You do not own this baby profile");
        }

        // C7: period validation — "24h" or "7d" only (BABY-052, ADR-BABY-006-002)
        if (!VALID_PERIODS.contains(period)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "BABY-052",
                    "Invalid period. Accepted values: 24h, 7d");
        }

        // C2: calculate rolling window bounds — NOT calendar-based (L1 fix)
        Instant toDate = Instant.now();
        Instant fromDate = "24h".equals(period)
                ? toDate.minus(Duration.ofHours(24))
                : toDate.minus(Duration.ofDays(7));

        // C5: SQL aggregation via repository (ADR-BABY-006-001 — not Java loops)
        List<LogTypeAggregateRow> rows = babyDailyLogRepository.aggregateByLogType(babyId, fromDate, toDate);

        // Build summaries with zero-fill for all 6 log types (L2 fix — return 200 with count=0)
        Map<String, LogTypeSummary> summaries = buildSummaries(rows, babyId, fromDate, toDate);

        // C3: Gemini AI insight — async, fail-open (null on error) — ADR-BABY-006-003
        String aiInsight = fetchGeminiInsight(summaries, period);

        return BabyLogSummaryResponse.builder()
                .babyId(babyId)
                .period(period)
                .fromDate(fromDate)
                .toDate(toDate)
                .summaries(summaries)
                .aiInsight(aiInsight)
                .build();
    }

    private Map<String, LogTypeSummary> buildSummaries(
            List<LogTypeAggregateRow> rows, UUID babyId, Instant fromDate, Instant toDate) {

        Map<String, LogTypeSummary> summaries = new HashMap<>();

        // Zero-fill all 6 log types
        for (String logType : ALL_LOG_TYPES) {
            summaries.put(logType, LogTypeSummary.builder()
                    .count(0).totalQuantity(null).unit(null).maxValue(null).latestNote(null).build());
        }

        // Populate from SQL aggregation results
        for (LogTypeAggregateRow row : rows) {
            BigDecimal totalQty = row.getTotalQuantity();
            BigDecimal maxQty = row.getMaxQuantity();

            List<String> notes = null;
            if ("MEDICINE".equals(row.getLogType())) {
                notes = babyDailyLogRepository.findNotesByLogTypeAndPeriod(babyId, "MEDICINE", fromDate, toDate);
            }

            summaries.put(row.getLogType(), LogTypeSummary.builder()
                    .count((int) row.getCount())
                    .totalQuantity(totalQty)
                    .unit(row.getUnit())
                    .maxValue("FEVER".equals(row.getLogType()) ? maxQty : null)
                    .latestNote(null)
                    .notes("MEDICINE".equals(row.getLogType()) ? notes : null)
                    .build());
        }

        return summaries;
    }

    // C3: Gemini fail-open — returns null on any error (ADR-BABY-006-003)
    private String fetchGeminiInsight(Map<String, LogTypeSummary> summaries, String period) {
        try {
            CompletableFuture<String> future = geminiInsightService.generateInsight(summaries, period);
            return future.join();
        } catch (Exception e) {
            return null;
        }
    }
}
