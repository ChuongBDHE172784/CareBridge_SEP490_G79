package com.carebridge.backend.journey.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CanonicalAuditEventMappingTest {

    @Test
    void transitionSerializesAndHydratesCanonicalPayload() {
        String longReason = "r".repeat(600);
        MotherJourneyTransition transition = MotherJourneyTransition.builder()
                .journeyId(UUID.randomUUID())
                .eventType(JourneyTransitionType.STAGE_CHANGED)
                .fromStage(JourneyType.PREGNANCY)
                .toStage(JourneyType.POSTPARTUM)
                .changes(Map.of("status", "ACTIVE"))
                .source(JourneyDateSource.CLINICIAN_CONFIRMED)
                .confidence(JourneyDateConfidence.CONFIRMED)
                .reason(longReason)
                .journeyVersion(4)
                .build();

        transition.prepareCanonicalEvent();

        assertThat(transition.getEventCategory()).isEqualTo("MOTHER_JOURNEY_TRANSITION");
        assertThat(transition.getPayload())
                .containsEntry("eventType", "STAGE_CHANGED")
                .containsEntry("source", "CLINICIAN_CONFIRMED")
                .containsEntry("confidence", "CONFIRMED")
                .containsEntry("reason", longReason)
                .containsEntry("journeyVersion", 4L);

        transition.setEventType(null);
        transition.setChanges(null);
        transition.setJourneyVersion(0);
        transition.setSource(null);
        transition.setConfidence(null);
        transition.setReason(null);
        transition.hydrateCanonicalEvent();

        assertThat(transition.getEventType()).isEqualTo(JourneyTransitionType.STAGE_CHANGED);
        assertThat(transition.getChanges()).containsEntry("status", "ACTIVE");
        assertThat(transition.getJourneyVersion()).isEqualTo(4);
        assertThat(transition.getSource()).isEqualTo(JourneyDateSource.CLINICIAN_CONFIRMED);
        assertThat(transition.getConfidence()).isEqualTo(JourneyDateConfidence.CONFIRMED);
        assertThat(transition.getReason()).isEqualTo(longReason);
        assertThat(transition.getResourceType()).isEqualTo("mother_journeys");
    }

    @Test
    void pregnancyOutcomeKeepsRevisionAndClinicalFieldsInPayload() {
        UUID submissionId = UUID.randomUUID();
        UUID supersedes = UUID.randomUUID();
        String longReason = "clinical ".repeat(80);
        PregnancyOutcomeEvidence evidence = PregnancyOutcomeEvidence.builder()
                .journeyId(UUID.randomUUID())
                .submissionId(submissionId)
                .outcomeType(PregnancyOutcomeType.LIVE_BIRTH)
                .outcomeDate(LocalDate.of(2026, 7, 1))
                .source(JourneyDateSource.CLINICIAN_CONFIRMED)
                .reason(longReason)
                .supersedesEvidenceId(supersedes)
                .semanticHash("semantic-hash")
                .revisionNumber(2)
                .journeyVersion(7)
                .correction(true)
                .build();

        evidence.prepareCanonicalEvent();

        assertThat(evidence.getPayload())
                .containsEntry("outcomeType", "LIVE_BIRTH")
                .containsEntry("outcomeDate", "2026-07-01")
                .containsEntry("revisionNumber", 2)
                .containsEntry("journeyVersion", 7L)
                .containsEntry("submissionId", submissionId)
                .containsEntry("reason", longReason)
                .containsEntry("supersedesEvidenceId", supersedes)
                .containsEntry("correction", true);

        evidence.setSource(null);
        evidence.setReason(null);
        evidence.setSubmissionId(null);
        evidence.hydrateCanonicalEvent();
        assertThat(evidence.getSource()).isEqualTo(JourneyDateSource.CLINICIAN_CONFIRMED);
        assertThat(evidence.getReason()).isEqualTo(longReason);
        assertThat(evidence.getSubmissionId()).isEqualTo(submissionId);
    }

    @Test
    void baselineUsesCorrelationAndPayloadProjection() {
        MotherBaselineContext baseline = MotherBaselineContext.builder()
                .id(UUID.randomUUID())
                .submissionId(UUID.randomUUID())
                .journeyId(UUID.randomUUID())
                .source("ONBOARDING")
                .revision(3)
                .schemaVersion("2")
                .lifecycleGoal(LifecycleGoal.CURRENTLY_PREGNANT)
                .locale("vi-VN")
                .timeZone("Asia/Ho_Chi_Minh")
                .preferences("daily")
                .effectiveAt(Instant.parse("2026-07-26T01:00:00Z"))
                .build();

        baseline.prepareCanonicalEvent();

        assertThat(baseline.getRecordedAt()).isEqualTo(baseline.getEffectiveAt());
        assertThat(baseline.getPayload())
                .containsEntry("revision", 3L)
                .containsEntry("schemaVersion", "2")
                .containsEntry("source", "ONBOARDING")
                .containsEntry("lifecycleGoal", "CURRENTLY_PREGNANT");
    }

    @Test
    void legacyJourneyPayloadsUseExplicitCompatibilityDefaults() {
        MotherBaselineContext baseline = MotherBaselineContext.builder()
                .payload(Map.of("source", "MIGRATED"))
                .build();
        baseline.hydrateCanonicalEvent();

        PregnancyOutcomeEvidence evidence = PregnancyOutcomeEvidence.builder()
                .payload(Map.of("revisionNumber", 1))
                .build();
        evidence.hydrateCanonicalEvent();

        assertThat(baseline.getRevision()).isZero();
        assertThat(baseline.getSchemaVersion()).isEqualTo("MOTHER_BASELINE_V1");
        assertThat(evidence.getJourneyVersion()).isZero();
    }
}
