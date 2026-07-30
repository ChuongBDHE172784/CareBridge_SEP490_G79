package com.carebridge.backend.identity.admin.repository;

import com.carebridge.backend.identity.admin.entity.AccountLockAppeal;
import com.carebridge.backend.identity.admin.entity.AccountLockAppealStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountLockAppealRepository extends JpaRepository<AccountLockAppeal, UUID> {
    Page<AccountLockAppeal> findByStatusOrderBySubmittedAtDesc(
            AccountLockAppealStatus status, Pageable pageable);

    Optional<AccountLockAppeal> findByIdAndStatus(UUID id, AccountLockAppealStatus status);

    boolean existsByUserIdAndLockEpisodeIdAndStatus(
            UUID userId, UUID lockEpisodeId, AccountLockAppealStatus status);

    boolean existsByUserIdAndLockEpisodeId(UUID userId, UUID lockEpisodeId);

    Optional<AccountLockAppeal> findTopByUserIdAndLockEpisodeIdOrderBySubmittedAtDesc(
            UUID userId, UUID lockEpisodeId);

    @Modifying
    @Query("""
            update AccountLockAppeal a
               set a.status = com.carebridge.backend.identity.admin.entity.AccountLockAppealStatus.CANCELLED,
                   a.reviewedAt = :now,
                   a.reviewedBy = :reviewerId,
                   a.reviewNote = :note
             where a.userId = :userId
               and a.lockEpisodeId = :episodeId
               and a.status = com.carebridge.backend.identity.admin.entity.AccountLockAppealStatus.PENDING
            """)
    int cancelPending(@Param("userId") UUID userId,
                      @Param("episodeId") UUID episodeId,
                      @Param("now") java.time.Instant now,
                      @Param("reviewerId") UUID reviewerId,
                      @Param("note") String note);
}
