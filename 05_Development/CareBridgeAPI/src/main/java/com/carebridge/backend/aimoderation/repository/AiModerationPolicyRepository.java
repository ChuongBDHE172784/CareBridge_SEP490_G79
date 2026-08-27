package com.carebridge.backend.aimoderation.repository;

import com.carebridge.backend.aimoderation.entity.AiModerationPolicy;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AiModerationPolicyRepository extends JpaRepository<AiModerationPolicy, UUID> {

    Optional<AiModerationPolicy> findByPolicyCode(String policyCode);

    boolean existsByPolicyCodeIgnoreCase(String policyCode);

    List<AiModerationPolicy> findByActiveTrueOrderByPolicyCodeAsc();

    Page<AiModerationPolicy> findByActive(boolean active, Pageable pageable);
}
