package com.carebridge.backend.baby.repository;

import com.carebridge.backend.baby.entity.BabyProfile;
import com.carebridge.backend.baby.entity.BabyProfileStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BabyProfileRepository extends JpaRepository<BabyProfile, UUID> {

    long countByOwnerUserId(UUID ownerUserId);

    List<BabyProfile> findByOwnerUserIdAndStatusOrderByCreatedAtAsc(UUID ownerUserId, BabyProfileStatus status);

    List<BabyProfile> findByStatus(BabyProfileStatus status);

    Optional<BabyProfile> findByIdAndOwnerUserId(UUID id, UUID ownerUserId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from BabyProfile b where b.id=:id and b.ownerUserId=:owner")
    Optional<BabyProfile> findOwnedByIdForUpdate(@Param("id") UUID id, @Param("owner") UUID owner);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from BabyProfile b where b.id=:id and b.ownerUserId=:owner "
            + "and b.status=com.carebridge.backend.baby.entity.BabyProfileStatus.ACTIVE")
    Optional<BabyProfile> findOwnedActiveByIdForUpdate(
            @Param("id") UUID id, @Param("owner") UUID owner);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE users
               SET settings_jsonb = jsonb_set(
                   CASE WHEN jsonb_typeof(settings_jsonb) = 'object'
                        THEN settings_jsonb ELSE '{}'::jsonb END,
                   '{activeBabyId}',
                   to_jsonb(cast(:babyId as text)),
                   true),
                   updated_at = now()
             WHERE user_id = :ownerUserId
            """, nativeQuery = true)
    int setActiveBaby(@Param("ownerUserId") UUID ownerUserId, @Param("babyId") UUID babyId);

    @Query(value = """
            SELECT CASE
                     WHEN jsonb_typeof(settings_jsonb) = 'object'
                      AND settings_jsonb ->> 'activeBabyId' ~*
                          '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
                     THEN (settings_jsonb ->> 'activeBabyId')::uuid
                     ELSE NULL
                   END
              FROM users
             WHERE user_id = :viewerId
            """, nativeQuery = true)
    Optional<UUID> findActiveBabyId(@Param("viewerId") UUID viewerId);
}
