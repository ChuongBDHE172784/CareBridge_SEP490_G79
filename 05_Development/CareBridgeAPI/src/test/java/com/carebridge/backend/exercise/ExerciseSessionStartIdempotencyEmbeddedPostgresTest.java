package com.carebridge.backend.exercise;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.exercise.dto.StartSessionRequest;
import com.carebridge.backend.exercise.dto.StartSessionResponse;
import com.carebridge.backend.exercise.entity.ExerciseSafetyCheck;
import com.carebridge.backend.exercise.entity.ExerciseSession;
import com.carebridge.backend.exercise.entity.SafetyCheckStatus;
import com.carebridge.backend.exercise.entity.SessionStatus;
import com.carebridge.backend.exercise.repository.ExerciseSafetyCheckRepository;
import com.carebridge.backend.exercise.repository.ExerciseSessionRepository;
import com.carebridge.backend.exercise.service.IExerciseSessionService;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.MotherJourney;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import com.carebridge.backend.testsupport.AbstractEmbeddedPostgresIntegrationTest;
import com.carebridge.backend.testsupport.CanonicalUserFixture;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** Real PostgreSQL regression for idempotent active exercise-session starts. */
class ExerciseSessionStartIdempotencyEmbeddedPostgresTest
        extends AbstractEmbeddedPostgresIntegrationTest {

    private static final UUID BICEP_CURL_ID =
            UUID.fromString("60000000-0000-0000-0000-000000000004");
    private static final UUID SQUAT_ID =
            UUID.fromString("60000000-0000-0000-0000-000000000006");

    @Autowired private IExerciseSessionService sessionService;
    @Autowired private ExerciseSessionRepository sessionRepository;
    @Autowired private ExerciseSafetyCheckRepository safetyCheckRepository;
    @Autowired private MotherJourneyRepository journeyRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private UUID ownerUserId;
    private UUID careSubjectId;
    private UUID journeyId;

    @BeforeEach
    void seedOwnerAndJourney() {
        ownerUserId = UUID.randomUUID();
        careSubjectId = UUID.randomUUID();
        CanonicalUserFixture.insertUser(
                jdbcTemplate,
                ownerUserId,
                "Exercise session owner",
                String.format("09%08d", Math.floorMod(ownerUserId.hashCode(), 100_000_000)),
                "MOTHER");
        jdbcTemplate.update("""
                insert into care_subjects (
                    care_subject_id, person_id, owner_user_id, subject_type,
                    nickname, status, created_at, updated_at)
                values (?, ?, ?, 'MOTHER', 'Exercise session owner', 'ACTIVE', now(), now())
                """, careSubjectId, ownerUserId, ownerUserId);

        journeyId = journeyRepository.saveAndFlush(MotherJourney.builder()
                .ownerUserId(ownerUserId)
                .careSubjectId(careSubjectId)
                .journeyType(JourneyType.PREGNANCY)
                .status(JourneyStatus.ACTIVE)
                .startDate(LocalDate.of(2026, 6, 1))
                .lastMenstrualDate(LocalDate.of(2026, 6, 1))
                .estimatedDueDate(LocalDate.of(2027, 3, 8))
                .build()).getId();
        jdbcTemplate.update(
                "update care_subjects set mother_journey_id=? where care_subject_id=?",
                journeyId, careSubjectId);
    }

    @AfterEach
    void cleanFixture() {
        jdbcTemplate.update("delete from maternal_exercise_sessions where owner_user_id=?", ownerUserId);
        jdbcTemplate.update(
                "delete from health_observations where care_subject_id=? and observation_type='EXERCISE_SAFETY_RESULT'",
                careSubjectId);
        jdbcTemplate.update(
                "update care_subjects set mother_journey_id=null where care_subject_id=?",
                careSubjectId);
        if (journeyId != null) {
            jdbcTemplate.update("delete from mother_journeys where journey_id=?", journeyId);
        }
        jdbcTemplate.update("delete from care_subjects where care_subject_id=?", careSubjectId);
        jdbcTemplate.update("delete from users where user_id=?", ownerUserId);
    }

    @Test
    void repeatedStart_returnsExistingPausedSessionWithoutInsert() {
        UUID safetyCheckId = saveSafetyCheck(BICEP_CURL_ID);
        ExerciseSession existing = saveActiveSession(
                BICEP_CURL_ID, safetyCheckId, SessionStatus.PAUSED);

        StartSessionResponse response = sessionService.startSession(
                BICEP_CURL_ID,
                request(safetyCheckId),
                ownerUserId);

        assertThat(response.getExerciseSessionId()).isEqualTo(existing.getExerciseSessionId());
        assertThat(response.getSessionStatus()).isEqualTo(SessionStatus.PAUSED.name());
        assertThat(response.getStartedAt()).isEqualTo(existing.getStartedAt());
        OffsetDateTime dayStart = OffsetDateTime.now(ZoneOffset.UTC)
                .toLocalDate().atStartOfDay().atOffset(ZoneOffset.UTC);
        assertThat(sessionRepository
                .findFirstByExerciseIdAndUserIdAndSessionStatusInAndStartedAtGreaterThanEqualAndStartedAtLessThanOrderByStartedAtAscExerciseSessionIdAsc(
                        BICEP_CURL_ID,
                        ownerUserId,
                        java.util.List.of(SessionStatus.IN_PROGRESS, SessionStatus.PAUSED),
                        dayStart,
                        dayStart.plusDays(1)))
                .hasValueSatisfying(session -> assertThat(session.getExerciseSessionId())
                        .isEqualTo(existing.getExerciseSessionId()));
    }

    @Test
    void concurrentStarts_returnOneSessionAndPersistOneRow() throws Exception {
        UUID safetyCheckId = saveSafetyCheck(BICEP_CURL_ID);
        StartSessionRequest request = request(safetyCheckId);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<StartSessionResponse> first = executor.submit(
                    () -> sessionService.startSession(BICEP_CURL_ID, request, ownerUserId));
            Future<StartSessionResponse> second = executor.submit(
                    () -> sessionService.startSession(BICEP_CURL_ID, request, ownerUserId));

            StartSessionResponse firstResponse = first.get(30, TimeUnit.SECONDS);
            StartSessionResponse secondResponse = second.get(30, TimeUnit.SECONDS);
            assertThat(secondResponse.getExerciseSessionId())
                    .isEqualTo(firstResponse.getExerciseSessionId());
            assertThat(jdbcTemplate.queryForObject(
                    "select count(*) from maternal_exercise_sessions where owner_user_id=? and exercise_template_id=? and session_status in ('IN_PROGRESS','PAUSED')",
                    Integer.class,
                    ownerUserId,
                    BICEP_CURL_ID)).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void activeSessionForAnotherExercise_doesNotBlockNewExercise() {
        UUID bicepSafetyCheckId = saveSafetyCheck(BICEP_CURL_ID);
        ExerciseSession existingBicep = saveActiveSession(
                BICEP_CURL_ID, bicepSafetyCheckId, SessionStatus.IN_PROGRESS);
        UUID squatSafetyCheckId = saveSafetyCheck(SQUAT_ID);

        StartSessionResponse response = sessionService.startSession(
                SQUAT_ID,
                request(squatSafetyCheckId),
                ownerUserId);

        assertThat(response.getExerciseSessionId()).isNotEqualTo(existingBicep.getExerciseSessionId());
        assertThat(response.getExerciseId()).isEqualTo(SQUAT_ID);
        assertThat(response.getSessionStatus()).isEqualTo(SessionStatus.IN_PROGRESS.name());
        assertThat(sessionRepository.findById(existingBicep.getExerciseSessionId()))
                .hasValueSatisfying(session -> assertThat(session.getSessionStatus())
                        .isEqualTo(SessionStatus.IN_PROGRESS));
    }

    private StartSessionRequest request(UUID safetyCheckId) {
        return StartSessionRequest.builder()
                .safetyCheckId(safetyCheckId)
                .journeyId(journeyId)
                .build();
    }

    private UUID saveSafetyCheck(UUID exerciseId) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return safetyCheckRepository.saveAndFlush(ExerciseSafetyCheck.builder()
                .exerciseId(exerciseId)
                .userId(ownerUserId)
                .journeyId(careSubjectId)
                .answerJson(Map.of(
                        "Q1_NO_DIZZINESS", true,
                        "Q2_NO_CONTRACTIONS", true,
                        "Q3_NO_BLEEDING", true,
                        "Q4_HYDRATED_AND_FED", true))
                .redFlagDetected(false)
                .resultStatus(SafetyCheckStatus.CLEARED)
                .completedAt(now)
                .createdAt(now)
                .build()).getSafetyCheckId();
    }

    private ExerciseSession saveActiveSession(
            UUID exerciseId, UUID safetyCheckId, SessionStatus status) {
        // PostgreSQL timestamptz stores microseconds, but OffsetDateTime.now() carries
        // sub-microsecond digits on this platform. Without truncating, the returned
        // entity keeps nanosecond precision the column never persisted, and comparing
        // it against a value read back from the database fails on those lost digits.
        OffsetDateTime startedAt = OffsetDateTime.now(ZoneOffset.UTC)
                .minusMinutes(1)
                .truncatedTo(ChronoUnit.MICROS);
        return sessionRepository.saveAndFlush(ExerciseSession.builder()
                .exerciseSessionId(UUID.randomUUID())
                .exerciseId(exerciseId)
                .journeyId(journeyId)
                .userId(ownerUserId)
                .safetyCheckId(safetyCheckId)
                .startedAt(startedAt)
                .pausedSeconds(0)
                .warningCount(0)
                .sessionStatus(status)
                .summaryJson("{}")
                .createdAt(startedAt)
                .updatedAt(startedAt)
                .build());
    }
}
