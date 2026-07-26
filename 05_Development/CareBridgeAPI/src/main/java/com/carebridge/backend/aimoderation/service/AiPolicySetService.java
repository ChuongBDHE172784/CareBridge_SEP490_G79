package com.carebridge.backend.aimoderation.service;

import com.carebridge.backend.aimoderation.entity.AiModerationPolicy;
import com.carebridge.backend.aimoderation.policy.AiContentHasher;
import com.carebridge.backend.aimoderation.repository.AiModerationPolicyRepository;
import com.carebridge.backend.content.entity.ReportTargetType;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Snapshot of the active policy set plus its deterministic fingerprint. The fingerprint is
 * part of the assessment idempotency key: any change that affects classification (guidance,
 * category, severity, threshold, target types, activation — all of which bump the policy
 * version) produces a new hash, so changed policies trigger fresh assessments.
 */
@Service
@RequiredArgsConstructor
public class AiPolicySetService {

    public record AiPolicySet(List<AiModerationPolicy> policies,
                              Map<String, AiModerationPolicy> byCode,
                              String policySetHash) {

        public boolean isEmpty() {
            return policies.isEmpty();
        }
    }

    private final AiModerationPolicyRepository policyRepository;

    @Transactional(readOnly = true)
    public AiPolicySet activeSnapshotFor(ReportTargetType targetType) {
        List<AiModerationPolicy> active = policyRepository.findByActiveTrueOrderByPolicyCodeAsc().stream()
                .filter(policy -> policy.appliesTo(targetType))
                .toList();
        return new AiPolicySet(active, byCode(active), fingerprint(active));
    }

    @Transactional(readOnly = true)
    public String currentHash() {
        return fingerprint(policyRepository.findByActiveTrueOrderByPolicyCodeAsc());
    }

    private static Map<String, AiModerationPolicy> byCode(List<AiModerationPolicy> policies) {
        return policies.stream().collect(Collectors.toUnmodifiableMap(AiModerationPolicy::getPolicyCode,
                Function.identity()));
    }

    static String fingerprint(List<AiModerationPolicy> policies) {
        String joined = policies.stream()
                .sorted(java.util.Comparator.comparing(AiModerationPolicy::getPolicyCode))
                .map(p -> String.join(":",
                        p.getPolicyCode(),
                        String.valueOf(p.getVersion()),
                        p.getSeverity().name(),
                        p.getViolationCategory().name(),
                        p.getConfidenceThreshold().stripTrailingZeros().toPlainString(),
                        p.getApplicableTargetTypes()))
                .collect(Collectors.joining("|"));
        return AiContentHasher.sha256Hex(joined);
    }
}
