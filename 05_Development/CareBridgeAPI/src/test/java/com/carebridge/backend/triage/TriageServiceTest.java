package com.carebridge.backend.triage;

import com.carebridge.backend.triage.dto.response.TriageResultResponse;
import com.carebridge.backend.triage.entity.IntakeSession;
import com.carebridge.backend.triage.repository.IIntakeSessionRepository;
import com.carebridge.backend.triage.service.EvidenceSourceService;
import com.carebridge.backend.triage.service.impl.TriageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TriageServiceTest {
    private static final UUID USER = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private IIntakeSessionRepository repository;
    private TriageService service;

    @BeforeEach
    void setUp() {
        repository = mock(IIntakeSessionRepository.class);
        EvidenceSourceService evidence = mock(EvidenceSourceService.class);
        when(evidence.isApprovedDeepLink(any())).thenReturn(true);
        service = new TriageService(repository, evidence, new ObjectMapper());
    }

    @Test
    void projectsCanonicalSessionForHistoryAndResultWithoutRewritingIt() {
        IntakeSession session = canonical("PREGNANCY", IntakeStatus.COMPLETED, RiskLevel.YELLOW);
        String before = session.getResultJson();
        when(repository.findByIdAndUserId(session.getId(), USER)).thenReturn(Optional.of(session));
        when(repository.findByUserIdOrderByCreatedAtDesc(USER)).thenReturn(List.of(session));

        TriageResultResponse result = service.getResult(session.getId(), USER);

        assertThat(result.getStage()).isEqualTo("PREGNANCY");
        assertThat(result.getRiskLevel()).isEqualTo("YELLOW");
        assertThat(result.getSummary()).isEqualTo("Can danh gia som");
        assertThat(result.getRecommendationCode()).isEqualTo("CONTACT_HEALTHCARE_PROVIDER");
        assertThat(service.listSessions(USER)).singleElement()
                .extracting("sessionId").isEqualTo(session.getId());
        assertThat(session.getResultJson()).isEqualTo(before);
    }

    @Test
    void canonicalUnmappedStageNeverDefaultsToInfant() {
        IntakeSession session = canonical("POSSIBLE_PREGNANCY", IntakeStatus.NEED_MORE_INFO, null);
        session.setStage(null);
        when(repository.findByIdAndUserId(session.getId(), USER)).thenReturn(Optional.of(session));

        assertThat(service.getResult(session.getId(), USER).getStage())
                .isEqualTo("POSSIBLE_PREGNANCY");
    }

    @Test
    void legacySessionRemainsReadable() {
        IntakeSession session = base(IntakeStatus.COMPLETED, RiskLevel.RED);
        session.setSchemaVersion("1.0");
        session.setStage(null);
        session.setRawAiResponse("""
                {"riskLevel":"RED","summary":"Legacy red","recommendedAction":"Go now",
                 "emergencyActionRequired":true,"disclaimer":"Legacy disclaimer"}
                """);
        when(repository.findByIdAndUserId(session.getId(), USER)).thenReturn(Optional.of(session));

        TriageResultResponse result = service.getResult(session.getId(), USER);

        assertThat(result.getStage()).isEqualTo("INFANT");
        assertThat(result.getRiskLevel()).isEqualTo("RED");
        assertThat(result.getSummary()).isEqualTo("Legacy red");
    }

    @Test
    void continuationSecretIsExposedOnlyWhileTerminalAndActive() {
        IntakeSession session = canonical("POSTPARTUM_MOTHER", IntakeStatus.COMPLETED, RiskLevel.YELLOW);
        session.setContinuationToken(UUID.randomUUID());
        session.setContinuationExpiresAt(Instant.now().plusSeconds(60));
        when(repository.findByIdAndUserId(session.getId(), USER)).thenReturn(Optional.of(session));
        assertThat(service.getResult(session.getId(), USER).getContinuationToken()).isNotNull();

        session.setContinuationAcknowledgedAt(Instant.now());
        assertThat(service.getResult(session.getId(), USER).getContinuationToken()).isNull();
    }

    @Test
    void invalidCanonicalCitationIsDroppedWithoutChangingDisposition() {
        IntakeSession session = canonical("PREGNANCY", IntakeStatus.COMPLETED, RiskLevel.YELLOW);
        session.setResultJson(session.getResultJson().replace(
                "\"citations\":[]",
                "\"citations\":[{\"title\":\"Bad\",\"url\":\"http://example.com\"}]"));
        when(repository.findByIdAndUserId(session.getId(), USER)).thenReturn(Optional.of(session));

        TriageResultResponse result = service.getResult(session.getId(), USER);

        assertThat(result.getRiskLevel()).isEqualTo("YELLOW");
        assertThat(result.getCitations()).isEmpty();
        assertThat(result.getWarning()).isNotBlank();
    }

    @Test
    void nonLegacyCitationCannotBeUpgradedByTheReadModel() {
        IntakeSession session = base(IntakeStatus.COMPLETED, RiskLevel.YELLOW);
        session.setSchemaVersion("2.0");
        session.setRawAiResponse("""
                {"responseSchemaVersion":"2.0","riskLevel":"YELLOW",
                 "citations":[{"title":"Incomplete","organization":"WHO",
                 "url":"https://www.who.int/example","excerpt":"Evidence"}]}
                """);
        when(repository.findByIdAndUserId(session.getId(), USER)).thenReturn(Optional.of(session));

        TriageResultResponse result = service.getResult(session.getId(), USER);

        assertThat(result.getCitations()).isEmpty();
        assertThat(result.getWarning()).isNotBlank();
    }

    private IntakeSession canonical(String stage, IntakeStatus status, RiskLevel risk) {
        IntakeSession session = base(status, risk);
        session.setSchemaVersion("triage-v2-1");
        session.setResultJson("""
                {"contract":"triage-v2-1","v2State":{"finalResponse":"Can danh gia som",
                 "rulesetVersion":"2.2.0","decisiveRuleIds":["RULE_1"]},
                 "publicResponse":{"stage":"%s","outcome":"%s","action":"EARLY_CLINICAL_ASSESSMENT",
                 "questions":[],"citations":[],"readiness":{"technicalStatus":"READY"}}}
                """.formatted(stage, risk == null ? "NEEDS_MORE_INFO" : risk.name()));
        return session;
    }

    private IntakeSession base(IntakeStatus status, RiskLevel risk) {
        return IntakeSession.builder()
                .id(UUID.randomUUID()).userId(USER).stage(TriageStage.PREGNANCY)
                .symptoms("TRIAGE_REDACTED").status(status).riskLevel(risk)
                .emergency(risk == RiskLevel.RED).disclaimer("Not a diagnosis")
                .createdAt(Instant.now()).createdBy(USER).resultJson("{}")
                .schemaVersion("1.0").build();
    }
}
