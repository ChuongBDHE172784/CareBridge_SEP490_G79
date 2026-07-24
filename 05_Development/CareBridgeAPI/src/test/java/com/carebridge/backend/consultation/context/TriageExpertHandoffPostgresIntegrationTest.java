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
import com.carebridge.backend.security.rbac.Role;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
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
        User mother = userRepository.save(User.builder()
                .phone(uniquePhone())
                .name("Story 6.8 Mother")
                .role(Role.MOTHER)
                .emailVerified(false)
                .phoneVerified(false)
                .enabled(true)
                .locked(false)
                .build());
        User expertUser = userRepository.save(User.builder()
                .phone(uniquePhone())
                .name("Story 6.8 Expert")
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
        assertThat(count("consultation_requests", "requester_user_id", mother.getId())).isOne();
        assertThat(count("consent_grants", "user_id", mother.getId())).isOne();
        assertThat(count("consultation_context_shares", "owner_user_id", mother.getId())).isOne();
        assertThat(countCitationsForRequest(created.consultationRequestId())).isZero();
        entityManager.flush();
        List<String> featureAudits = jdbcTemplate.queryForList(
                """
                SELECT new_value_json::text
                FROM audit_logs
                WHERE actor_user_id = ? AND entity_type = 'TRIAGE_EXPERT_HANDOFF'
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
