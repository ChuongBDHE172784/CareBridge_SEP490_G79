package com.carebridge.backend.baby;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.repository.AuditLogRepository;
import com.carebridge.backend.baby.dto.CreateBabyProfileRequest;
import com.carebridge.backend.baby.dto.LinkBabyJourneyRequest;
import com.carebridge.backend.baby.entity.BabyProfile;
import com.carebridge.backend.baby.entity.BabyProfileStatus;
import com.carebridge.backend.baby.repository.BabyLinkSubmissionRepository;
import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.baby.policy.BabyJourneyLinkagePolicy;
import com.carebridge.backend.baby.service.IBabyService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.journey.entity.JourneyDateSource;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.MotherJourney;
import com.carebridge.backend.journey.entity.PregnancyOutcomeEvidence;
import com.carebridge.backend.journey.entity.PregnancyOutcomeType;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import com.carebridge.backend.journey.repository.PregnancyOutcomeEvidenceRepository;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.rbac.Role;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.core.io.ClassPathResource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import javax.sql.DataSource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/** Production Spring/JPA/PostgreSQL evidence for Story 6.5. */
class BabyJourneyLinkSpringPostgresIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private IBabyService babyService;
    @Autowired private BabyProfileRepository babyRepository;
    @Autowired private BabyLinkSubmissionRepository submissionRepository;
    @Autowired private MotherJourneyRepository journeyRepository;
    @Autowired private PregnancyOutcomeEvidenceRepository evidenceRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private AuditLogRepository auditRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private DataSource dataSource;
    @MockitoSpyBean private BabyJourneyLinkagePolicy linkagePolicy;

    private UUID motherId;
    private UUID canonicalJourneyId;
    private UUID foreignMotherId;
    private UUID foreignJourneyId;

    @BeforeEach
    void setUp() {
        wipeStoryFixtures();
        motherId = seedMother("6500000001");
        canonicalJourneyId = seedEligibleJourney(motherId);
        foreignMotherId = seedMother("6500000002");
        foreignJourneyId = seedEligibleJourney(foreignMotherId);
    }

    @AfterEach
    void cleanUp() {
        wipeStoryFixtures();
    }

    @Test
    void linkedBabyReadSupportsZeroOneManyAndContainsLegacyOwnerMismatch() {
        assertThat(babyService.listJourneyBabies(canonicalJourneyId, 0, 20, motherId).getTotalElements())
                .isZero();

        seedBaby(motherId, canonicalJourneyId, "One");
        assertThat(babyService.listJourneyBabies(canonicalJourneyId, 0, 20, motherId).getTotalElements())
                .isEqualTo(1L);

        seedBaby(motherId, canonicalJourneyId, "Two");
        var page = babyService.listJourneyBabies(canonicalJourneyId, 0, 1, motherId);
        assertThat(page.getData()).hasSize(1);
        assertThat(page.getTotalElements()).isEqualTo(2L);
        assertThat(page.getTotalPages()).isEqualTo(2);

        // Synthetic legacy mismatch: FK-valid but owner-incompatible link.
        UUID containedBaby = seedBaby(motherId, foreignJourneyId, "Contained");
        assertThat(babyService.listJourneyBabies(canonicalJourneyId, 0, 20, motherId).getData())
                .extracting(item -> item.getId())
                .doesNotContain(containedBaby);
        assertThatThrownBy(() -> babyService.listJourneyBabies(foreignJourneyId, 0, 20, motherId))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getCode())
                        .isEqualTo("LINK_RESOURCE_NOT_FOUND"));
    }

    @Test
    void sanitizedLegacyAssessmentClassifiesInvalidFixturesAndApiContainsEveryRow() throws Exception {
        UUID validBaby = seedBaby(motherId, canonicalJourneyId, "Valid");
        UUID ownerMismatch = seedBaby(motherId, foreignJourneyId, "Owner mismatch");

        UUID nonCanonicalJourney = seedJourney(
                motherId, JourneyStatus.COMPLETED, PregnancyOutcomeType.LIVE_BIRTH, true);
        UUID nonCanonical = seedBaby(motherId, nonCanonicalJourney, "Non canonical");

        UUID noEvidenceMother = seedMother("6500000003");
        UUID noEvidenceJourney = seedJourney(
                noEvidenceMother, JourneyStatus.ACTIVE, PregnancyOutcomeType.LIVE_BIRTH, false);
        UUID missingEvidence = seedBaby(noEvidenceMother, noEvidenceJourney, "Missing evidence");

        UUID incompatibleMother = seedMother("6500000004");
        UUID incompatibleJourney = seedJourney(
                incompatibleMother, JourneyStatus.ACTIVE, PregnancyOutcomeType.PREGNANCY_LOSS, true);
        UUID incompatible = seedBaby(incompatibleMother, incompatibleJourney, "Incompatible outcome");

        var visible = babyService.listJourneyBabies(canonicalJourneyId, 0, 20, motherId).getData();
        assertThat(visible).extracting(item -> item.getId())
                .containsExactly(validBaby)
                .doesNotContain(ownerMismatch, nonCanonical, missingEvidence, incompatible);
        assertThat(babyService.listJourneyBabies(foreignJourneyId, 0, 20, foreignMotherId).getData())
                .extracting(item -> item.getId())
                .doesNotContain(ownerMismatch);

        assertLinkReadRejected(noEvidenceJourney, noEvidenceMother, "LINK_NOT_ELIGIBLE");
        assertLinkReadRejected(incompatibleJourney, incompatibleMother, "LINK_NOT_ELIGIBLE");
        assertLinkReadRejected(nonCanonicalJourney, motherId, "LINK_RESOURCE_NOT_FOUND");
        assertLinkReadRejected(foreignJourneyId, motherId, "LINK_RESOURCE_NOT_FOUND");

        String assessmentSql = new ClassPathResource(
                "db/assessment/story_6_5_legacy_baby_links.sql")
                .getContentAsString(StandardCharsets.UTF_8);
        Map<String, Long> sanitizedCounts = runReadOnlyAssessment(assessmentSql);

        assertThat(sanitizedCounts).containsEntry("VALID", 1L);
        assertThat(sanitizedCounts).containsEntry("OWNER_MISMATCH", 1L);
        assertThat(sanitizedCounts).containsEntry("NON_CANONICAL_OR_INACTIVE", 1L);
        assertThat(sanitizedCounts).containsEntry("MISSING_OUTCOME_EVIDENCE", 1L);
        assertThat(sanitizedCounts).containsEntry("INCOMPATIBLE_OUTCOME", 1L);
        assertThat(assessmentSql.toLowerCase()).doesNotContain("delete ", "update ", "truncate ");
    }

    @Test
    void createWithLinkReplayIsAuthoritativeAndAuditedExactlyOnce() {
        UUID submissionId = UUID.randomUUID();
        CreateBabyProfileRequest request = createRequest(submissionId, "Bean");

        var first = babyService.createBabyProfile(request, motherId);
        var replay = babyService.createBabyProfile(request, motherId);

        assertThat(replay.getId()).isEqualTo(first.getId());
        assertThat(babyRepository.countByOwnerUserId(motherId)).isEqualTo(1L);
        assertThat(submissionRepository.count()).isEqualTo(1L);
        assertThat(auditRepository.findByEntityIdAndAction(
                        first.getId(), AuditAction.BABY_JOURNEY_LINK_ACCEPTED))
                .hasSize(1);

        request.setNickname("Changed intent");
        assertThatThrownBy(() -> babyService.createBabyProfile(request, motherId))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getCode())
                        .isEqualTo("LINK_SUBMISSION_CONFLICT"));
        assertThat(babyRepository.countByOwnerUserId(motherId)).isEqualTo(1L);
        assertThat(countAudit(AuditAction.BABY_JOURNEY_LINK_REJECTED, motherId)).isEqualTo(1L);
    }

    @Test
    void conflictingExistingLinkRollsBackAndLeavesDurableSanitizedRejectionAudit() {
        UUID babyId = seedBaby(motherId, foreignJourneyId, "Legacy conflict");
        LinkBabyJourneyRequest request = new LinkBabyJourneyRequest();
        request.setRelatedJourneyId(canonicalJourneyId);
        request.setSubmissionId(UUID.randomUUID());

        assertThatThrownBy(() -> babyService.linkExistingBaby(babyId, request, motherId))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getCode())
                        .isEqualTo("BABY_ALREADY_LINKED"));

        assertThat(babyRepository.findById(babyId).orElseThrow().getRelatedJourneyId())
                .isEqualTo(foreignJourneyId);
        assertThat(submissionRepository.count()).isZero();
        var rejected = auditRepository.findByEntityIdAndAction(
                babyId, AuditAction.BABY_JOURNEY_LINK_REJECTED);
        assertThat(rejected).hasSize(1);
        assertThat(rejected.getFirst().getNewValueJson())
                .contains("BABY_ALREADY_LINKED")
                .doesNotContain("Legacy conflict", "birthDate", "token");
    }

    @Test
    void sameOwnerExistingBabyLinkAndReplayProduceOneAcceptedAudit() {
        UUID babyId = seedBaby(motherId, null, "Existing");
        LinkBabyJourneyRequest request = new LinkBabyJourneyRequest();
        request.setRelatedJourneyId(canonicalJourneyId);
        request.setSubmissionId(UUID.randomUUID());

        var first = babyService.linkExistingBaby(babyId, request, motherId);
        var replay = babyService.linkExistingBaby(babyId, request, motherId);

        assertThat(first.getRelatedJourneyId()).isEqualTo(canonicalJourneyId);
        assertThat(replay.getBabyId()).isEqualTo(babyId);
        assertThat(babyRepository.findById(babyId).orElseThrow().getRelatedJourneyId())
                .isEqualTo(canonicalJourneyId);
        assertThat(submissionRepository.count()).isEqualTo(1L);
        assertThat(auditRepository.findByEntityIdAndAction(
                        babyId, AuditAction.BABY_JOURNEY_LINK_ACCEPTED))
                .hasSize(1);
    }

    @Test
    void concurrentSameSubmissionCreatesOneBabyOneSubmissionAndOneAcceptedAudit() throws Exception {
        UUID submissionId = UUID.randomUUID();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var futures = new ArrayList<java.util.concurrent.Future<UUID>>();
            for (int i = 0; i < 2; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await(10, TimeUnit.SECONDS);
                    return babyService.createBabyProfile(
                            createRequest(submissionId, "Concurrent"), motherId).getId();
                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            List<UUID> results = new ArrayList<>();
            for (var future : futures) results.add(future.get(30, TimeUnit.SECONDS));
            assertThat(results).hasSize(2).allMatch(results.getFirst()::equals);
        }

        assertThat(babyRepository.countByOwnerUserId(motherId)).isEqualTo(1L);
        assertThat(submissionRepository.count()).isEqualTo(1L);
        UUID babyId = babyRepository.findByOwnerUserIdAndStatusOrderByCreatedAtAsc(
                motherId, BabyProfileStatus.ACTIVE).getFirst().getId();
        assertThat(auditRepository.findByEntityIdAndAction(
                        babyId, AuditAction.BABY_JOURNEY_LINK_ACCEPTED))
                .hasSize(1);
    }

    @Test
    void concurrentDifferentJourneysForOneExistingBabyYieldOneSuccessAndOneTypedConflict() throws Exception {
        UUID secondJourneyId = journeyRepository.saveAndFlush(MotherJourney.builder()
                .ownerUserId(motherId)
                .journeyType(JourneyType.POSTPARTUM)
                .status(JourneyStatus.COMPLETED)
                .startDate(LocalDate.of(2025, 12, 1))
                .deliveryDate(LocalDate.of(2025, 12, 1))
                .pregnancyOutcome(PregnancyOutcomeType.LIVE_BIRTH)
                .pregnancyOutcomeDate(LocalDate.of(2025, 12, 1))
                .build()).getId();
        UUID babyId = seedBaby(motherId, null, "Concurrent existing");

        // The product allows only one canonical journey. Stub only the eligibility gate so this
        // test can deterministically exercise the production service/repository row-lock race.
        doAnswer(invocation -> journeyRepository.findById(invocation.getArgument(0, UUID.class)).orElseThrow())
                .when(linkagePolicy).requireEligibleJourney(any(UUID.class), eq(motherId));

        LinkBabyJourneyRequest firstRequest = linkRequest(canonicalJourneyId, UUID.randomUUID());
        LinkBabyJourneyRequest secondRequest = linkRequest(secondJourneyId, UUID.randomUUID());
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Object> outcomes = new ArrayList<>();
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> concurrentLink(babyId, firstRequest, ready, start));
            var second = executor.submit(() -> concurrentLink(babyId, secondRequest, ready, start));
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            outcomes.add(first.get(30, TimeUnit.SECONDS));
            outcomes.add(second.get(30, TimeUnit.SECONDS));
        }

        assertThat(outcomes.stream().filter(UUID.class::isInstance).map(UUID.class::cast).toList())
                .hasSize(1)
                .allMatch(babyId::equals);
        assertThat(outcomes.stream().filter(String.class::isInstance).map(String.class::cast).toList())
                .containsExactly("BABY_ALREADY_LINKED");

        UUID authoritativeJourney = babyRepository.findById(babyId).orElseThrow().getRelatedJourneyId();
        assertThat(authoritativeJourney).isIn(canonicalJourneyId, secondJourneyId);
        assertThat(auditRepository.findByEntityIdAndAction(
                        babyId, AuditAction.BABY_JOURNEY_LINK_ACCEPTED))
                .hasSize(1);
        assertThat(auditRepository.findByEntityIdAndAction(
                        babyId, AuditAction.BABY_JOURNEY_LINK_REJECTED))
                .hasSize(1);
    }

    @Test
    void databaseForeignKeyRejectsMissingJourneyWithoutPersistingBaby() {
        BabyProfile invalid = BabyProfile.builder()
                .ownerUserId(motherId)
                .relatedJourneyId(UUID.randomUUID())
                .nickname("Invalid FK")
                .birthDate(LocalDate.of(2026, 1, 1))
                .build();

        assertThatThrownBy(() -> babyRepository.saveAndFlush(invalid))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from baby_profiles where owner_user_id=? and nickname='Invalid FK'",
                Long.class,
                motherId)).isZero();
    }

    private UUID seedMother(String phone) {
        return userRepository.saveAndFlush(User.builder()
                .phone(phone)
                .email(phone + "@story65.test")
                .name("Story 65 Mother")
                .accountStatus("ACTIVE")
                .emailVerified(true)
                .phoneVerified(true)
                .role(Role.MOTHER)
                .enabled(true)
                .locked(false)
                .mustChangePassword(false)
                .build()).getId();
    }

    private UUID seedEligibleJourney(UUID ownerId) {
        return seedJourney(ownerId, JourneyStatus.ACTIVE, PregnancyOutcomeType.LIVE_BIRTH, true);
    }

    private UUID seedJourney(
            UUID ownerId,
            JourneyStatus status,
            PregnancyOutcomeType outcome,
            boolean withEvidence) {
        MotherJourney journey = journeyRepository.saveAndFlush(MotherJourney.builder()
                .ownerUserId(ownerId)
                .journeyType(JourneyType.POSTPARTUM)
                .status(status)
                .startDate(LocalDate.of(2026, 1, 1))
                .deliveryDate(LocalDate.of(2026, 1, 1))
                .pregnancyOutcome(outcome)
                .pregnancyOutcomeDate(LocalDate.of(2026, 1, 1))
                .build());
        if (withEvidence) {
            evidenceRepository.saveAndFlush(PregnancyOutcomeEvidence.builder()
                    .journeyId(journey.getId())
                    .ownerUserId(ownerId)
                    .submissionId(UUID.randomUUID())
                    .outcomeType(outcome)
                    .outcomeDate(LocalDate.of(2026, 1, 1))
                    .source(JourneyDateSource.SELF_REPORTED)
                    .actorUserId(ownerId)
                    .reason("Synthetic Story 6.5 integration fixture")
                    .effectiveAt(Instant.parse("2026-01-01T00:00:00Z"))
                    .revisionNumber(1)
                    .journeyVersion(journey.getVersion())
                    .semanticHash(UUID.randomUUID().toString())
                    .correction(false)
                    .build());
        }
        return journey.getId();
    }

    private UUID seedBaby(UUID ownerId, UUID journeyId, String nickname) {
        return babyRepository.saveAndFlush(BabyProfile.builder()
                .ownerUserId(ownerId)
                .relatedJourneyId(journeyId)
                .nickname(nickname)
                .birthDate(LocalDate.of(2026, 1, 1))
                .status(BabyProfileStatus.ACTIVE)
                .active(false)
                .build()).getId();
    }

    private CreateBabyProfileRequest createRequest(UUID submissionId, String nickname) {
        CreateBabyProfileRequest request = new CreateBabyProfileRequest();
        request.setNickname(nickname);
        request.setBirthDate(LocalDate.of(2026, 1, 1));
        request.setRelatedJourneyId(canonicalJourneyId);
        request.setSubmissionId(submissionId);
        return request;
    }

    private LinkBabyJourneyRequest linkRequest(UUID journeyId, UUID submissionId) {
        LinkBabyJourneyRequest request = new LinkBabyJourneyRequest();
        request.setRelatedJourneyId(journeyId);
        request.setSubmissionId(submissionId);
        return request;
    }

    private Object concurrentLink(
            UUID babyId,
            LinkBabyJourneyRequest request,
            CountDownLatch ready,
            CountDownLatch start) throws Exception {
        ready.countDown();
        start.await(10, TimeUnit.SECONDS);
        try {
            return babyService.linkExistingBaby(babyId, request, motherId).getBabyId();
        } catch (BusinessException exception) {
            return exception.getCode();
        }
    }

    private long countAudit(AuditAction action, UUID actor) {
        return auditRepository.findAll().stream()
                .filter(log -> actor.equals(log.getActorUserId()))
                .filter(log -> action == log.getAction())
                .count();
    }

    private void assertLinkReadRejected(UUID journeyId, UUID ownerId, String expectedCode) {
        assertThatThrownBy(() -> babyService.listJourneyBabies(journeyId, 0, 20, ownerId))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getCode())
                        .isEqualTo(expectedCode));
    }

    private Map<String, Long> runReadOnlyAssessment(String assessmentSql) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            connection.setReadOnly(true);
            try (var readOnly = connection.createStatement()) {
                readOnly.execute("set transaction read only");
                try (var mode = readOnly.executeQuery("show transaction_read_only")) {
                    assertThat(mode.next()).isTrue();
                    assertThat(mode.getString(1)).isEqualTo("on");
                }
                Map<String, Long> counts;
                try (var result = readOnly.executeQuery(assessmentSql)) {
                    var rows = new ArrayList<Map.Entry<String, Long>>();
                    while (result.next()) {
                        rows.add(Map.entry(result.getString("reason"), result.getLong("row_count")));
                    }
                    counts = rows.stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                }
                connection.rollback();
                return counts;
            } catch (Exception failure) {
                connection.rollback();
                throw failure;
            }
        }
    }

    private void wipeStoryFixtures() {
        jdbcTemplate.execute("truncate table baby_link_submissions, pregnancy_outcome_evidence, baby_profiles, mother_journeys, audit_logs, users cascade");
    }
}
