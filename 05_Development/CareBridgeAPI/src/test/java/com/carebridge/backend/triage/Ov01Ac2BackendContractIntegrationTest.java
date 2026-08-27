package com.carebridge.backend.triage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.repository.AuditLogRepository;
import com.carebridge.backend.baby.entity.BabyProfile;
import com.carebridge.backend.baby.entity.BabyProfileStatus;
import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.MotherJourney;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import com.carebridge.backend.journey.service.LifecycleSafetyOutcomeProjector;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import com.carebridge.backend.testsupport.CanonicalUserFixture;
import com.carebridge.backend.triage.entity.IntakeSession;
import com.carebridge.backend.triage.exception.TriageException;
import com.carebridge.backend.triage.repository.IIntakeSessionRepository;
import com.carebridge.backend.triage.service.ITriageContinuationService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** Story 6.10 AC2 real Spring/PostgreSQL contracts for E2E-006/008/015. */
@Execution(ExecutionMode.SAME_THREAD)
class Ov01Ac2BackendContractIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private LifecycleSafetyOutcomeProjector outcomeProjector;
    @Autowired private ITriageContinuationService continuationService;
    @Autowired private IIntakeSessionRepository intakeRepository;
    @Autowired private MotherJourneyRepository journeyRepository;
    @Autowired private BabyProfileRepository babyRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private AuditLogRepository auditRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    @Transactional
    void ov01E2e008_greenProjectsTypedOriginsExactlyOnceWithoutEmergencySideEffects() {
        User mother = seedUser("Story 6.10 E2E-008 Mother");
        MotherJourney journey = seedJourney(mother.getId());
        BabyProfile baby = babyRepository.saveAndFlush(BabyProfile.builder()
                .ownerUserId(mother.getId())
                .nickname("E2E-008 synthetic baby")
                .birthDate(LocalDate.of(2026, 7, 1))
                .status(BabyProfileStatus.ACTIVE)
                .active(true)
                .build());

        for (OriginDashboard origin : OriginDashboard.values()) {
            UUID originReference = origin == OriginDashboard.MOTHER_JOURNEY
                    ? journey.getId() : baby.getId();
            TriageStage stage = origin == OriginDashboard.MOTHER_JOURNEY
                    ? TriageStage.POSTPARTUM : TriageStage.INFANT;
            UUID boundJourneyId = origin == OriginDashboard.MOTHER_JOURNEY
                    ? journey.getId() : null;
            IntakeSession intake = completedGreenIntake(
                    mother.getId(), boundJourneyId, origin, originReference, stage);

            var first = outcomeProjector.ensureProjected(intake.getId(), mother.getId());
            var replay = outcomeProjector.ensureProjected(intake.getId(), mother.getId());

            assertThat(first.created()).isTrue();
            assertThat(first.outcomeId()).isNotNull();
            assertThat(replay.created()).isFalse();
            assertThat(replay.outcomeId()).isNull();
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM audit_events "
                            + "WHERE payload->>'triageSessionId' = CAST(? AS text) "
                            + "AND event_category = 'SAFETY_OUTCOME'",
                    Long.class, intake.getId())).isOne();
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM audit_events "
                            + "WHERE payload->>'triageSessionId' = CAST(? AS text) "
                            + "AND payload->>'emergencySessionId' IS NOT NULL",
                    Long.class, intake.getId())).isZero();
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM safety_events WHERE triage_handoff_id = ?",
                    Long.class, intake.getId())).isZero();
            assertThat(auditRepository.findByEntityIdAndAction(
                    first.outcomeId(), AuditAction.AI_TRIAGE)).hasSize(1);
        }
    }

    @Test
    @Transactional
    void ov01E2e015_accountBCannotResolveOrAcknowledgeAccountAContinuation() {
        User accountA = seedUser("Story 6.10 E2E-015 A");
        User accountB = seedUser("Story 6.10 E2E-015 B");
        MotherJourney journey = seedJourney(accountA.getId());
        UUID token = UUID.randomUUID();
        IntakeSession intake = completedGreenIntake(
                accountA.getId(), journey.getId(), OriginDashboard.MOTHER_JOURNEY,
                journey.getId(), TriageStage.POSTPARTUM, token);

        assertThatThrownBy(() -> continuationService.resolve(
                accountB.getId(), token.toString()))
                .isInstanceOfSatisfying(TriageException.class, error -> {
                    assertThat(error.getCode()).isEqualTo("TRIAGE-014");
                    assertThat(error.getHttpStatus().value()).isEqualTo(404);
                });
        assertThatThrownBy(() -> continuationService.acknowledge(
                accountB.getId(), token.toString()))
                .isInstanceOfSatisfying(TriageException.class, error -> {
                    assertThat(error.getCode()).isEqualTo("TRIAGE-014");
                    assertThat(error.getHttpStatus().value()).isEqualTo(404);
                });

        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM triage_sessions WHERE triage_session_id = ? AND user_id = ?",
                Long.class, intake.getId(), accountA.getId())).isOne();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM triage_sessions WHERE triage_session_id = ? AND user_id = ?",
                Long.class, intake.getId(), accountB.getId())).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM triage_sessions "
                        + "WHERE triage_session_id = ? AND continuation_acknowledged_at IS NOT NULL",
                Long.class, intake.getId())).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM audit_events "
                        + "WHERE payload->>'triageSessionId' = CAST(? AS text)",
                Long.class, intake.getId())).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM safety_events WHERE triage_handoff_id = ?",
                Long.class, intake.getId())).isZero();
    }

    private IntakeSession completedGreenIntake(
            UUID ownerId,
            UUID journeyId,
            OriginDashboard origin,
            UUID originReference,
            TriageStage stage) {
        return completedGreenIntake(
                ownerId, journeyId, origin, originReference, stage, UUID.randomUUID());
    }

    private IntakeSession completedGreenIntake(
            UUID ownerId,
            UUID journeyId,
            OriginDashboard origin,
            UUID originReference,
            TriageStage stage,
            UUID continuationToken) {
        return intakeRepository.saveAndFlush(IntakeSession.builder()
                .userId(ownerId)
                .journeyId(journeyId)
                .originDashboard(origin)
                .originReferenceId(originReference)
                .babyProfileId(origin == OriginDashboard.BABY_PROFILE
                        ? originReference : null)
                .stage(stage)
                .riskLevel(RiskLevel.GREEN)
                .status(IntakeStatus.COMPLETED)
                .continuationToken(continuationToken)
                .continuationExpiresAt(Instant.now().plusSeconds(3600))
                .symptoms("synthetic E2E contract fixture")
                .createdAt(Instant.now())
                .completedAt(Instant.now())
                .createdBy(ownerId)
                .build());
    }

    private User seedUser(String name) {
        UUID userId = UUID.randomUUID();
        CanonicalUserFixture.insertUser(
                jdbcTemplate, userId, name, "09" + String.format("%08d",
                        Math.floorMod(System.nanoTime(), 100_000_000L)), "MOTHER");
        return userRepository.findById(userId).orElseThrow();
    }

    private MotherJourney seedJourney(UUID ownerId) {
        UUID careSubjectId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO care_subjects (
                    care_subject_id, person_id, owner_user_id, subject_type,
                    nickname, status, created_at, updated_at)
                SELECT ?, u.user_id, u.user_id, 'MOTHER', u.display_name,
                       'ACTIVE', now(), now()
                  FROM users u
                 WHERE u.user_id = ?
                """, careSubjectId, ownerId);
        MotherJourney journey = journeyRepository.saveAndFlush(MotherJourney.builder()
                .ownerUserId(ownerId)
                .careSubjectId(careSubjectId)
                .journeyType(JourneyType.POSTPARTUM)
                .status(JourneyStatus.ACTIVE)
                .startDate(LocalDate.of(2026, 7, 1))
                .build());
        jdbcTemplate.update(
                "UPDATE care_subjects SET mother_journey_id = ? WHERE care_subject_id = ?",
                journey.getId(), careSubjectId);
        return journey;
    }
}
