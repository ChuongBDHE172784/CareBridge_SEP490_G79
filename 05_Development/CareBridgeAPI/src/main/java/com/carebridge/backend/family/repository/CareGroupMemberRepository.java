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

    boolean existsByCareGroupIdAndUserIdAndInviteStatus(UUID careGroupId, UUID userId, InviteStatus status);

    List<CareGroupMember> findByCareGroupIdAndInviteStatusIn(UUID careGroupId, List<InviteStatus> statuses);

    long countByCareGroupId(UUID careGroupId);

    void deleteByCareGroupId(UUID careGroupId);

    List<CareGroupMember> findAllByCareGroupIdAndUserId(UUID careGroupId, UUID userId);

    default Optional<CareGroupMember> findByCareGroupIdAndUserId(UUID careGroupId, UUID userId) {
        List<CareGroupMember> list = findAllByCareGroupIdAndUserId(careGroupId, userId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
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
