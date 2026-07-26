package com.carebridge.backend.triage.engine;

import com.carebridge.backend.triage.TriageStage;
import com.carebridge.backend.triage.dto.HealthMemoryContextItem;
import com.carebridge.backend.triage.dto.request.RunIntakeRequest;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CB-TRIAGE-THMC-IMP-001-TEST — THMC-TC-12 (CRITICAL, BR-SAFETY).
 * Oracle: BR-THMC-004 / ADR-THMC-003 Decision — "context participates only in summary
 * phrasing, never in rule matching; riskLevel with context >= riskLevel without context".
 * Real deterministic engine (no mock), constructed exactly as TriageGraphServiceTest does.
 */
class TriageGraphServiceHealthContextTest {

    private final TriageGraphService graph = new TriageGraphService(
            new SymptomNormalizer(),
            new SourceRetriever(),
            new PediatricRiskRules());

    @Test
    void thmcTc12_benignGreenContext_neverLowersDeterministicRedRisk() {
        // Hardcoded RED red-flag pediatric input (PediatricRiskRules — breathing distress + lethargy)
        RunIntakeRequest redRequest = RunIntakeRequest.builder()
                .stage(TriageStage.INFANT)
                .babyProfileId(UUID.fromString("00000000-0000-0000-0000-00000000c001"))
                .childAgeMonths(7)
                .symptomList(List.of("khó thở"))
                .breathingStatus("Tím tái")
                .consciousnessStatus("Li bì")
                .build();
        HealthMemoryContextItem benignGreenContextItem = new HealthMemoryContextItem(
                "SYNTHETIC prior triage: risk GREEN; mild cough", "INFANT",
                Instant.parse("2026-07-20T09:00:00Z"), Instant.parse("2026-08-19T09:00:00Z"));

        ChildTriageResult baseline = graph.run(redRequest);
        ChildTriageResult withCtx = graph.run(redRequest, List.of(benignGreenContextItem));

        // RED stays RED with benign context; emergency flag unchanged (BR-SAFETY)
        assertThat(baseline.getRiskLevel()).isEqualTo("RED");
        assertThat(withCtx.getRiskLevel()).isEqualTo("RED");
        assertThat(baseline.isEmergencyActionRequired()).isTrue();
        assertThat(withCtx.isEmergencyActionRequired()).isTrue();
        // Context did not enter rule matching: matched rule sets identical
        assertThat(withCtx.getMatchedRules()).isEqualTo(baseline.getMatchedRules());
    }
}
