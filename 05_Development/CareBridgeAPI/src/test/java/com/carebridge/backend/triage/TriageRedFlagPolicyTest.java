package com.carebridge.backend.triage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.carebridge.backend.triage.entity.RedFlagSeverity;
import com.carebridge.backend.triage.policy.TriageRedFlagPolicy;
import com.carebridge.backend.triage.repository.RedFlagRuleRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

// CRITICAL — BR-SAFETY fail-safe test cases (RFR-TC-011..015)
@ExtendWith(MockitoExtension.class)
class TriageRedFlagPolicyTest {

    @Mock
    private RedFlagRuleRepository redFlagRuleRepository;

    private static final String FLOOR_QUERY = "tôi bị khó thở dữ dội"; // FX-008

    // RFR-TC-011 — CRITICAL
    // lenient(): per ADR-001/C2, the floor check runs FIRST and independently — for a floor-matching
    // query the repository is never even consulted, so this precondition stub is legitimately unused.
    // That the stub goes unconsumed is itself evidence the floor-first short-circuit is real.
    @Test
    void isRedFlag_floorKeyword_matchesWhenDbEmpty() {
        lenient().when(redFlagRuleRepository.findBySeverityAndActiveTrue(RedFlagSeverity.RED))
                .thenReturn(List.of());
        TriageRedFlagPolicy policy = new TriageRedFlagPolicy(redFlagRuleRepository);

        boolean result = policy.isRedFlag(FLOOR_QUERY);

        assertThat(result).isTrue();
    }

    // RFR-TC-012 — CRITICAL (see RFR-TC-011 note on lenient())
    @Test
    void isRedFlag_floorKeyword_matchesWhenRepositoryThrows() {
        lenient().when(redFlagRuleRepository.findBySeverityAndActiveTrue(RedFlagSeverity.RED))
                .thenThrow(new DataAccessResourceFailureException("simulated DB outage"));
        TriageRedFlagPolicy policy = new TriageRedFlagPolicy(redFlagRuleRepository);

        assertThatCode(() -> {
            boolean result = policy.isRedFlag(FLOOR_QUERY);
            assertThat(result).isTrue();
        }).doesNotThrowAnyException();
    }

    // RFR-TC-013
    @Test
    void isRedFlag_dbAddedRedKeyword_matchesAdditively() {
        when(redFlagRuleRepository.findBySeverityAndActiveTrue(RedFlagSeverity.RED))
                .thenReturn(List.of(RedFlagRuleTestFactory.makeAdminRule()));
        TriageRedFlagPolicy policy = new TriageRedFlagPolicy(redFlagRuleRepository);

        boolean result = policy.isRedFlag("Tôi đang có từ khoá thử nghiệm khẩn cấp, phải làm sao?");

        assertThat(result).isTrue();
    }

    // RFR-TC-014 — ADR-003 boundary
    @Test
    void isRedFlag_greenSeverityRule_doesNotTrigger() {
        when(redFlagRuleRepository.findBySeverityAndActiveTrue(RedFlagSeverity.RED)).thenReturn(List.of());
        TriageRedFlagPolicy policy = new TriageRedFlagPolicy(redFlagRuleRepository);

        boolean result = policy.isRedFlag("tôi bị đau đầu nhẹ hôm nay");

        assertThat(result).isFalse();
    }

    // RFR-TC-015
    @Test
    void isRedFlag_deactivatedRule_doesNotTrigger() {
        when(redFlagRuleRepository.findBySeverityAndActiveTrue(RedFlagSeverity.RED)).thenReturn(List.of());
        TriageRedFlagPolicy policy = new TriageRedFlagPolicy(redFlagRuleRepository);

        boolean result = policy.isRedFlag("tôi đã tắt thông báo này");

        assertThat(result).isFalse();
    }
}
