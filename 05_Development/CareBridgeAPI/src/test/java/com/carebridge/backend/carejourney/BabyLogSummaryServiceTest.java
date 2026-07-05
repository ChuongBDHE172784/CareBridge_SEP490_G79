package com.carebridge.backend.carejourney;

import com.carebridge.backend.baby.entity.BabyProfile;
import com.carebridge.backend.baby.entity.BabyProfileStatus;
import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.carejourney.dto.BabyLogSummaryResponse;
import com.carebridge.backend.carejourney.repository.BabyDailyLogRepository;
import com.carebridge.backend.carejourney.repository.LogTypeAggregateRow;
import com.carebridge.backend.carejourney.service.GeminiInsightService;
import com.carebridge.backend.carejourney.service.impl.BabyLogSummaryServiceImpl;
import com.carebridge.backend.common.exception.AccessDeniedBusinessException;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BabyLogSummaryServiceTest {

    @Mock private BabyDailyLogRepository babyDailyLogRepository;
    @Mock private BabyProfileRepository babyProfileRepository;
    @Mock private GeminiInsightService geminiInsightService;
    @InjectMocks private BabyLogSummaryServiceImpl service;

    static final UUID USER_ID       = UUID.fromString("00000000-0000-0000-0000-000000000100");
    static final UUID BABY_ID       = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID OTHER_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000200");

    private BabyProfile makeActiveBaby() {
        return BabyProfile.builder()
                .id(BABY_ID).ownerUserId(USER_ID).nickname("Test Baby")
                .status(BabyProfileStatus.ACTIVE).build();
    }

    private BabyProfile makeOtherUserBaby() {
        return BabyProfile.builder()
                .id(BABY_ID).ownerUserId(OTHER_USER_ID).nickname("Other Baby")
                .status(BabyProfileStatus.ACTIVE).build();
    }

    private Principal makePrincipal(UUID userId) {
        return userId::toString;
    }

    // Build mock aggregate rows BEFORE using in when().thenReturn() to avoid nested stubbing
    private LogTypeAggregateRow buildFeedingRow() {
        LogTypeAggregateRow row = mock(LogTypeAggregateRow.class);
        when(row.getLogType()).thenReturn("FEEDING");
        when(row.getCount()).thenReturn(3L);
        when(row.getTotalQuantity()).thenReturn(new BigDecimal("450"));
        when(row.getMaxQuantity()).thenReturn(new BigDecimal("200"));
        when(row.getUnit()).thenReturn("ml");
        return row;
    }

    private LogTypeAggregateRow buildSleepRow() {
        LogTypeAggregateRow row = mock(LogTypeAggregateRow.class);
        when(row.getLogType()).thenReturn("SLEEP");
        when(row.getCount()).thenReturn(2L);
        when(row.getTotalQuantity()).thenReturn(new BigDecimal("7.5"));
        when(row.getMaxQuantity()).thenReturn(new BigDecimal("4.0"));
        when(row.getUnit()).thenReturn("hours");
        return row;
    }

    // BABY-TC-036-001: 24h summary with aggregated data
    @Test
    void getSummary_24hPeriod_returnsAggregatedData() {
        // Pre-create mocks before using in when() to avoid nested stubbing
        LogTypeAggregateRow feedingRow = buildFeedingRow();
        LogTypeAggregateRow sleepRow = buildSleepRow();

        when(babyProfileRepository.findById(BABY_ID)).thenReturn(Optional.of(makeActiveBaby()));
        when(babyDailyLogRepository.aggregateByLogType(eq(BABY_ID), any(), any()))
                .thenReturn(List.of(feedingRow, sleepRow));
        when(geminiInsightService.generateInsight(any(), eq("24h")))
                .thenReturn(CompletableFuture.completedFuture("Baby is doing well"));

        BabyLogSummaryResponse resp = service.getSummary(BABY_ID, "24h", makePrincipal(USER_ID));

        assertThat(resp.getPeriod()).isEqualTo("24h");
        assertThat(resp.getSummaries()).containsKey("FEEDING");
        assertThat(resp.getSummaries().get("FEEDING").getCount()).isEqualTo(3);
        assertThat(resp.getSummaries().get("FEEDING").getTotalQuantity()).isEqualByComparingTo("450");
        assertThat(resp.getSummaries()).containsKey("SLEEP");
        assertThat(resp.getAiInsight()).isEqualTo("Baby is doing well");
        assertThat(resp.getFromDate())
                .isAfter(Instant.now().minus(Duration.ofHours(25)))
                .isBefore(Instant.now().minus(Duration.ofHours(23)));
    }

    // BABY-TC-036-002: 7d summary with wider range
    @Test
    void getSummary_7dPeriod_usesCorrectTimeRange() {
        LogTypeAggregateRow feedingRow = buildFeedingRow();

        when(babyProfileRepository.findById(BABY_ID)).thenReturn(Optional.of(makeActiveBaby()));
        when(babyDailyLogRepository.aggregateByLogType(eq(BABY_ID), any(), any()))
                .thenReturn(List.of(feedingRow));
        when(geminiInsightService.generateInsight(any(), eq("7d")))
                .thenReturn(CompletableFuture.completedFuture(null));

        BabyLogSummaryResponse resp = service.getSummary(BABY_ID, "7d", makePrincipal(USER_ID));

        assertThat(resp.getPeriod()).isEqualTo("7d");
        assertThat(resp.getFromDate())
                .isAfter(Instant.now().minus(Duration.ofDays(8)))
                .isBefore(Instant.now().minus(Duration.ofDays(6)));
        verify(babyDailyLogRepository).aggregateByLogType(
                eq(BABY_ID),
                argThat(from -> from.isBefore(Instant.now().minus(Duration.ofDays(6)))),
                argThat(to -> to.isAfter(Instant.now().minus(Duration.ofHours(1))))
        );
    }

    // BABY-TC-036-003: No logs in period -> 200 with zero counts (L2)
    @Test
    void getSummary_noLogs_returns200WithZeroCounts() {
        when(babyProfileRepository.findById(BABY_ID)).thenReturn(Optional.of(makeActiveBaby()));
        when(babyDailyLogRepository.aggregateByLogType(any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(geminiInsightService.generateInsight(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        BabyLogSummaryResponse resp = service.getSummary(BABY_ID, "24h", makePrincipal(USER_ID));

        assertThat(resp).isNotNull();
        assertThat(resp.getSummaries()).isNotNull();
        assertThat(resp.getSummaries()).containsKey("FEEDING");
        assertThat(resp.getSummaries().get("FEEDING").getCount()).isZero();
        assertThat(resp.getAiInsight()).isNull();
    }

    // BABY-TC-036-004: Invalid period -> BusinessException BABY-052
    @Test
    void getSummary_invalidPeriod_throwsBadRequest() {
        when(babyProfileRepository.findById(BABY_ID)).thenReturn(Optional.of(makeActiveBaby()));

        assertThatThrownBy(() -> service.getSummary(BABY_ID, "30d", makePrincipal(USER_ID)))
                .isInstanceOf(BusinessException.class);
    }

    // BABY-TC-036-005: Baby not owned -> AccessDeniedBusinessException BABY-051
    @Test
    void getSummary_babyNotOwned_throwsForbidden() {
        when(babyProfileRepository.findById(BABY_ID)).thenReturn(Optional.of(makeOtherUserBaby()));

        assertThatThrownBy(() -> service.getSummary(BABY_ID, "24h", makePrincipal(USER_ID)))
                .isInstanceOf(AccessDeniedBusinessException.class);
    }

    // BABY-TC-036-006: Baby not found -> ResourceNotFoundException BABY-050
    @Test
    void getSummary_babyNotFound_throwsNotFound() {
        when(babyProfileRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSummary(BABY_ID, "24h", makePrincipal(USER_ID)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // BABY-TC-036-007: Gemini AI error -> aiInsight = null (fail-open, C3)
    @Test
    void getSummary_geminiError_summaryStillReturnedWithNullInsight() {
        LogTypeAggregateRow feedingRow = buildFeedingRow();

        when(babyProfileRepository.findById(BABY_ID)).thenReturn(Optional.of(makeActiveBaby()));
        when(babyDailyLogRepository.aggregateByLogType(any(), any(), any()))
                .thenReturn(List.of(feedingRow));
        when(geminiInsightService.generateInsight(any(), any()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Gemini API error")));

        BabyLogSummaryResponse resp = service.getSummary(BABY_ID, "24h", makePrincipal(USER_ID));

        assertThat(resp).isNotNull();
        assertThat(resp.getSummaries()).containsKey("FEEDING");
        assertThat(resp.getAiInsight()).isNull();
    }
}
