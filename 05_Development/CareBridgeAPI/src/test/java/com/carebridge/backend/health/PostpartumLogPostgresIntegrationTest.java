package com.carebridge.backend.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.consent.entity.ConsentDataType;
import com.carebridge.backend.consent.entity.ConsentGrant;
import com.carebridge.backend.consent.entity.ConsentPurpose;
import com.carebridge.backend.consent.repository.ConsentGrantRepository;
import com.carebridge.backend.health.dto.AddPostpartumLogRequest;
import com.carebridge.backend.health.dto.PostpartumLogResponse;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** Real PostgreSQL coverage for Story 6.4 recovery invariants and idempotency. */
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

    private UUID motherId;
    private UUID journeyId;

    @BeforeEach
    void setUp() {
        wipeUsersAndDependents();
        motherId = seedEligibleMother();
        journeyId = createDirectPostpartumJourney();
    }

    @AfterEach
    void cleanUp() {
        wipeUsersAndDependents();
    }

    @Test
    void directPostpartumAndRecoveryLog_preserveSingleCanonicalAndZeroBabySideEffects() {
        PostpartumLogResponse created = logService.addLog(
                motherId, journeyId, request(UUID.randomUUID(), (short) 3));

        assertThat(created.getJourneyId()).isEqualTo(journeyId);
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
                "select count(*) from baby_profiles where owner_user_id = ?",
                Long.class,
                motherId)).isZero();
        assertThat(logRepository.count()).isEqualTo(1L);
    }

    @Test
    void listLogs_equalDateAndTimestamp_usesIdAsStablePostgresPageBoundary() {
        Instant sameCreatedAt = Instant.parse("2026-07-20T00:00:00Z");
        List<UUID> ids = List.of(
                UUID.fromString("00000000-0000-4000-8000-000000000001"),
                UUID.fromString("00000000-0000-4000-8000-000000000002"),
                UUID.fromString("00000000-0000-4000-8000-000000000003"));
        for (UUID id : ids) {
            jdbcTemplate.update("""
                    insert into postpartum_logs (
                        postpartum_log_id, journey_id, submission_id, log_date,
                        pain_level, status, created_at, updated_at)
                    values (?, ?, ?, ?, 1, 'ACTIVE', ?, ?)
                    """, id, journeyId, UUID.randomUUID(), RECOVERY_START,
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
        assertThat(logRepository.count()).isEqualTo(1L);
        assertThatThrownBy(() -> logService.addLog(
                motherId, journeyId, request(submissionId, (short) 8)))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getCode())
                        .isEqualTo("POSTPARTUM_SUBMISSION_CONFLICT"));
        assertThat(logRepository.count()).isEqualTo(1L);
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
                "select count(*) from postpartum_logs where journey_id = ? and submission_id = ?",
                Long.class,
                journeyId,
                submissionId)).isEqualTo(1L);
    }

    private UUID seedEligibleMother() {
        User mother = userRepository.save(User.builder()
                .email("story64-postpartum@carebridge.test")
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

    private void wipeUsersAndDependents() {
        jdbcTemplate.execute("TRUNCATE TABLE users CASCADE");
    }
}
