package com.carebridge.backend.journey.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CanonicalAuditEventMappingTest {

    @Test
    void transitionSerializesAndHydratesCanonicalPayload() {
        String longReason = "r".repeat(500);
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
    void transitionRejectsReasonBeyondLegacyLimit() {
        MotherJourneyTransition transition = MotherJourneyTransition.builder()
                .journeyId(UUID.randomUUID())
                .eventType(JourneyTransitionType.DETAILS_CHANGED)
                .reason("r".repeat(501))
                .build();

        assertThatThrownBy(transition::prepareCanonicalEvent)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Journey transition reason must not exceed 500 characters");
    }

    @Test
    void inferredPregnancyEpochEventHydratesAsCanonicalTransition() {
        MotherJourneyTransition transition = MotherJourneyTransition.builder()
                .journeyId(UUID.randomUUID())
                .payload(Map.of(
                        "eventType", "PREGNANCY_EPOCH_STARTED",
                        "toStage", "PREGNANCY",
                        "changes", Map.of("canonicalLmp", "2026-01-01"),
                        "journeyVersion", 3,
                        "source", "SYSTEM_DERIVED",
                        "confidence", "ESTIMATED",
                        "reason", "CHECKLIST_P2_GESTATIONAL_DATING_BACKFILL"))
                .build();

        transition.hydrateCanonicalEvent();

        assertThat(transition.getEventType()).isEqualTo(JourneyTransitionType.PREGNANCY_EPOCH_STARTED);
        assertThat(transition.getJourneyVersion()).isEqualTo(3L);
        assertThat(transition.getSource()).isEqualTo(JourneyDateSource.SYSTEM_DERIVED);
        assertThat(transition.getConfidence()).isEqualTo(JourneyDateConfidence.ESTIMATED);
    }

    @Test
    void legacyEpochProjectionSerializesCanonicalEventAndCommitBoundary() {
        Instant committed = Instant.parse("2026-07-18T03:00:00Z");
        UUID correlation = UUID.randomUUID();
        MotherJourneyTransition transition = MotherJourneyTransition.builder()
                .journeyId(UUID.randomUUID())
                .eventType(JourneyTransitionType.STAGE_CHANGED)
                .toStage(JourneyType.PREGNANCY)
                .effectiveAt(committed)
                .recordedAt(committed)
                .correlationId(correlation)
                .pregnancyEpochStarted(true)
                .build();

        transition.prepareCanonicalEvent();

        assertThat(transition.getPayload())
                .containsEntry("eventType", "PREGNANCY_EPOCH_STARTED")
                .containsEntry("effectiveFrom", committed.toString())
                .containsEntry("recordedAt", committed.toString())
                .containsEntry("correlationId", correlation.toString())
                .containsKey("gestationalDatingRevision");
    }

    @Test
    void datingCorrectionLeavesEffectiveToForNextEventDerivation() {
        Instant committed = Instant.parse("2026-07-18T03:00:00Z");
        MotherJourneyTransition transition = MotherJourneyTransition.builder()
                .journeyId(UUID.randomUUID())
                .eventType(JourneyTransitionType.DATING_CORRECTED)
                .toStage(JourneyType.PREGNANCY)
                .effectiveAt(committed)
                .recordedAt(committed)
                .gestationalDatingRevision(3L)
                .canonicalLmp(LocalDate.of(2026, 6, 2))
                .supersedesDatingRevision(2L)
                .build();

        transition.prepareCanonicalEvent();

        assertThat(transition.getPayload())
                .containsEntry("supersedesDatingRevision", 2L)
                .containsEntry("effectiveFrom", committed.toString())
                .doesNotContainKey("effectiveTo");
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

    @Test
    void malformedLegacyPayloadsHydrateFailClosedInsteadOfBreakingTimelineReads() {
        MotherJourneyTransition transition = MotherJourneyTransition.builder()
                .payload(Map.ofEntries(
                        Map.entry("eventType", "NOT_A_REAL_EVENT"),
                        Map.entry("fromStage", "NOT_A_REAL_STAGE"),
                        Map.entry("changes", "not-an-object"),
                        Map.entry("journeyVersion", "not-a-number"),
                        Map.entry("source", "UNKNOWN_SOURCE"),
                        Map.entry("confidence", "UNKNOWN_CONFIDENCE"),
                        Map.entry("basis", "UNKNOWN_BASIS"),
                        Map.entry("canonicalLmp", "not-a-date"),
                        Map.entry("gestationalDatingRevision", "9223372036854775808"),
                        Map.entry("supersedesDatingRevision", "not-a-number"),
                        Map.entry("correlationId", "not-a-uuid")))
                .build();
        PregnancyOutcomeEvidence evidence = PregnancyOutcomeEvidence.builder()
                .payload(Map.of(
                        "outcomeType", "NOT_A_REAL_OUTCOME",
                        "outcomeDate", "not-a-date",
                        "revisionNumber", "not-a-number",
                        "journeyVersion", "9223372036854775808",
                        "source", "UNKNOWN_SOURCE",
                        "submissionId", "not-a-uuid",
                        "supersedesEvidenceId", "not-a-uuid"))
                .build();

        assertThatCode(transition::hydrateCanonicalEvent).doesNotThrowAnyException();
        assertThatCode(evidence::hydrateCanonicalEvent).doesNotThrowAnyException();
        assertThat(transition.getEventType()).isEqualTo(JourneyTransitionType.DETAILS_CHANGED);
        assertThat(transition.getChanges()).isEmpty();
        assertThat(transition.getJourneyVersion()).isZero();
        assertThat(transition.getGestationalDatingRevision()).isNull();
        assertThat(transition.getCanonicalLmp()).isNull();
        assertThat(transition.getCorrelationId()).isNull();
        assertThat(evidence.getOutcomeType()).isNull();
        assertThat(evidence.getOutcomeDate()).isNull();
        assertThat(evidence.getRevisionNumber()).isZero();
        assertThat(evidence.getJourneyVersion()).isZero();
        assertThat(evidence.getSubmissionId()).isNull();
        assertThat(evidence.getSupersedesEvidenceId()).isNull();
    }
}
