package com.carebridge.backend.health;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.common.exception.ResourceNotFoundException;
import com.carebridge.backend.health.dto.GenerateHealthSummaryRequest;
import com.carebridge.backend.health.dto.ListHealthSummaryFilter;
import com.carebridge.backend.health.entity.HealthSummary;
import com.carebridge.backend.health.repository.HealthRecordRepository;
import com.carebridge.backend.health.repository.HealthSummaryRepository;
import com.carebridge.backend.health.service.impl.HealthSummaryServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthSummaryServiceTest {

    // Props Isolation — synthetic IDs only (never real DB data)
    static final UUID OWNER_ID   = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID OTHER_ID   = UUID.fromString("00000000-0000-0000-0000-000000000002");
    static final UUID SUMMARY_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");

    @Mock private HealthSummaryRepository summaryRepository;
    @Mock private HealthRecordRepository healthRecordRepository;
    @Mock private AuditService auditService;
    @InjectMocks private HealthSummaryServiceImpl service;

    // ── Factories ─────────────────────────────────────────────────

    private GenerateHealthSummaryRequest makeRequest() {
        return new GenerateHealthSummaryRequest(
                null, null, "24H",
                LocalDate.now().minusDays(1), LocalDate.now(),
                "{\"observations\":[\"Normal blood pressure\"]}"
        );
    }

    private GenerateHealthSummaryRequest makeForbiddenKeywordRequest() {
        return new GenerateHealthSummaryRequest(
                null, null, "24H",
                LocalDate.now().minusDays(1), LocalDate.now(),
                "{\"diagnosis\":\"Hypertension\",\"observations\":[]}"
        );
    }

    private HealthSummary makeSavedSummary() {
        return HealthSummary.builder()
                .id(SUMMARY_ID)
                .ownerUserId(OWNER_ID)
                .summaryPeriod("24H")
                .periodStart(LocalDate.now().minusDays(1))
                .periodEnd(LocalDate.now())
                .summaryJson("{\"observations\":[\"Normal blood pressure\"]}")
                .status("ACTIVE")
                .createdAt(Instant.now())
                .build();
    }

    // ── HEALTH-TC-001: generateSummary — valid request → returns response ──

    @Test
    void generateSummary_validRequest_returnsResponse() {
        when(summaryRepository.save(any())).thenReturn(makeSavedSummary());

        var response = service.generateSummary(makeRequest(), OWNER_ID);

        assertThat(response).isNotNull();
        assertThat(response.summaryId()).isEqualTo(SUMMARY_ID);
        assertThat(response.summaryPeriod()).isEqualTo("24H");
        assertThat(response.status()).isEqualTo("ACTIVE");
    }

    // ── HEALTH-TC-002: generateSummary — forbidden keyword in summaryJson → HEALTH-005 ──

    @Test
    void generateSummary_forbiddenKeyword_throwsHEALTH005() {
        assertThatThrownBy(() -> service.generateSummary(makeForbiddenKeywordRequest(), OWNER_ID))
                .isInstanceOf(BusinessException.class);
    }

    // ── HEALTH-TC-003: getSummary — owner reads own summary → returns response ──

    @Test
    void getSummary_ownerAccess_returnsResponse() {
        when(summaryRepository.findByIdAndOwnerUserId(SUMMARY_ID, OWNER_ID))
                .thenReturn(Optional.of(makeSavedSummary()));

        var response = service.getSummary(SUMMARY_ID, OWNER_ID);

        assertThat(response).isNotNull();
        assertThat(response.summaryId()).isEqualTo(SUMMARY_ID);
    }

    // ── HEALTH-TC-004: getSummary — summary not found → HEALTH-004 ──

    @Test
    void getSummary_notFound_throwsResourceNotFound() {
        when(summaryRepository.findByIdAndOwnerUserId(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSummary(UUID.randomUUID(), OWNER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── HEALTH-TC-005: getSummary — non-owner → 404 (privacy by design, HEALTH-004) ──

    @Test
    void getSummary_nonOwner_throwsResourceNotFound() {
        when(summaryRepository.findByIdAndOwnerUserId(SUMMARY_ID, OTHER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSummary(SUMMARY_ID, OTHER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── HEALTH-TC-006: listSummaries — returns owner's list ──

    @Test
    void listSummaries_owner_returnsList() {
        when(summaryRepository.findActiveByOwnerFiltered(any(), any(), any(), any()))
                .thenReturn(List.of(makeSavedSummary()));

        var filter = new ListHealthSummaryFilter(null, null, null);
        var result = service.listSummaries(OWNER_ID, filter);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).summaryId()).isEqualTo(SUMMARY_ID);
    }

    // ── HEALTH-TC-007: listSummaries — empty list when no summaries ──

    @Test
    void listSummaries_empty_returnsEmptyList() {
        when(summaryRepository.findActiveByOwnerFiltered(any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        var filter = new ListHealthSummaryFilter(null, null, null);
        var result = service.listSummaries(OWNER_ID, filter);

        assertThat(result).isEmpty();
    }
}
