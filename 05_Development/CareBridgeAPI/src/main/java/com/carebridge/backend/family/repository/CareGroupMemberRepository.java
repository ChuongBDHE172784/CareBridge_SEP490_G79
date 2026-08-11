package com.carebridge.backend.family.repository;

import com.carebridge.backend.family.entity.CareGroupMember;
import com.carebridge.backend.family.entity.InviteStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CareGroupMemberRepository extends JpaRepository<CareGroupMember, UUID> {

    @Query(value = """
            SELECT cgm.user_id
              FROM care_groups cg
              JOIN care_group_members cgm
                ON cgm.care_group_id = cg.care_group_id
             WHERE cg.owner_user_id = :ownerUserId
               AND cg.status = 'ACTIVE'
               AND cgm.invitation_status = 'ACCEPTED'
               AND cgm.is_emergency_contact = TRUE
             GROUP BY cgm.user_id
             ORDER BY MIN(cgm.emergency_contact_priority) ASC NULLS LAST,
                      cgm.user_id ASC
            """, nativeQuery = true)
    List<UUID> findEmergencyContactUserIds(@Param("ownerUserId") UUID ownerUserId);

    /**
     * Emergency fall alerts are delivered to every eligible Family account in an
     * active care group, not only members manually marked as primary contacts.
     * The role join prevents alerts from being delivered to the Mother herself or
     * to non-Family accounts that could otherwise share the same group.
     */
    @Query(value = """
            SELECT cgm.user_id
              FROM care_groups cg
              JOIN care_group_members cgm
                ON cgm.care_group_id = cg.care_group_id
              JOIN users u
                ON u.user_id = cgm.user_id
             WHERE cg.owner_user_id = :ownerUserId
               AND cg.status = 'ACTIVE'
               AND cgm.invitation_status = 'ACCEPTED'
               AND u.role = 'FAMILY'
               AND u.enabled = TRUE
               AND u.locked = FALSE
             GROUP BY cgm.user_id
             ORDER BY cgm.user_id ASC
            """, nativeQuery = true)
    List<UUID> findAcceptedFamilyUserIds(@Param("ownerUserId") UUID ownerUserId);

    /**
     * Keeps the active care-group scope alongside each eligible Family member
     * so emergency notification records can be displayed in that group's
     * dashboard rather than remaining globally unscoped.
     */
    @Query(value = """
            SELECT cgm.*
              FROM care_groups cg
              JOIN care_group_members cgm
                ON cgm.care_group_id = cg.care_group_id
              JOIN users u
                ON u.user_id = cgm.user_id
             WHERE cg.owner_user_id = :ownerUserId
               AND cg.status = 'ACTIVE'
               AND cgm.invitation_status = 'ACCEPTED'
               AND u.role = 'FAMILY'
               AND u.enabled = TRUE
               AND u.locked = FALSE
             ORDER BY cgm.care_group_id ASC, cgm.user_id ASC
            """, nativeQuery = true)
    List<CareGroupMember> findAcceptedFamilyMembersForEmergencyAlerts(
            @Param("ownerUserId") UUID ownerUserId);

    boolean existsByCareGroupIdAndUserIdAndInviteStatus(UUID careGroupId, UUID userId, InviteStatus status);

    /** A delegated Family account may act in the Mother's direct conversation only while its
     * membership is accepted and the Mother's care group remains active. */
    @Query(value = """
            SELECT EXISTS (
                SELECT 1
                  FROM care_groups cg
                  JOIN care_group_members cgm ON cgm.care_group_id = cg.care_group_id
                 WHERE cg.owner_user_id = :motherUserId
                   AND cg.status = 'ACTIVE'
                   AND cgm.user_id = :familyUserId
                   AND cgm.invitation_status = 'ACCEPTED'
            )
            """, nativeQuery = true)
    boolean existsAcceptedMemberOfActiveMotherCareGroup(
            @Param("motherUserId") UUID motherUserId, @Param("familyUserId") UUID familyUserId);

    List<CareGroupMember> findByCareGroupIdAndInviteStatusIn(UUID careGroupId, List<InviteStatus> statuses);

    long countByCareGroupId(UUID careGroupId);

    void deleteByCareGroupId(UUID careGroupId);

    List<CareGroupMember> findAllByCareGroupIdAndUserId(UUID careGroupId, UUID userId);

    default Optional<CareGroupMember> findByCareGroupIdAndUserId(UUID careGroupId, UUID userId) {
        List<CareGroupMember> list = findAllByCareGroupIdAndUserId(careGroupId, userId);
        // A user can have an old REVOKED/EXPIRED invite alongside a later ACCEPTED
        // membership. Authorization must always resolve the active membership first.
        return list.stream()
                .filter(member -> member.getInviteStatus() == InviteStatus.ACCEPTED
                        && (member.getInviteExpiresAt() == null
                        || !member.getInviteExpiresAt().isBefore(Instant.now())))
                .findFirst()
                .or(() -> list.stream().findFirst());
    }

    /**
     * Finds the first member row matching groupId + userId + a specific status.
     * Avoids the ambiguity of findByCareGroupIdAndUserId when a user has multiple
     * rows in the same group (e.g. an OWNER row AND a PENDING invite row).
     */
    Optional<CareGroupMember> findFirstByCareGroupIdAndUserIdAndInviteStatus(
            UUID careGroupId, UUID userId, InviteStatus inviteStatus);


    List<CareGroupMember> findByUserIdAndInviteStatus(UUID userId, InviteStatus status);

    Optional<CareGroupMember> findByInviteToken(String inviteToken);

    long countByCareGroupIdAndInviteStatus(UUID careGroupId, InviteStatus status);

    Optional<CareGroupMember> findByIdAndCareGroupId(UUID memberId, UUID careGroupId);

    /** Self-join requests: PENDING members who have no invite token (they joined by code, not invited by Mother). */
    List<CareGroupMember> findByCareGroupIdAndInviteStatusAndInviteTokenIsNull(UUID careGroupId, InviteStatus status);

    /**
     * UC-83 (ADR-FAM-008): single-use conditional accept.
     * Returns 1 if the row was PENDING and was transitioned; 0 if already in a non-PENDING state (race lost).
     */
    @Modifying
    @Query("UPDATE CareGroupMember m SET m.inviteStatus = com.carebridge.backend.family.entity.InviteStatus.ACCEPTED, " +
           "m.joinedAt = :joinedAt, m.updatedAt = :joinedAt " +
           "WHERE m.id = :id AND m.inviteStatus = com.carebridge.backend.family.entity.InviteStatus.PENDING")
    int acceptIfPending(@Param("id") UUID id, @Param("joinedAt") Instant joinedAt);

    /**
     * UC-83 (ADR-FAM-006): lazy expiry — transitions PENDING+expired row to EXPIRED.
     * Returns 1 if transitioned; 0 if already non-PENDING or not yet past expiresAt.
     */
    @Modifying
    @Query("UPDATE CareGroupMember m SET m.inviteStatus = com.carebridge.backend.family.entity.InviteStatus.EXPIRED, " +
           "m.updatedAt = :now " +
           "WHERE m.id = :id AND m.inviteStatus = com.carebridge.backend.family.entity.InviteStatus.PENDING " +
           "AND m.inviteExpiresAt < :now")
    int markExpiredIfPending(@Param("id") UUID id, @Param("now") Instant now);
}
