package com.carebridge.backend.consultation.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.carebridge.backend.consultation.context.dto.HandoffCreateResponse;
import com.carebridge.backend.consultation.context.dto.HandoffParticipantResponse;
import com.carebridge.backend.consultation.context.dto.TriageExpertHandoffCreateRequest;
import com.carebridge.backend.consultation.context.policy.TriageExpertHandoffPolicy;
import com.carebridge.backend.consultation.context.service.ITriageExpertHandoffService;
import com.carebridge.backend.expert.entity.ExpertProfile;
import com.carebridge.backend.expert.repository.ExpertProfileRepository;
import com.carebridge.backend.expert.truststatus.TrustStatus;
import com.carebridge.backend.expert.verificationstatus.VerificationStatus;
import com.carebridge.backend.integration.zegocloud.IZegoCloudService;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.MotherJourney;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import com.carebridge.backend.testsupport.CanonicalUserFixture;
import com.carebridge.backend.triage.IntakeStatus;
import com.carebridge.backend.triage.OriginDashboard;
import com.carebridge.backend.triage.RiskLevel;
import com.carebridge.backend.triage.TriageStage;
import com.carebridge.backend.triage.dto.response.TriageResultResponse;
import com.carebridge.backend.triage.entity.IntakeSession;
import com.carebridge.backend.triage.repository.IIntakeSessionRepository;
import com.carebridge.backend.triage.service.ITriageService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

class TriageExpertHandoffPostgresIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private ITriageExpertHandoffService handoffService;
    @Autowired private UserRepository userRepository;
    @Autowired private ExpertProfileRepository expertProfileRepository;
    @Autowired private MotherJourneyRepository motherJourneyRepository;
    @Autowired private IIntakeSessionRepository intakeRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @PersistenceContext private EntityManager entityManager;

    @MockitoBean private ITriageService triageService;
    @MockitoBean private IZegoCloudService zegoCloudService;

    @Test
    @Transactional
    void createReplayAndParticipantReadKeepOneCommittedAggregatePerKey() {
        User mother = seedUser("Story 6.8 Mother", "MOTHER");
        User expertUser = seedUser("Story 6.8 Expert", "EXPERT");
        // Canonical model: the expert profile IS the users row (profile id == user id), so
        // enrich the already-seeded users row instead of inserting a second entity.
        jdbcTemplate.update(
                "UPDATE users SET specialty = ?, verification_status = ?, trust_status = ? "
                        + "WHERE user_id = ?",
                "Maternal health",
                VerificationStatus.APPROVED.name(),
                TrustStatus.ACTIVE.name(),
                expertUser.getId());
        ExpertProfile expert = expertProfileRepository
                .findByUserId(expertUser.getId())
                .orElseThrow();
        MotherJourney journey = seedJourney(mother.getId());
        IntakeSession intake = intakeRepository.save(IntakeSession.builder()
                .userId(mother.getId())
                .stage(TriageStage.POSTPARTUM)
                .journeyId(journey.getId())
                .originDashboard(OriginDashboard.MOTHER_JOURNEY)
                .originReferenceId(journey.getId())
                .continuationToken(UUID.randomUUID())
                .continuationExpiresAt(Instant.now().plusSeconds(3600))
                .symptoms("server-owned synthetic fixture")
                .riskLevel(RiskLevel.YELLOW)
                .status(IntakeStatus.COMPLETED)
                .createdAt(Instant.now())
                .completedAt(Instant.now())
                .createdBy(mother.getId())
                .build());
        UUID key = UUID.randomUUID();
        when(triageService.getResult(intake.getId(), mother.getId())).thenReturn(
                TriageResultResponse.builder()
                        .sessionId(intake.getId())
                        .stage("POSTPARTUM")
                        .riskLevel("YELLOW")
                        .summary("  Reviewed\tminimum context  ")
                        .citations(List.of())
                        .build());
        TriageExpertHandoffCreateRequest request = new TriageExpertHandoffCreateRequest(
                key,
                expert.getExpertProfileId(),
                true,
                TriageExpertHandoffPolicy.POLICY_VERSION);

        HandoffCreateResponse created = handoffService.create(intake.getId(), request, mother.getId());
        HandoffCreateResponse replayed = handoffService.create(intake.getId(), request, mother.getId());
        HandoffParticipantResponse expertView =
                handoffService.read(created.consultationRequestId(), expertUser.getId());

        assertThat(created.replayed()).isFalse();
        assertThat(replayed.replayed()).isTrue();
        assertThat(replayed.consultationRequestId()).isEqualTo(created.consultationRequestId());
        assertThat(replayed.sharedAt()).isEqualTo(created.sharedAt());
        assertThat(expertView.context().riskSummary()).isEqualTo("Reviewed minimum context");
        assertThat(count("expert_consultation_requests", "requester_user_id", mother.getId())).isOne();
        assertThat(count("data_permissions", "owner_user_id", mother.getId())).isOne();
        assertThat(count("consultation_context_shares", "owner_user_id", mother.getId())).isOne();
        assertThat(countCitationsForRequest(created.consultationRequestId())).isZero();
        entityManager.flush();
        List<String> featureAudits = jdbcTemplate.queryForList(
                """
                SELECT after_payload_jsonb::text
                FROM audit_events
                WHERE actor_user_id = ?
                  AND resource_type = 'TRIAGE_EXPERT_HANDOFF'
                  AND event_origin = 'AUDIT_LOG'
                """,
                String.class,
                mother.getId());
        assertThat(featureAudits).singleElement()
                .satisfies(details -> {
                    assertThat(details).contains(
                            "TRIAGE_CONTEXT_SHARED", "YELLOW_EXPERT_CONTEXT_V1");
                    assertThat(details).doesNotContain(
                            "Reviewed minimum context", "riskSummary", "citations", "symptoms");
                });
    }

    private User seedUser(String name, String role) {
        UUID userId = UUID.randomUUID();
        CanonicalUserFixture.insertUser(jdbcTemplate, userId, name, uniquePhone(), role);
        return userRepository.findById(userId).orElseThrow();
    }

    private MotherJourney seedJourney(UUID ownerId) {
        UUID careSubjectId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO care_subjects (
                    care_subject_id, person_id, owner_user_id, subject_type,
                    nickname, status, created_at, updated_at)
                SELECT ?, u.person_id, u.user_id, 'MOTHER', u.display_name,
                       'ACTIVE', now(), now()
                  FROM users u
                 WHERE u.user_id = ?
                """, careSubjectId, ownerId);
        MotherJourney journey = motherJourneyRepository.saveAndFlush(MotherJourney.builder()
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

    private long count(String table, String ownerColumn, UUID ownerId) {
        if (ownerId == null) {
            return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
        }
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE " + ownerColumn + " = ?",
                Long.class,
                ownerId);
    }

    private long countCitationsForRequest(UUID consultationRequestId) {
        return jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM consultation_context_citations citation
                JOIN consultation_context_shares context_share
                  ON context_share.context_share_id = citation.context_share_id
                WHERE context_share.consultation_request_id = ?
                """,
                Long.class,
                consultationRequestId);
    }

    private static String uniquePhone() {
        return "09" + String.format("%08d", Math.floorMod(System.nanoTime(), 100_000_000L));
    }
}
