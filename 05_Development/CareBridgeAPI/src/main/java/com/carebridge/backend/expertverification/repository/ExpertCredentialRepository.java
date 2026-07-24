package com.carebridge.backend.expertverification.repository;

import com.carebridge.backend.expert.entity.ExpertProfile;
import com.carebridge.backend.expertverification.entity.ExpertCredential;
import com.carebridge.backend.expertverification.reviewstatus.ReviewStatus;
import com.carebridge.backend.security.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;

public interface ExpertCredentialRepository extends JpaRepository<ExpertCredential, UUID> {

	List<ExpertCredential> findByExpertProfileId(UUID expertProfileId);

	List<ExpertCredential> findByExpertProfileIdAndReviewStatus(UUID expertProfileId, ReviewStatus reviewStatus);

	Optional<ExpertCredential> findFirstByExpertProfileIdAndReviewStatusOrderByReviewedAtDescCreatedAtDesc(
		UUID expertProfileId, ReviewStatus reviewStatus);

	List<ExpertCredential> findByReviewStatus(ReviewStatus reviewStatus);

	Optional<ExpertCredential> findByCredentialId(UUID credentialId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT credential FROM ExpertCredential credential WHERE credential.credentialId = :credentialId")
	Optional<ExpertCredential> findByCredentialIdForUpdate(@Param("credentialId") UUID credentialId);

	boolean existsByExpertProfileIdAndCredentialType(UUID expertProfileId, String credentialType);

	@Query("SELECT c, p, u FROM ExpertCredential c " +
		"LEFT JOIN ExpertProfile p ON c.expertProfileId = p.expertProfileId " +
		"LEFT JOIN User u ON p.userId = u.id " +
		"WHERE c.reviewStatus = :status " +
		"ORDER BY c.createdAt DESC")
	List<Object[]> findPendingWithExpert(@Param("status") ReviewStatus status);
}
