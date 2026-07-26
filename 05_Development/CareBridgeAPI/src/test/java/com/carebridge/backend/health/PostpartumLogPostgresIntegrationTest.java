package com.carebridge.backend.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.consent.entity.ConsentDataType;
import com.carebridge.backend.consent.entity.ConsentGrant;
import com.carebridge.backend.consent.entity.ConsentPurpose;
import com.carebridge.backend.consent.repository.ConsentGrantRepository;
import com.carebridge.backend.health.dto.AddPostpartumLogRequest;
import com.carebridge.backend.health.repository.PostpartumLogRepository;
import com.carebridge.backend.health.service.IPostpartumLogService;
import com.carebridge.backend.journey.dto.CreateJourneyRequest;
import com.carebridge.backend.journey.entity.JourneyDateConfidence;
import com.carebridge.backend.journey.entity.JourneyDateSource;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.LifecycleGoal;
import com.carebridge.backend.journey.entity.MotherBaselineContext;
import com.carebridge.backend.journey.repository.MotherBaselineContextRepository;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import com.carebridge.backend.journey.service.IJourneyTransitionService;
import com.carebridge.backend.journey.service.LifecycleConsentValidator;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.rbac.Role;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Real PostgreSQL coverage for Story 6.4 recovery invariants and idempotency. */
@Isolated
@Execution(ExecutionMode.SAME_THREAD)
class PostpartumLogPostgresIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final LocalDate RECOVERY_START = LocalDate.of(2026, 7, 1);

    @Autowired private UserRepository userRepository;
    @Autowired private MotherBaselineContextRepository baselineRepository;
    @Autowired private ConsentGrantRepository consentRepository;
    @Autowired private MotherJourneyRepository journeyRepository;
    @Autowired private PostpartumLogRepository logRepository;
    @Autowired private IJourneyTransitionService journeyService;
    @Autowired private IPostpartumLogService logService;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private MockMvc mockMvc;

    private UUID motherId;
    private UUID journeyId;

    @BeforeEach
    void setUp() {
        motherId = seedEligibleMother();
        journeyId = createDirectPostpartumJourney();
    }

    @Test
    void ov01E2e006_directPostpartumApiPreservesCardinalityAndZeroBabySideEffects()
            throws Exception {
        UUID submissionId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/journeys/{journeyId}/postpartum-logs", journeyId)
                        .with(user(motherId.toString()).roles("MOTHER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "submissionId":"%s",
                                  "logDate":"%s",
                                  "painLevel":3
                                }
                                """.formatted(submissionId, RECOVERY_START)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.journeyId").value(journeyId.toString()))
                .andExpect(jsonPath("$.data.submissionId").value(submissionId.toString()));

        assertThat(journeyRepository.existsByOwnerUserIdAndStatusAndJourneyTypeIn(
                motherId,
                JourneyStatus.ACTIVE,
                List.of(JourneyType.PRE_PREGNANCY, JourneyType.PREGNANCY, JourneyType.POSTPARTUM)))
                .isTrue();
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from mother_journeys where owner_user_id = ? and status = 'ACTIVE'",
                Long.class,
                motherId)).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from care_subjects where owner_user_id = ? and subject_type = 'BABY'",
                Long.class,
                motherId)).isZero();
        assertThat(postpartumCount(journeyId)).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from maternal_observations "
                        + "where mother_journey_id = ? and submission_id = ? "
                        + "and observation_type = 'POSTPARTUM_LOG'",
                Long.class,
                journeyId,
                submissionId)).isEqualTo(1L);
    }

    @Test
    void listLogs_equalDateAndTimestamp_usesIdAsStablePostgresPageBoundary() {
        Instant sameCreatedAt = Instant.parse("2026-07-20T00:00:00Z");
        List<UUID> ids = List.of(
                UUID.fromString("00000000-0000-4000-8000-000000000001"),
                UUID.fromString("00000000-0000-4000-8000-000000000002"),
                UUID.fromString("00000000-0000-4000-8000-000000000003"));
        for (UUID id : ids) {
            UUID submissionId = UUID.randomUUID();
            jdbcTemplate.update("""
                    insert into health_observations (
                        health_observation_id, care_subject_id, subject_type,
                        observation_type, value_numeric, observed_at,
                        raw_payload_jsonb, source_type, legacy_source, legacy_id,
                        created_at, updated_at)
                    values (?, ?, 'MOTHER', 'POSTPARTUM_LOG', 1, ?,
                            jsonb_build_object('submissionId', CAST(? AS text),
                                               'recordStatus', 'ACTIVE'),
                            'POSTPARTUM_LOG', 'postpartum_logs', ?, ?, ?)
                    """, id, journeyId, RECOVERY_START, submissionId, id.toString(),
                    Timestamp.from(sameCreatedAt), Timestamp.from(sameCreatedAt));

        }

        var first = logService.listLogs(journeyId, motherId, 0, 1);
        var second = logService.listLogs(journeyId, motherId, 1, 1);
        var third = logService.listLogs(journeyId, motherId, 2, 1);

        assertThat(List.of(
                first.getContent().get(0).getPostpartumLogId(),
                second.getContent().get(0).getPostpartumLogId(),
                third.getContent().get(0).getPostpartumLogId()))
                .containsExactly(ids.get(2), ids.get(1), ids.get(0));
    }

    @Test
    void submissionReplayAndDifferentPayload_haveStablePostgresOutcomes() {
        UUID submissionId = UUID.randomUUID();
        var original = request(submissionId, (short) 3);
        UUID firstId = logService.addLog(motherId, journeyId, original).getPostpartumLogId();

        UUID replayId = logService.addLog(
                motherId, journeyId, request(submissionId, (short) 3)).getPostpartumLogId();

        assertThat(replayId).isEqualTo(firstId);
        assertThat(postpartumCount(journeyId)).isEqualTo(1L);
        assertThatThrownBy(() -> logService.addLog(
                motherId, journeyId, request(submissionId, (short) 8)))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getCode())
                        .isEqualTo("POSTPARTUM_SUBMISSION_CONFLICT"));
        assertThat(postpartumCount(journeyId)).isEqualTo(1L);
    }

    @Test
    void concurrentSameSubmission_createsExactlyOnePostgresRow() throws Exception {
        UUID submissionId = UUID.randomUUID();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<UUID> first = executor.submit(() -> {
            start.await();
            return logService.addLog(
                    motherId, journeyId, request(submissionId, (short) 3)).getPostpartumLogId();
        });
        Future<UUID> second = executor.submit(() -> {
            start.await();
            return logService.addLog(
                    motherId, journeyId, request(submissionId, (short) 3)).getPostpartumLogId();
        });

        UUID firstId;
        UUID secondId;
        try {
            start.countDown();
            firstId = first.get(15, TimeUnit.SECONDS);
            secondId = second.get(15, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        assertThat(secondId).isEqualTo(firstId);
        assertThat(jdbcTemplate.queryForObject(
<<<<<<< Updated upstream
                "select count(*) from maternal_observations "
                        + "where legacy_source = 'POSTPARTUM_LOG' "
                        + "and mother_journey_id = ? and submission_id = ?",
=======
                """
                select count(*) from health_observations
                 where care_subject_id = ?
                   and legacy_source = 'postpartum_logs'
                   and raw_payload_jsonb->>'submissionId' = CAST(? AS text)
                """,
>>>>>>> Stashed changes
                Long.class,
                journeyId,
                submissionId)).isEqualTo(1L);
    }

    private UUID seedEligibleMother() {
        User mother = userRepository.save(User.builder()
                .email("story64-postpartum-" + UUID.randomUUID() + "@carebridge.test")
                .passwordHash("$2a$10$abcdefghijklmnopqrstuv")
                .name("Story 6.4 Mother")
                .role(Role.MOTHER)
                .enabled(true)
                .locked(false)
                .accountStatus("ACTIVE")
                .emailVerified(true)
                .phoneVerified(false)
                .build());
        UUID submissionId = UUID.randomUUID();
        baselineRepository.save(MotherBaselineContext.builder()
                .id(UUID.randomUUID())
                .ownerUserId(mother.getId())
                .submissionId(submissionId)
                .revision(1L)
                .schemaVersion("1.0")
                .source("SELF_REPORTED")
                .lifecycleGoal(LifecycleGoal.POSTPARTUM_RECOVERY)
                .locale("vi-VN")
                .timeZone("Asia/Ho_Chi_Minh")
                .preferences("{}")
                .recordedAt(Instant.now())
                .build());
        consentRepository.save(ConsentGrant.builder()
                .userId(mother.getId())
                .dataType(ConsentDataType.MOTHER_BASELINE)
                .purpose(ConsentPurpose.PERSONALIZE)
                .scope(LifecycleConsentValidator.LIFECYCLE_SCOPE)
                .policyVersion(LifecycleConsentValidator.POLICY_VERSION)
                .evidenceKey(submissionId)
                .locale("vi-VN")
                .consentGivenAt(Instant.now())
                .expiryAt(Instant.now().plusSeconds(3600))
                .build());
        return mother.getId();
    }

    private UUID createDirectPostpartumJourney() {
        CreateJourneyRequest request = new CreateJourneyRequest();
        request.setJourneyType(JourneyType.POSTPARTUM);
        request.setStartDate(RECOVERY_START);
        request.setDateSource(JourneyDateSource.SELF_REPORTED);
        request.setDateConfidence(JourneyDateConfidence.CONFIRMED);
        request.setChangeReason("STORY_6_4_POSTGRES_TEST");
        request.setEffectiveAt(Instant.now());
        return journeyService.createJourney(request, motherId).getId();
    }

    private AddPostpartumLogRequest request(UUID submissionId, short painLevel) {
        AddPostpartumLogRequest request = new AddPostpartumLogRequest();
        request.setSubmissionId(submissionId);
        request.setLogDate(RECOVERY_START);
        request.setPainLevel(painLevel);
        request.setMoodLevel((short) 6);
        request.setSleepHours(new BigDecimal("6.5"));
        request.setSymptomNote("Synthetic integration observation");
        return request;
    }

    private long postpartumCount(UUID targetJourneyId) {
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from maternal_observations "
                        + "where legacy_source = 'POSTPARTUM_LOG' and mother_journey_id = ?",
                Long.class,
                targetJourneyId);
        return count == null ? 0 : count;
    }
}
