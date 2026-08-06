package com.carebridge.backend.exercise;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.exercise.dto.PostureEventRequest;
import com.carebridge.backend.exercise.dto.PostureFeedbackResponse;
import com.carebridge.backend.exercise.service.IPostureAnalysisService;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.MotherJourney;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import com.carebridge.backend.testsupport.AbstractEmbeddedPostgresIntegrationTest;
import com.carebridge.backend.testsupport.CanonicalUserFixture;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** PostgreSQL regression for journey-backed posture analysis transaction ownership. */
@TestPropertySource(properties = "carebridge.exercise-correction.enabled=false")
class ExercisePostureEventsEmbeddedPostgresTest extends AbstractEmbeddedPostgresIntegrationTest {

    private static final UUID DEMO_EXERCISE_ID =
            UUID.fromString("60000000-0000-0000-0000-000000000004");
    private static final String POSTURE_EVENT_BODY = """
            {
              "eventTimeMs": 1000,
              "keypointSummaryJson": {
                "nose": {"x": 0.5, "y": 0.4, "z": 0.0, "visibility": 0.99}
              }
            }
            """;

    @Autowired private IPostureAnalysisService postureAnalysisService;
    @Autowired private MotherJourneyRepository motherJourneyRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private MockMvc mockMvc;

    private UUID ownerUserId;
    private UUID careSubjectId;
    private UUID journeyId;
    private UUID sessionId;

    @BeforeEach
    void seedJourneyBackedSession() {
        ownerUserId = UUID.randomUUID();
        careSubjectId = UUID.randomUUID();
        sessionId = UUID.randomUUID();

        CanonicalUserFixture.insertUser(
                jdbcTemplate,
                ownerUserId,
                "Posture event owner",
                String.format("09%08d", Math.floorMod(ownerUserId.hashCode(), 100_000_000)),
                "MOTHER");
        jdbcTemplate.update("""
                insert into care_subjects (
                    care_subject_id, person_id, owner_user_id, subject_type,
                    nickname, status, created_at, updated_at)
                values (?, ?, ?, 'MOTHER', 'Posture event owner', 'ACTIVE', now(), now())
                """, careSubjectId, ownerUserId, ownerUserId);

        journeyId = motherJourneyRepository.saveAndFlush(MotherJourney.builder()
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
                journeyId,
                careSubjectId);
        jdbcTemplate.update("""
                insert into maternal_exercise_sessions (
                    exercise_session_id, mother_journey_id, owner_user_id,
                    exercise_template_id, started_at, paused_seconds,
                    session_status, warning_count, summary_jsonb, created_at, updated_at)
                values (?, ?, ?, ?, now(), 0, 'IN_PROGRESS', 0, '{}'::jsonb, now(), now())
                """, sessionId, journeyId, ownerUserId, DEMO_EXERCISE_ID);

        assertThat(jdbcTemplate.queryForObject("""
                select count(*)
                  from care_item_templates
                 where entry_type='POSTURE_CONFIG'
                   and parent_template_id=?
                   and template_status='ACTIVE'
                   and (effective_to is null or effective_to > now())
                """, Integer.class, DEMO_EXERCISE_ID)).isOne();
    }

    @AfterEach
    void cleanFixture() {
        if (sessionId != null) {
            jdbcTemplate.update(
                    "delete from health_observations where source_record_id=?", sessionId);
            jdbcTemplate.update(
                    "delete from maternal_exercise_sessions where exercise_session_id=?", sessionId);
        }
        if (careSubjectId != null) {
            jdbcTemplate.update(
                    "update care_subjects set mother_journey_id=null where care_subject_id=?",
                    careSubjectId);
        }
        if (journeyId != null) {
            jdbcTemplate.update("delete from mother_journeys where journey_id=?", journeyId);
        }
        if (careSubjectId != null) {
            jdbcTemplate.update("delete from care_subjects where care_subject_id=?", careSubjectId);
        }
        if (ownerUserId != null) {
            jdbcTemplate.update("delete from users where user_id=?", ownerUserId);
        }
    }

    @Test
    void journeyBackedEventUsesProxiedServiceTransactionOutsideTestTransaction() {
        assertThat(AopUtils.isAopProxy(postureAnalysisService)).isTrue();
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();

        ApiResponse<PostureFeedbackResponse> response = postureAnalysisService.analyzePosture(
                sessionId, ownerUserId, postureRequest());

        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
        assertThat(response.getData().getPostureCode()).isEqualTo("MODEL_UNAVAILABLE");
        assertPersistedUnavailableEvent();
    }

    @Test
    void providerDisabledFallbackReturnsHttp200AndPersistsEvent() throws Exception {
        mockMvc.perform(post("/api/v1/exercises/sessions/" + sessionId + "/posture-events")
                        .with(csrf())
                        .with(user(ownerUserId.toString()).roles("MOTHER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(POSTURE_EVENT_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.postureCode").value("MODEL_UNAVAILABLE"))
                .andExpect(jsonPath("$.data.severity").value("WARNING"));

        assertPersistedUnavailableEvent();
    }

    private PostureEventRequest postureRequest() {
        PostureEventRequest request = new PostureEventRequest();
        request.setEventTimeMs(1000L);
        request.setKeypointSummaryJson(Map.<String, Object>of(
                "nose", Map.of("x", 0.5, "y", 0.4, "z", 0.0, "visibility", 0.99)));
        return request;
    }

    private void assertPersistedUnavailableEvent() {
        assertThat(jdbcTemplate.queryForObject("""
                select count(*)
                  from health_observations
                 where source_record_id=? and observation_type='POSTURE_FEEDBACK'
                """, Integer.class, sessionId)).isOne();
        assertThat(jdbcTemplate.queryForObject("""
                select raw_payload_jsonb ->> 'postureCode'
                  from health_observations
                 where source_record_id=? and observation_type='POSTURE_FEEDBACK'
                """, String.class, sessionId)).isEqualTo("MODEL_UNAVAILABLE");
        assertThat(jdbcTemplate.queryForObject("""
                select raw_payload_jsonb ->> 'analysisStatus'
                  from health_observations
                 where source_record_id=? and observation_type='POSTURE_FEEDBACK'
                """, String.class, sessionId)).isEqualTo("MODEL_UNAVAILABLE");
    }
}
