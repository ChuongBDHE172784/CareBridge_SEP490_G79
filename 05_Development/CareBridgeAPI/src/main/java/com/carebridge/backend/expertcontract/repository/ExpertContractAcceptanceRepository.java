package com.carebridge.backend.expertcontract.repository;

import com.carebridge.backend.expertcontract.entity.ExpertContractAcceptance;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpertContractAcceptanceRepository
        extends JpaRepository<ExpertContractAcceptance, UUID> {

    Optional<ExpertContractAcceptance> findTopByExpertUserIdAndDecisionOrderByOccurredAtDesc(
            UUID expertUserId, String decision);

    List<ExpertContractAcceptance> findByExpertUserIdOrderByOccurredAtDesc(UUID expertUserId);

    /**
     * Bản ký còn hiệu lực = row ACCEPTED mới nhất, và sau nó không có row TERMINATED nào.
     * Trả rỗng khi chuyên gia chưa ký hoặc đã bị chấm dứt hợp tác.
     */
    default Optional<ExpertContractAcceptance> findActiveAcceptance(UUID expertUserId) {
        Optional<ExpertContractAcceptance> accepted =
                findTopByExpertUserIdAndDecisionOrderByOccurredAtDesc(
                        expertUserId, ExpertContractAcceptance.DECISION_ACCEPTED);
        if (accepted.isEmpty()) {
            return Optional.empty();
        }
        Optional<ExpertContractAcceptance> terminated =
                findTopByExpertUserIdAndDecisionOrderByOccurredAtDesc(
                        expertUserId, ExpertContractAcceptance.DECISION_TERMINATED);
        boolean stillActive = terminated.isEmpty()
                || terminated.get().getOccurredAt().isBefore(accepted.get().getOccurredAt());
        return stillActive ? accepted : Optional.empty();
    }
}
