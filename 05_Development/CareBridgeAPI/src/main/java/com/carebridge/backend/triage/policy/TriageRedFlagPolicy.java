package com.carebridge.backend.triage.policy;

import com.carebridge.backend.triage.entity.RedFlagRule;
import com.carebridge.backend.triage.entity.RedFlagSeverity;
import com.carebridge.backend.triage.repository.RedFlagRuleRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TriageRedFlagPolicy {

    private static final Logger log = LoggerFactory.getLogger(TriageRedFlagPolicy.class);

    // C2 (TDS §17.1): fail-safe floor — always evaluated first and independently of the DB (ADR-001).
    private static final List<String> FLOOR_KEYWORDS = List.of(
            "chảy máu nhiều", "ngất xỉu", "khó thở", "co giật", "tim ngừng đập",
            "xuất huyết", "hôn mê", "đau ngực dữ dội", "sảy thai", "sinh non",
            "ngộ độc", "bất tỉnh", "đuối nước", "gãy xương hở", "bỏng nặng",
            "mất ý thức", "không thở", "đau bụng dữ dội", "chảy máu âm đạo nhiều");

    private static final String EMERGENCY_GUIDANCE =
            "Đây có thể là tình huống khẩn cấp y tế. " +
            "Hãy gọi 115 hoặc đến cơ sở y tế gần nhất ngay lập tức. Đừng chờ đợi.";

    private final RedFlagRuleRepository redFlagRuleRepository;

    public boolean isRedFlag(String query) {
        if (query == null || query.isBlank()) {
            return false;
        }
        String lowerQuery = query.toLowerCase();

        // Step 1 (C2): floor check — unconditional, independent of DB state.
        if (FLOOR_KEYWORDS.stream().anyMatch(keyword -> lowerQuery.contains(keyword.toLowerCase()))) {
            return true;
        }

        // Step 2 (ADR-001/ADR-004): DB-backed additive rules, read-through, no cache.
        // C3/BR-SAFETY-RFR-002: a DB error must fail-closed to floor-only, never throw, never fail-open.
        try {
            List<RedFlagRule> activeRedRules = redFlagRuleRepository.findBySeverityAndActiveTrue(RedFlagSeverity.RED);
            return activeRedRules.stream()
                    .anyMatch(rule -> lowerQuery.contains(rule.getKeyword().toLowerCase()));
        } catch (RuntimeException ex) {
            log.warn("red_flag_rules lookup failed, falling back to floor-only", ex);
            return false;
        }
    }

    public String getEmergencyGuidance() {
        return EMERGENCY_GUIDANCE;
    }
}
