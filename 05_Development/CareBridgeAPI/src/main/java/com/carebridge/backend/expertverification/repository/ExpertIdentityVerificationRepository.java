package com.carebridge.backend.expertverification.repository;

import com.carebridge.backend.expertverification.entity.ExpertIdentityVerification;
import com.carebridge.backend.expertverification.enums.IdentityReviewStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface ExpertIdentityVerificationRepository
        extends JpaRepository<ExpertIdentityVerification, UUID> {

    Optional<ExpertIdentityVerification> findFirstByExpertProfileIdOrderByCreatedAtDesc(UUID expertProfileId);

    List<ExpertIdentityVerification> findByReviewStatusInOrderByCreatedAtAsc(
            List<IdentityReviewStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT attempt FROM ExpertIdentityVerification attempt WHERE attempt.id = :id")
    Optional<ExpertIdentityVerification> findByIdForUpdate(@Param("id") UUID id);
}
