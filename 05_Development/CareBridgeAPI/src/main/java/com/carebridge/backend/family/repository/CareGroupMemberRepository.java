package com.carebridge.backend.family.repository;

import com.carebridge.backend.family.entity.CareGroupMember;
import com.carebridge.backend.family.entity.InviteStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CareGroupMemberRepository extends JpaRepository<CareGroupMember, UUID> {

    boolean existsByCareGroupIdAndUserIdAndInviteStatus(UUID careGroupId, UUID userId, InviteStatus status);

    List<CareGroupMember> findByCareGroupIdAndInviteStatusIn(UUID careGroupId, List<InviteStatus> statuses);

    long countByCareGroupId(UUID careGroupId);

    Optional<CareGroupMember> findByCareGroupIdAndUserId(UUID careGroupId, UUID userId);

    List<CareGroupMember> findByUserIdAndInviteStatus(UUID userId, InviteStatus status);
}
