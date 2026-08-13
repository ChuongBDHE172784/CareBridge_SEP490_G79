package com.carebridge.backend.triage;

import com.carebridge.backend.baby.entity.BabyProfile;
import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.emergency.service.IEmergencyService;
import com.carebridge.backend.journey.service.ILifecycleSafetyOutcomeProjector;
import com.carebridge.backend.security.service.EmailService;
import com.carebridge.backend.security.service.SmsService;
import com.carebridge.backend.triage.dto.response.IntakeSessionResponse;
import com.carebridge.backend.triage.entity.IntakeSession;
import com.carebridge.backend.triage.repository.IIntakeSessionRepository;
import com.carebridge.backend.triage.repository.RedFlagRuleRepository;
import com.carebridge.backend.triage.service.ChildTriageAiClient;
import com.carebridge.backend.triage.service.GeminiTriageClient;
import com.carebridge.backend.triage.service.ITriageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.carebridge.backend.triage.TriagePreScreenTestFactory.MOTHER_ID;
import static com.carebridge.backend.triage.TriagePreScreenTestFactory.makeOneShotRequest;
import static com.carebridge.backend.triage.TriagePreScreenTestFactory.makeRule;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TRFP-TC-INT-001 — end-to-end over real beans + H2 (project convention; no Testcontainers
 * harness exists in this codebase — verified UC-110 finding): a DB-seeded admin RED rule
 * short-circuits a real one-shot intake with accent-less matching text, independent of the AI.
 *
 * The AI client stub THROWS if invoked — stricter than "not asserted" (proves independence).
 * The emergency listener effect is observed via the mocked IEmergencyService boundary
 * (EmergencyEscalationHandler is a synchronous @EventListener calling openOrReuseFromTriage).
 */
@SpringBootTest
@Transactional
class TriageRedFlagPreScreenIntegrationTest {

    @Autowired private ITriageService triageService;
    @Autowired private IIntakeSessionRepository intakeSessionRepository;
    @Autowired private RedFlagRuleRepository redFlagRuleRepository;

    @MockitoBean private ChildTriageAiClient childTriageAiClient;
    @MockitoBean private IEmergencyService emergencyService;
    // H2 test-schema artifact (not a production concern): IntakeSession and StructuredIntakeData
    // both map triage_sessions, so ddl-auto=create-drop merges their columns and makes
    // StructuredIntakeData's NOT NULL columns (emergency_flag, extracted_at, ...) apply to the
    // plain IntakeSession insert — in real Postgres those columns carry defaults from the
    // canonical baseline. The synchronous IntakeSessionCompleted projection listener queries
    // triage_sessions in-transaction and would force that incomplete pending insert to flush;
    // mocking its projector keeps this test on its actual subject (pre-screen short-circuit).
    @MockitoBean private ILifecycleSafetyOutcomeProjector lifecycleSafetyOutcomeProjector;
    // Same full-context stubbing pattern as TriageIntegrationTest / RedFlagRuleIntegrationTest.
    @MockitoBean private GeminiTriageClient geminiTriageClient;
    @MockitoBean private EmailService emailService;
    @MockitoBean private SmsService smsService;
    // CB-TRIAGE-CONSENT-IMP-001: consent fixture for the elective-entry gate (Test-Spec §6 —
    // pre-existing suites updated where the gate now applies). The mocked gate's
    // ensureActiveConsent is a no-op, i.e. "consent granted"; this test's subject is unchanged.
    @MockitoBean private com.carebridge.backend.triage.service.ITriageConsentService triageConsentService;
    @MockitoBean private BabyProfileRepository babyProfileRepository;

    @Test
    void seededAdminRedRule_shortCircuitsOneShotIntake_whileInactiveRuleDoesNot() {
        when(babyProfileRepository.findByIdAndOwnerUserId(any(), any()))
                .thenReturn(Optional.of(new BabyProfile()));
        // Seed FX-001 (active RED/ESCALATE) and FX-002 (inactive) through the real repository.
        // saveAndFlush: with the red_flag_rules query space clean, Hibernate's auto-flush does
        // not try to flush the in-flight triage_sessions insert during the pre-screen SELECT
        // (see the H2 merged-schema note on the mocked projector above).
        redFlagRuleRepository.saveAndFlush(makeRule(rule -> rule.setId(null)));
        redFlagRuleRepository.saveAndFlush(makeRule(rule -> {
            rule.setId(null);
            rule.setKeyword("sặc sữa liên tục");
            rule.setActive(false);
        }));
        when(childTriageAiClient.triageChild(any()))
                .thenThrow(new IllegalStateException("AI stub must not be reached for a pre-screen RED"));

        // 1) Accent-less matching text → short-circuit, AI never touched.
        IntakeSessionResponse response = triageService.runIntake(makeOneShotRequest(), MOTHER_ID);

        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        assertThat(response.getRiskLevel()).isEqualTo("RED");
        IntakeSession session = intakeSessionRepository.findById(response.getSessionId()).orElseThrow();
        assertThat(session.getStatus()).isEqualTo(IntakeStatus.COMPLETED);
        assertThat(session.getRiskLevel()).isEqualTo(RiskLevel.RED);
        assertThat(session.isEmergency()).isTrue();
        assertThat(session.getRawAiResponse()).contains("RED_FLAG_RULE_PRESCREEN");
        verify(childTriageAiClient, never()).triageChild(any());
        // Escalation listener effect (EmergencyEscalationTriggered → handler → emergency service).
        verify(emergencyService, times(1)).openOrReuseFromTriage(session.getId(), MOTHER_ID);

        // 2) Inactive-rule keyword → NO short-circuit through the real SQL filter: the throwing
        //    AI stub is reached and the existing hardcoded Java fallback handles the request.
        IntakeSessionResponse inactiveResponse = triageService.runIntake(
                makeOneShotRequest(r -> r.setSymptoms("bé bị sặc sữa liên tục")), MOTHER_ID);

        verify(childTriageAiClient, times(1)).triageChild(any());
        IntakeSession inactiveSession =
                intakeSessionRepository.findById(inactiveResponse.getSessionId()).orElseThrow();
        assertThat(String.valueOf(inactiveSession.getRawAiResponse()))
                .doesNotContain("RED_FLAG_RULE_PRESCREEN");
    }
}
