package com.carebridge.backend.baby.repository;

import com.carebridge.backend.baby.entity.BabyProfile;
import com.carebridge.backend.baby.entity.BabyProfileStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BabyProfileRepository extends JpaRepository<BabyProfile, UUID> {

    long countByOwnerUserId(UUID ownerUserId);

    List<BabyProfile> findByOwnerUserIdAndStatusOrderByCreatedAtAsc(UUID ownerUserId, BabyProfileStatus status);

    Optional<BabyProfile> findByIdAndOwnerUserId(UUID id, UUID ownerUserId);

    @Lock(LockModeType.PESSIMISTIC_READ)
    Optional<BabyProfile> findByIdAndOwnerUserIdAndRelatedJourneyIdAndStatusAndActiveTrue(
            UUID id, UUID ownerUserId, UUID relatedJourneyId, BabyProfileStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from BabyProfile b where b.id=:id and b.ownerUserId=:owner")
    Optional<BabyProfile> findOwnedByIdForUpdate(@Param("id") UUID id, @Param("owner") UUID owner);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from BabyProfile b where b.id=:id and b.ownerUserId=:owner "
            + "and b.status=com.carebridge.backend.baby.entity.BabyProfileStatus.ACTIVE and b.active=true")
    Optional<BabyProfile> findOwnedActiveByIdForUpdate(
            @Param("id") UUID id, @Param("owner") UUID owner);

    Page<BabyProfile> findByOwnerUserIdAndRelatedJourneyIdAndStatus(
            UUID ownerUserId, UUID relatedJourneyId, BabyProfileStatus status, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE BabyProfile b SET b.active = :active WHERE b.ownerUserId = :ownerUserId")
    int updateActiveByOwnerUserId(@Param("ownerUserId") UUID ownerUserId, @Param("active") Boolean active);
}
