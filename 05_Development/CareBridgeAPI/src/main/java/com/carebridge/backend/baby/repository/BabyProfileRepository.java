package com.carebridge.backend.baby.repository;

import com.carebridge.backend.baby.entity.BabyProfile;
import com.carebridge.backend.baby.entity.BabyProfileStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BabyProfileRepository extends JpaRepository<BabyProfile, UUID> {

    long countByOwnerUserId(UUID ownerUserId);

    List<BabyProfile> findByOwnerUserIdAndStatusOrderByCreatedAtAsc(UUID ownerUserId, BabyProfileStatus status);

    Optional<BabyProfile> findByIdAndOwnerUserId(UUID id, UUID ownerUserId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE BabyProfile b SET b.active = :active WHERE b.ownerUserId = :ownerUserId")
    int updateActiveByOwnerUserId(@Param("ownerUserId") UUID ownerUserId, @Param("active") Boolean active);
}
