package com.carebridge.backend.family.repository;

import com.carebridge.backend.family.entity.CareGroup;
import com.carebridge.backend.family.entity.CareGroupStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CareGroupRepository extends JpaRepository<CareGroup, UUID> {

    long countByOwnerUserIdAndStatus(UUID ownerUserId, CareGroupStatus status);

    boolean existsByOwnerUserIdAndGroupNameIgnoreCase(UUID ownerUserId, String groupName);

    Optional<CareGroup> findByIdAndStatus(UUID id, CareGroupStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select careGroup from CareGroup careGroup where careGroup.id = :id")
    Optional<CareGroup> findByIdForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select careGroup from CareGroup careGroup
             where careGroup.id = :groupId
               and careGroup.ownerUserId = :ownerUserId
            """)
    Optional<CareGroup> findByIdAndOwnerUserIdForUpdate(
            @Param("groupId") UUID groupId,
            @Param("ownerUserId") UUID ownerUserId);

    List<CareGroup> findByOwnerUserIdAndStatus(UUID ownerUserId, CareGroupStatus status);

    List<CareGroup> findByStatus(CareGroupStatus status);

    @Query(value = """
            SELECT DISTINCT group_row.owner_user_id
              FROM care_groups group_row
              JOIN care_group_members member_row
                ON member_row.care_group_id = group_row.care_group_id
             WHERE group_row.status = 'ACTIVE'
               AND member_row.user_id = :actorUserId
               AND member_row.invitation_status = 'ACCEPTED'
               AND COALESCE((member_row.permission_json ->> 'CHECKLIST_VIEW')::boolean, false)
            """, nativeQuery = true)
    List<UUID> findActiveOwnerUserIdsForChecklistViewer(
            @Param("actorUserId") UUID actorUserId);

    List<CareGroup> findByLinkedBabyProfileId(UUID linkedBabyProfileId);

    List<CareGroup> findByLinkedJourneyId(UUID linkedJourneyId);
}
