package com.carebridge.backend.expertverification.repository;

import com.carebridge.backend.expertverification.entity.ExpertIdentityVerification;
import com.carebridge.backend.expertverification.enums.IdentityReviewStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpertIdentityVerificationRepository
        extends JpaRepository<ExpertIdentityVerification, UUID> {

    Optional<ExpertIdentityVerification> findFirstByExpertProfileIdOrderByCreatedAtDesc(UUID expertProfileId);

    List<ExpertIdentityVerification> findByReviewStatusInOrderByCreatedAtAsc(
            List<IdentityReviewStatus> statuses);
}
