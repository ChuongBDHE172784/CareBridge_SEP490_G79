package com.carebridge.backend.aimoderation.repository;

import com.carebridge.backend.aimoderation.entity.AiAssessmentStatus;
import com.carebridge.backend.aimoderation.entity.AiContentAssessment;
import com.carebridge.backend.content.entity.ReportTargetType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AiContentAssessmentRepository extends JpaRepository<AiContentAssessment, UUID> {

    // Idempotency probe: one successful assessment per (target, content, policy set, model)
    Optional<AiContentAssessment> findFirstByTargetTypeAndTargetIdAndContentHashAndPolicySetHashAndModelAndStatus(
            ReportTargetType targetType, UUID targetId, String contentHash, String policySetHash,
            String model, AiAssessmentStatus status);

    Optional<AiContentAssessment> findFirstByModerationCaseIdOrderByCreatedAtDesc(UUID moderationCaseId);

    Optional<AiContentAssessment> findFirstByTargetTypeAndTargetIdOrderByCreatedAtDesc(
            ReportTargetType targetType, UUID targetId);

    Optional<AiContentAssessment> findFirstByStatusOrderByCompletedAtDesc(AiAssessmentStatus status);
}
