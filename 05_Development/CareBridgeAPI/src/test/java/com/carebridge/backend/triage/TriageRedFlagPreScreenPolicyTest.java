package com.carebridge.backend.triage;

import com.carebridge.backend.triage.entity.RedFlagAction;
import com.carebridge.backend.triage.entity.RedFlagSeverity;
import com.carebridge.backend.triage.policy.PreScreenOutcome;
import com.carebridge.backend.triage.policy.PreScreenResult;
import com.carebridge.backend.triage.policy.TriageRedFlagPreScreenPolicy;
import com.carebridge.backend.triage.repository.RedFlagRuleRepository;
import com.carebridge.backend.triage.service.TriagePreScreenMetrics;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import static com.carebridge.backend.triage.TriagePreScreenTestFactory.makeRedEscalateRule;
import static com.carebridge.backend.triage.TriagePreScreenTestFactory.makeRule;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * CB-TRIAGE-TEST-003 — policy-layer unit tests (TRFP-TC-001..010, TRFP-TC-019).
 * Oracle: TDS CB-TRIAGE-IMP-003 ADR-002/003/004/005.
 */
@ExtendWith(MockitoExtension.class)
class TriageRedFlagPreScreenPolicyTest {

    @Mock private RedFlagRuleRepository redFlagRuleRepository;
    @Mock private TriagePreScreenMetrics metrics;

    private TriageRedFlagPreScreenPolicy policy() {
        return new TriageRedFlagPreScreenPolicy(redFlagRuleRepository, metrics);
    }

    @Test
    void screen_activeRedEscalateRuleMatch_returnsEscalateRed() {
        // TRFP-TC-001 (CRITICAL) — ADR-002 row 1
        when(redFlagRuleRepository.findByActiveTrue()).thenReturn(List.of(makeRedEscalateRule()));

        PreScreenResult result = policy().screen("bé bị ngã đập đầu xuống sàn");

        assertThat(result.outcome()).isEqualTo(PreScreenOutcome.ESCALATE_RED);
        assertThat(result.matchedKeywords()).contains("ngã đập đầu");
        assertThat(result.matchedRuleIds()).contains(makeRedEscalateRule().getId());
        assertThat(result.degraded()).isFalse();
        verify(redFlagRuleRepository, times(1)).findByActiveTrue();
    }

    @Test
    void screen_accentedKeywordMatchesUnaccentedInput_returnsEscalateRed() {
        // TRFP-TC-002 — ADR-005 / BR-SAFETY-TRFP-004, diacritics direction 1
        when(redFlagRuleRepository.findByActiveTrue()).thenReturn(List.of(makeRedEscalateRule()));

        PreScreenResult result = policy().screen("bé bị nga dap dau xuống sàn"); // FX-008

        assertThat(result.outcome()).isEqualTo(PreScreenOutcome.ESCALATE_RED);
    }

    @Test
    void screen_uppercaseAccentedInputMatchesUnaccentedKeyword_returnsEscalateRed() {
        // TRFP-TC-003 — ADR-005, case-insensitivity + diacritics direction 2
        when(redFlagRuleRepository.findByActiveTrue()).thenReturn(
                List.of(makeRule(r -> r.setKeyword("nga dap dau"))));

        PreScreenResult result = policy().screen("BÉ BỊ NGÃ ĐẬP ĐẦU"); // FX-009

        assertThat(result.outcome()).isEqualTo(PreScreenOutcome.ESCALATE_RED);
    }

    @Test
    void screen_inactiveRuleNeverMatches_returnsNoMatch() {
        // TRFP-TC-004 — ADR-002 inactive row; the active-only derived query excludes FX-002
        when(redFlagRuleRepository.findByActiveTrue()).thenReturn(List.of());

        PreScreenResult result = policy().screen("bé bị sặc sữa liên tục");

        assertThat(result.outcome()).isEqualTo(PreScreenOutcome.NO_MATCH);
        assertThat(result.matchedRuleIds()).isEmpty();
        assertThat(result.degraded()).isFalse();
    }

    @Test
    void screen_yellowMatch_returnsAnnotateOnly() {
        // TRFP-TC-005 — ADR-002 / BR-SAFETY-TRFP-005
        when(redFlagRuleRepository.findByActiveTrue()).thenReturn(List.of(makeRule(r -> {
            r.setKeyword("sốt kéo dài");
            r.setSeverity(RedFlagSeverity.YELLOW);
            r.setAction(RedFlagAction.WARN);
        }))); // FX-003

        PreScreenResult result = policy().screen("bé sốt kéo dài ba ngày");

        assertThat(result.outcome()).isEqualTo(PreScreenOutcome.ANNOTATE_ONLY);
        assertThat(result.matchedKeywords()).contains("sốt kéo dài");
    }

    @Test
    void screen_redWarnMatch_returnsAnnotateOnly() {
        // TRFP-TC-006 — ADR-002 action split (WARN vs ESCALATE)
        when(redFlagRuleRepository.findByActiveTrue()).thenReturn(List.of(makeRule(r -> {
            r.setKeyword("phát ban toàn thân");
            r.setAction(RedFlagAction.WARN);
        }))); // FX-004, severity stays RED

        PreScreenResult result = policy().screen("bé bị phát ban toàn thân");

        assertThat(result.outcome()).isEqualTo(PreScreenOutcome.ANNOTATE_ONLY);
    }

    @Test
    void screen_redBlockMatch_returnsEscalateRedNeverRejection() {
        // TRFP-TC-007 — ADR-002: BLOCK ≡ ESCALATE on intake, never rejection (BR-SAFETY)
        when(redFlagRuleRepository.findByActiveTrue()).thenReturn(List.of(makeRule(r -> {
            r.setKeyword("uống nhầm hóa chất");
            r.setAction(RedFlagAction.BLOCK);
        }))); // FX-005

        PreScreenResult result = assertDoesNotThrow(
                () -> policy().screen("bé uống nhầm hóa chất tẩy rửa"));

        assertThat(result.outcome()).isEqualTo(PreScreenOutcome.ESCALATE_RED);
    }

    @Test
    void screen_greenMatch_isInertNoMatch() {
        // TRFP-TC-008 — ADR-002: GREEN stays inert (consistent with UC-110 ADR-003)
        when(redFlagRuleRepository.findByActiveTrue()).thenReturn(List.of(makeRule(r -> {
            r.setKeyword("hắt hơi");
            r.setSeverity(RedFlagSeverity.GREEN);
            r.setAction(RedFlagAction.WARN);
        }))); // FX-006

        PreScreenResult result = policy().screen("bé hắt hơi vài lần");

        assertThat(result.outcome()).isEqualTo(PreScreenOutcome.NO_MATCH);
        assertThat(result.matchedKeywords()).isEmpty();
    }

    @Test
    void screen_repositoryThrows_degradesToNoMatchAndNeverPropagates() {
        // TRFP-TC-009 (CRITICAL) — ADR-003 / BR-SAFETY-TRFP-002 / CWE-703
        when(redFlagRuleRepository.findByActiveTrue())
                .thenThrow(new DataAccessResourceFailureException("db down")); // FX-007

        PreScreenResult result = assertDoesNotThrow(
                () -> policy().screen("bé bị nga dap dau xuống sàn"));

        assertThat(result.outcome()).isEqualTo(PreScreenOutcome.NO_MATCH);
        assertThat(result.degraded()).isTrue();
        verify(metrics, times(1)).recordDegraded(anyString());
    }

    @Test
    void screen_nullOrBlankInput_returnsNoMatchWithoutRepositoryCall() {
        // TRFP-TC-010 — boundary; precedent TriageRedFlagPolicy.java:32-34
        TriageRedFlagPreScreenPolicy policy = policy();

        for (String input : new String[] {null, "", "   "}) {
            PreScreenResult result = policy.screen(input);
            assertThat(result.outcome()).isEqualTo(PreScreenOutcome.NO_MATCH);
            assertThat(result.degraded()).isFalse();
        }
        verifyNoInteractions(redFlagRuleRepository);
    }

    @Test
    void screen_ruleSetChangeVisibleOnNextCall_noMemoization() {
        // TRFP-TC-019 — ADR-004 read-through freshness (no cache / C7)
        when(redFlagRuleRepository.findByActiveTrue())
                .thenReturn(List.of())
                .thenReturn(List.of(makeRedEscalateRule()));
        TriageRedFlagPreScreenPolicy policy = policy();

        assertThat(policy.screen("bé bị nga dap dau").outcome())
                .isEqualTo(PreScreenOutcome.NO_MATCH);
        assertThat(policy.screen("bé bị nga dap dau").outcome())
                .isEqualTo(PreScreenOutcome.ESCALATE_RED);

        verify(redFlagRuleRepository, times(2)).findByActiveTrue();
    }
}
