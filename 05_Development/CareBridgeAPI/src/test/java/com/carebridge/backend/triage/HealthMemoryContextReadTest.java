package com.carebridge.backend.triage;

import com.carebridge.backend.triage.dto.HealthMemoryContextItem;
import com.carebridge.backend.triage.entity.HealthMemoryEntry;
import com.carebridge.backend.triage.policy.HealthMemorySummaryPolicy;
import com.carebridge.backend.triage.repository.HealthMemoryEntryRepository;
import com.carebridge.backend.triage.repository.IIntakeSessionRepository;
import com.carebridge.backend.triage.service.impl.HealthMemoryServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.carebridge.backend.triage.HealthMemoryContextTestFactory.BABY_1;
import static com.carebridge.backend.triage.HealthMemoryContextTestFactory.BABY_2;
import static com.carebridge.backend.triage.HealthMemoryContextTestFactory.MOTHER_1;
import static com.carebridge.backend.triage.HealthMemoryContextTestFactory.NOW_FIXED;
import static com.carebridge.backend.triage.HealthMemoryContextTestFactory.USER_A;
import static com.carebridge.backend.triage.HealthMemoryContextTestFactory.makeActiveMemory;
import static com.carebridge.backend.triage.HealthMemoryContextTestFactory.makeProperties;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * CB-TRIAGE-THMC-IMP-001-TEST — read path (THMC-TC-07/08/10/16).
 * Oracle: HealthMemoryEntryRepository.java:14-33 guarded queries / BR-THMC-002 /
 * ADR-THMC-003 bounding / Logic Issue L4.
 */
@ExtendWith(MockitoExtension.class)
class HealthMemoryContextReadTest {

    @Mock private HealthMemoryEntryRepository memoryRepository;
    @Mock private IIntakeSessionRepository sessionRepository;

    private HealthMemoryServiceImpl service() {
        return new HealthMemoryServiceImpl(memoryRepository, sessionRepository,
                new HealthMemorySummaryPolicy(new ObjectMapper()), makeProperties());
    }

    @Test
    void thmcTc07_expiryBoundary_expiredExcluded_notYetExpiredIncluded() {
        // Oracle: repository strict `expiresAt > :now` (:19) — the mock replays the
        // documented predicate against the `now` argument the service actually passes.
        when(memoryRepository.findActivePediatric(eq(USER_A), eq(BABY_1), eq(TriageStage.INFANT),
                any(Instant.class))).thenAnswer(invocation -> {
                    Instant now = invocation.getArgument(3);
                    HealthMemoryEntry expired = makeActiveMemory(e -> {
                        e.setId(UUID.fromString("00000000-0000-0000-0000-00000000f0e1"));
                        e.setSummaryText("SYNTHETIC EXPIRED_TWIN");
                        e.setExpiresAt(now.minusSeconds(1));   // FX-THMC-007 (expired)
                    });
                    HealthMemoryEntry boundaryActive = makeActiveMemory(e -> {
                        e.setSummaryText("SYNTHETIC BOUNDARY_ACTIVE_TWIN");
                        e.setExpiresAt(now.plusSeconds(1));    // FX-THMC-007 (boundary twin)
                    });
                    return List.of(expired, boundaryActive).stream()
                            .filter(e -> e.getExpiresAt() == null || e.getExpiresAt().isAfter(now))
                            .toList();
                });

        List<HealthMemoryContextItem> result =
                service().loadContextForIntake(USER_A, TriageStage.INFANT, BABY_1);

        assertThat(result).extracting(HealthMemoryContextItem::summaryText)
                .containsExactly("SYNTHETIC BOUNDARY_ACTIVE_TWIN");
        // C3: sole read path is the guarded active query — no findAll / no bypass
        verify(memoryRepository, times(1)).findActivePediatric(
                eq(USER_A), eq(BABY_1), eq(TriageStage.INFANT), any(Instant.class));
        verifyNoMoreInteractions(memoryRepository);
    }

    @Test
    void thmcTc08_softDeletedMemory_neverInjected() {
        // Oracle: repository `deletedAt is null` (:16-17) / user-erasure right
        lenient().when(memoryRepository.findAll()).thenReturn(List.of(
                makeActiveMemory(e -> e.setDeletedAt(NOW_FIXED.minusSeconds(3600))))); // FX-THMC-008
        when(memoryRepository.findActivePediatric(eq(USER_A), eq(BABY_1), eq(TriageStage.INFANT),
                any(Instant.class))).thenReturn(List.of());

        List<HealthMemoryContextItem> result =
                service().loadContextForIntake(USER_A, TriageStage.INFANT, BABY_1);

        assertThat(result).isEmpty();
        verify(memoryRepository, never()).findAll();
        verify(memoryRepository, times(1)).findActivePediatric(
                eq(USER_A), eq(BABY_1), eq(TriageStage.INFANT), any(Instant.class));
    }

    @Test
    void thmcTc10_subjectAndStageIsolation_andNullProfileYieldsEmptyWithoutError() {
        HealthMemoryServiceImpl service = service();

        // 1) Pediatric routing: BABY_2 query is profile-bound — BABY_1 memory not returned
        when(memoryRepository.findActivePediatric(eq(USER_A), eq(BABY_2), eq(TriageStage.INFANT),
                any(Instant.class))).thenReturn(List.of());
        List<HealthMemoryContextItem> pediatric =
                service.loadContextForIntake(USER_A, TriageStage.INFANT, BABY_2);
        assertThat(pediatric).isEmpty();
        verify(memoryRepository).findActivePediatric(
                eq(USER_A), eq(BABY_2), eq(TriageStage.INFANT), any(Instant.class));

        // 2) Maternal routing (mirrors postpartum_shouldUseMaternalMemoryBoundaryOnly precedent)
        when(memoryRepository.findActiveMaternal(eq(USER_A), eq(MOTHER_1), eq(TriageStage.PREGNANCY),
                any(Instant.class))).thenReturn(List.of(makeActiveMemory(e -> {
                    e.setBabyProfileId(null);
                    e.setMotherProfileId(MOTHER_1);
                    e.setRelatedStage(TriageStage.PREGNANCY);
                    e.setSummaryText("SYNTHETIC maternal memory");
                })));
        List<HealthMemoryContextItem> maternal =
                service.loadContextForIntake(USER_A, TriageStage.PREGNANCY, MOTHER_1);
        assertThat(maternal).extracting(HealthMemoryContextItem::summaryText)
                .containsExactly("SYNTHETIC maternal memory");
        verify(memoryRepository).findActiveMaternal(
                eq(USER_A), eq(MOTHER_1), eq(TriageStage.PREGNANCY), any(Instant.class));
        // pediatric query used only once (section 1) — never for the maternal stage
        verify(memoryRepository, times(1)).findActivePediatric(any(), any(), any(), any());

        // 3) Logic Issue L4: null profile ⇒ empty context, NO TRIAGE-014, no repository query
        assertThatCode(() -> {
            List<HealthMemoryContextItem> nullProfile =
                    service.loadContextForIntake(USER_A, TriageStage.INFANT, null);
            assertThat(nullProfile).isEmpty();
        }).doesNotThrowAnyException();
        verifyNoMoreInteractions(memoryRepository);
        verifyNoInteractions(sessionRepository);
    }

    @Test
    void thmcTc16_contextBounded_maxEntriesNewestFirst_andSummaryTruncation() {
        // Oracle: ADR-THMC-003 Decision (3) / TDS §4.1 prompt bounding / FX-THMC-009
        Instant base = Instant.parse("2026-07-01T00:00:00Z");
        List<HealthMemoryEntry> newestFirst = new ArrayList<>();
        for (int i = 6; i >= 1; i--) {
            final int index = i;
            newestFirst.add(makeActiveMemory(e -> {
                e.setId(UUID.randomUUID());
                e.setCreatedAt(base.plusSeconds(index));
                e.setSummaryText(index == 6 ? "S".repeat(501) : "SYNTHETIC MEM_" + index);
            }));
        }
        when(memoryRepository.findActivePediatric(eq(USER_A), eq(BABY_1), eq(TriageStage.INFANT),
                any(Instant.class))).thenReturn(newestFirst);

        List<HealthMemoryContextItem> result =
                service().loadContextForIntake(USER_A, TriageStage.INFANT, BABY_1);

        // Boundary 5/6: cap enforced, dropped entry is the OLDEST (MEM_1), order preserved
        assertThat(result).hasSize(5);
        assertThat(result).extracting(HealthMemoryContextItem::summaryText)
                .doesNotContain("SYNTHETIC MEM_1");
        assertThat(result.subList(1, 5)).extracting(HealthMemoryContextItem::summaryText)
                .containsExactly("SYNTHETIC MEM_5", "SYNTHETIC MEM_4",
                        "SYNTHETIC MEM_3", "SYNTHETIC MEM_2");
        // Boundary 500/501: truncation to exactly maxSummaryChars
        assertThat(result).allMatch(item -> item.summaryText().length() <= 500);
        assertThat(result.get(0).summaryText()).isEqualTo("S".repeat(500));
    }
}
