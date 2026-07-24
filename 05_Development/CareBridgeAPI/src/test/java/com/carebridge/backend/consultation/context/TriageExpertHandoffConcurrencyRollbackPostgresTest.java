package com.carebridge.backend.consultation.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.consultation.context.dto.HandoffCreateResponse;
import com.carebridge.backend.consultation.context.dto.TriageExpertHandoffCreateRequest;
import com.carebridge.backend.consultation.context.exception.TriageExpertHandoffException;
import com.carebridge.backend.consultation.context.policy.TriageExpertHandoffPolicy;
import com.carebridge.backend.consultation.context.service.ITriageExpertHandoffService;
import com.carebridge.backend.consultation.dto.request.CreateConsultationRequestRequest;
import com.carebridge.backend.consultation.service.IConsultationRequestService;
import com.carebridge.backend.expert.entity.ExpertProfile;
import com.carebridge.backend.expert.repository.ExpertProfileRepository;
import com.carebridge.backend.expert.truststatus.TrustStatus;
import com.carebridge.backend.expert.verificationstatus.VerificationStatus;
import com.carebridge.backend.integration.zegocloud.IZegoCloudService;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.MotherJourney;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import com.carebridge.backend.notification.service.IConsultationRequestNotificationService;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.rbac.Role;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import com.carebridge.backend.triage.IntakeStatus;
import com.carebridge.backend.triage.OriginDashboard;
import com.carebridge.backend.triage.RiskLevel;
import com.carebridge.backend.triage.TriageStage;
import com.carebridge.backend.triage.dto.response.TriageResultResponse;
import com.carebridge.backend.triage.entity.EvidenceSource;
import com.carebridge.backend.triage.entity.IntakeSession;
import com.carebridge.backend.triage.repository.EvidenceSourceRepository;
import com.carebridge.backend.triage.repository.IIntakeSessionRepository;
import com.carebridge.backend.triage.service.ITriageService;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@Execution(ExecutionMode.SAME_THREAD)
class TriageExpertHandoffConcurrencyRollbackPostgresTest
        extends AbstractPostgresIntegrationTest {

    @Autowired private ITriageExpertHandoffService handoffService;
    @Autowired private IConsultationRequestService consultationRequestService;
    @Autowired private UserRepository userRepository;
    @Autowired private ExpertProfileRepository expertProfileRepository;
    @Autowired private MotherJourneyRepository motherJourneyRepository;
    @Autowired private IIntakeSessionRepository intakeRepository;
    @Autowired private EvidenceSourceRepository evidenceSourceRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private DataSource dataSource;

    @MockitoBean private ITriageService triageService;
    @MockitoBean private IZegoCloudService zegoCloudService;
    @MockitoBean private IConsultationRequestNotificationService notificationService;

    @Test
    void concurrentSameOwnerAndKeyCreatesOneAggregateAndReturnsNewThenReplay()
            throws Exception {
        Fixture fixture = seedFixture(false);
        UUID key = UUID.randomUUID();
        var request = handoffRequest(key, fixture.expert().getExpertProfileId());
        CountDownLatch start = new CountDownLatch(1);

        List<HandoffCreateResponse> responses = new ArrayList<>();
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<HandoffCreateResponse> first = executor.submit(() -> {
                start.await();
                return handoffService.create(fixture.intake().getId(), request, fixture.mother().getId());
            });
            Future<HandoffCreateResponse> second = executor.submit(() -> {
                start.await();
                return handoffService.create(fixture.intake().getId(), request, fixture.mother().getId());
            });
            start.countDown();
            responses.add(first.get(10, TimeUnit.SECONDS));
            responses.add(second.get(10, TimeUnit.SECONDS));
        }

        assertThat(responses).extracting(HandoffCreateResponse::replayed)
                .containsExactlyInAnyOrder(false, true);
        assertThat(responses).extracting(HandoffCreateResponse::consultationRequestId)
                .containsOnly(responses.getFirst().consultationRequestId());
        assertAggregateCounts(fixture.mother().getId(), 1, 1, 1, 0);
        verify(notificationService, timeout(2_000).times(1))
                .notifyCreated(
                        fixture.expertUser().getId(),
                        fixture.mother().getId(),
                        responses.getFirst().consultationRequestId());
    }

    @Test
    void changedIntentAndPreExistingGenericKeyBothFailAsIdempotencyConflicts() {
        Fixture changedIntentFixture = seedFixture(false);
        UUID changedIntentKey = UUID.randomUUID();
        handoffService.create(
                changedIntentFixture.intake().getId(),
                handoffRequest(changedIntentKey, changedIntentFixture.expert().getExpertProfileId()),
                changedIntentFixture.mother().getId());
        IntakeSession otherIntake = seedAdditionalIntake(changedIntentFixture);

        assertConflict(() -> handoffService.create(
                otherIntake.getId(),
                handoffRequest(changedIntentKey, changedIntentFixture.expert().getExpertProfileId()),
                changedIntentFixture.mother().getId()));
        assertAggregateCounts(changedIntentFixture.mother().getId(), 1, 1, 1, 0);

        Fixture genericCollisionFixture = seedFixture(false);
        UUID genericKey = UUID.randomUUID();
        consultationRequestService.create(
                CreateConsultationRequestRequest.builder()
                        .clientRequestId(genericKey)
                        .expertProfileId(genericCollisionFixture.expert().getExpertProfileId())
                        .topic(TriageExpertHandoffPolicy.TOPIC)
                        .description(TriageExpertHandoffPolicy.DESCRIPTION)
                        .build(),
                genericCollisionFixture.mother().getId());

        assertConflict(() -> handoffService.create(
                genericCollisionFixture.intake().getId(),
                handoffRequest(genericKey, genericCollisionFixture.expert().getExpertProfileId()),
                genericCollisionFixture.mother().getId()));
        assertAggregateCounts(genericCollisionFixture.mother().getId(), 1, 0, 0, 0);
    }

    @Test
    void postRequestContextPersistenceFailureRollsBackAggregateAndAfterCommitNotification() {
        Fixture fixture = seedFixture(false);
        UUID key = UUID.randomUUID();
        String suffix = key.toString().replace("-", "");
        String function = "story68_fail_context_" + suffix;
        String trigger = "trg_story68_fail_context_" + suffix;
        jdbcTemplate.execute("CREATE FUNCTION " + function + "() RETURNS trigger AS $$ "
                + "BEGIN RAISE EXCEPTION 'synthetic context failure'; END; $$ LANGUAGE plpgsql");
        jdbcTemplate.execute("CREATE TRIGGER " + trigger
                + " BEFORE INSERT ON consultation_context_shares FOR EACH ROW EXECUTE FUNCTION "
                + function + "()");

        try {
            assertThatThrownBy(() -> handoffService.create(
                            fixture.intake().getId(),
                            handoffRequest(key, fixture.expert().getExpertProfileId()),
                            fixture.mother().getId()))
                    .isInstanceOfSatisfying(
                            TriageExpertHandoffException.class,
                            error -> assertThat(error.getCode()).isEqualTo("HANDOFF-010"));
        } finally {
            jdbcTemplate.execute("DROP TRIGGER " + trigger + " ON consultation_context_shares");
            jdbcTemplate.execute("DROP FUNCTION " + function + "()");
        }

        assertAggregateCounts(fixture.mother().getId(), 0, 0, 0, 0);
        assertThat(notificationCount(fixture.expertUser().getId())).isZero();
        verify(notificationService, after(500).never())
                .notifyCreated(
                        eq(fixture.expertUser().getId()),
                        eq(fixture.mother().getId()),
                        any(UUID.class));
    }

    @Test
    void expertEligibilityLossWhileHandoffWaitsOnIntakeLockFailsClosed() throws Exception {
        Fixture fixture = seedFixture(false);
        UUID key = UUID.randomUUID();
        try (Connection locker = dataSource.getConnection();
                ExecutorService executor = Executors.newSingleThreadExecutor()) {
            locker.setAutoCommit(false);
            lockRow(locker, "SELECT id FROM intake_sessions WHERE id = ? FOR UPDATE", fixture.intake().getId());

            Future<HandoffCreateResponse> handoff = executor.submit(() -> handoffService.create(
                    fixture.intake().getId(),
                    handoffRequest(key, fixture.expert().getExpertProfileId()),
                    fixture.mother().getId()));
            awaitBlockedQuery("intake_sessions");
            update(locker, "UPDATE expert_profiles SET trust_status = 'REVOKED' "
                    + "WHERE expert_profile_id = ?", fixture.expert().getExpertProfileId());
            locker.commit();

            assertHandoffFailure(handoff, "HANDOFF-004");
        }

        assertAggregateCounts(fixture.mother().getId(), 0, 0, 0, 0);
        assertThat(notificationCount(fixture.expertUser().getId())).isZero();
    }

    @Test
    void evidenceApprovalLossWhileResolverWaitsOnSourceLockExcludesCitation() throws Exception {
        Fixture fixture = seedFixture(true);
        UUID key = UUID.randomUUID();
        try (Connection locker = dataSource.getConnection();
                ExecutorService executor = Executors.newSingleThreadExecutor()) {
            locker.setAutoCommit(false);
            lockRow(
                    locker,
                    "SELECT id FROM evidence_sources WHERE id = ? FOR UPDATE",
                    fixture.evidenceSource().getId());

            Future<HandoffCreateResponse> handoff = executor.submit(() -> handoffService.create(
                    fixture.intake().getId(),
                    handoffRequest(key, fixture.expert().getExpertProfileId()),
                    fixture.mother().getId()));
            awaitBlockedQuery("evidence_sources");
            update(locker, "UPDATE evidence_sources SET status = 'DEPRECATED', updated_at = now() "
                    + "WHERE id = ?", fixture.evidenceSource().getId());
            locker.commit();

            HandoffCreateResponse response = handoff.get(10, TimeUnit.SECONDS);
            assertThat(response.replayed()).isFalse();
            assertThat(response.context().citations()).isEmpty();
        }

        assertAggregateCounts(fixture.mother().getId(), 1, 1, 1, 0);
    }

    private Fixture seedFixture(boolean withCitation) {
        User mother = userRepository.save(User.builder()
                .phone(uniquePhone())
                .name("Story 6.8 race Mother")
                .role(Role.MOTHER)
                .emailVerified(false)
                .phoneVerified(false)
                .enabled(true)
                .locked(false)
                .build());
        User expertUser = userRepository.save(User.builder()
                .phone(uniquePhone())
                .name("Story 6.8 race Expert")
                .role(Role.EXPERT)
                .emailVerified(false)
                .phoneVerified(false)
                .enabled(true)
                .locked(false)
                .build());
        ExpertProfile expert = expertProfileRepository.save(ExpertProfile.builder()
                .userId(expertUser.getId())
                .specialty("Maternal health")
                .verificationStatus(VerificationStatus.APPROVED)
                .trustStatus(TrustStatus.ACTIVE)
                .build());
        MotherJourney journey = motherJourneyRepository.save(MotherJourney.builder()
                .ownerUserId(mother.getId())
                .journeyType(JourneyType.POSTPARTUM)
                .status(JourneyStatus.ACTIVE)
                .startDate(LocalDate.of(2026, 7, 1))
                .build());
        IntakeSession intake = createIntake(mother.getId(), journey.getId());
        EvidenceSource evidence = withCitation ? seedEvidenceSource() : null;
        List<Map<String, Object>> citations = evidence == null
                ? List.of()
                : List.of(Map.of("url", "https://" + evidence.getDomain() + "/guideline"));
        when(triageService.getResult(intake.getId(), mother.getId())).thenReturn(
                triageResult(intake, citations));
        return new Fixture(mother, expertUser, expert, journey, intake, evidence);
    }

    private IntakeSession seedAdditionalIntake(Fixture fixture) {
        IntakeSession intake = createIntake(fixture.mother().getId(), fixture.journey().getId());
        when(triageService.getResult(intake.getId(), fixture.mother().getId()))
                .thenReturn(triageResult(intake, List.of()));
        return intake;
    }

    private IntakeSession createIntake(UUID ownerId, UUID journeyId) {
        return intakeRepository.save(IntakeSession.builder()
                .userId(ownerId)
                .stage(TriageStage.POSTPARTUM)
                .journeyId(journeyId)
                .originDashboard(OriginDashboard.MOTHER_JOURNEY)
                .originReferenceId(journeyId)
                .continuationToken(UUID.randomUUID())
                .continuationExpiresAt(Instant.now().plusSeconds(3_600))
                .symptoms("server-owned synthetic race fixture")
                .riskLevel(RiskLevel.YELLOW)
                .status(IntakeStatus.COMPLETED)
                .createdAt(Instant.now())
                .completedAt(Instant.now())
                .createdBy(ownerId)
                .build());
    }

    private EvidenceSource seedEvidenceSource() {
        String domain = "story68-" + UUID.randomUUID() + ".example";
        Instant now = Instant.now();
        return evidenceSourceRepository.save(EvidenceSource.builder()
                .domain(domain)
                .baseUrl("https://" + domain)
                .organization("Synthetic approved authority")
                .category("OTHER")
                .status("APPROVED")
                .discoveryMode("MANUAL_ADMIN_ADD")
                .applicableStages("POSTPARTUM")
                .reviewedAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

    private static TriageResultResponse triageResult(
            IntakeSession intake, List<Map<String, Object>> citations) {
        return TriageResultResponse.builder()
                .sessionId(intake.getId())
                .stage("POSTPARTUM")
                .riskLevel("YELLOW")
                .summary("Reviewed minimum context")
                .citations(citations)
                .build();
    }

    private static TriageExpertHandoffCreateRequest handoffRequest(UUID key, UUID expertId) {
        return new TriageExpertHandoffCreateRequest(
                key, expertId, true, TriageExpertHandoffPolicy.POLICY_VERSION);
    }

    private void assertAggregateCounts(
            UUID ownerId, long requests, long consents, long contexts, long citations) {
        assertThat(count("consultation_requests", "requester_user_id", ownerId))
                .isEqualTo(requests);
        assertThat(count("consent_grants", "user_id", ownerId)).isEqualTo(consents);
        assertThat(count("consultation_context_shares", "owner_user_id", ownerId))
                .isEqualTo(contexts);
        Long citationCount = jdbcTemplate.queryForObject(
                """
                SELECT count(*)
                FROM consultation_context_citations citation
                JOIN consultation_context_shares share
                  ON share.context_share_id = citation.context_share_id
                WHERE share.owner_user_id = ?
                """,
                Long.class,
                ownerId);
        assertThat(citationCount).isEqualTo(citations);
    }

    private long notificationCount(UUID expertUserId) {
        return jdbcTemplate.queryForObject(
                """
                SELECT count(*) FROM notification_records
                WHERE user_id = ?
                  AND type = 'CONSULTATION'
                  AND reference_type = 'CONSULTATION_REQUEST'
                """,
                Long.class,
                expertUserId);
    }

    private long count(String table, String ownerColumn, UUID ownerId) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM " + table + " WHERE " + ownerColumn + " = ?",
                Long.class,
                ownerId);
    }

    private void awaitBlockedQuery(String tableName) throws InterruptedException {
        for (int attempt = 0; attempt < 100; attempt++) {
            Integer blocked = jdbcTemplate.queryForObject(
                    """
                    SELECT count(*) FROM pg_stat_activity
                    WHERE datname = current_database()
                      AND wait_event_type = 'Lock'
                      AND query ILIKE ?
                    """,
                    Integer.class,
                    "%" + tableName + "%");
            if (blocked != null && blocked > 0) {
                return;
            }
            Thread.sleep(25);
        }
        throw new AssertionError("Handoff never blocked on " + tableName);
    }

    private static void lockRow(Connection connection, String sql, UUID id) throws SQLException {
        try (var statement = connection.prepareStatement(sql)) {
            statement.setObject(1, id);
            try (var result = statement.executeQuery()) {
                assertThat(result.next()).isTrue();
            }
        }
    }

    private static void update(Connection connection, String sql, UUID id) throws SQLException {
        try (var statement = connection.prepareStatement(sql)) {
            statement.setObject(1, id);
            assertThat(statement.executeUpdate()).isOne();
        }
    }

    private static void assertHandoffFailure(
            Future<HandoffCreateResponse> future, String expectedCode) throws Exception {
        assertThatThrownBy(() -> future.get(10, TimeUnit.SECONDS))
                .isInstanceOfSatisfying(ExecutionException.class, execution -> {
                    assertThat(execution.getCause())
                            .isInstanceOfSatisfying(
                                    TriageExpertHandoffException.class,
                                    error -> assertThat(error.getCode()).isEqualTo(expectedCode));
                });
    }

    private static void assertConflict(ThrowingAction action) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(
                        TriageExpertHandoffException.class,
                        error -> assertThat(error.getCode()).isEqualTo("HANDOFF-009"));
    }

    private static String uniquePhone() {
        return "09" + String.format("%08d", Math.floorMod(System.nanoTime(), 100_000_000L));
    }

    @FunctionalInterface
    private interface ThrowingAction {
        void run() throws Exception;
    }

    private record Fixture(
            User mother,
            User expertUser,
            ExpertProfile expert,
            MotherJourney journey,
            IntakeSession intake,
            EvidenceSource evidenceSource) {}
}
