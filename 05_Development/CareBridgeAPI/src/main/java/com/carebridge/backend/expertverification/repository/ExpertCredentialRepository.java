package com.carebridge.backend.expertverification.repository;

import com.carebridge.backend.expertverification.entity.ExpertCredential;
import com.carebridge.backend.expertverification.reviewstatus.ReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExpertCredentialRepository extends JpaRepository<ExpertCredential, UUID> {

    List<ExpertCredential> findByExpertProfileId(UUID expertProfileId);

    List<ExpertCredential> findByExpertProfileIdAndReviewStatus(UUID expertProfileId, ReviewStatus reviewStatus);

    List<ExpertCredential> findByReviewStatus(ReviewStatus reviewStatus);

    Optional<ExpertCredential> findByCredentialId(UUID credentialId);

    boolean existsByExpertProfileIdAndCredentialType(UUID expertProfileId, String credentialType);
}
