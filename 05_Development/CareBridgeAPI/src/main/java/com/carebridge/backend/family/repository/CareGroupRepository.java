package com.carebridge.backend.family.repository;

import com.carebridge.backend.family.entity.CareGroup;
import com.carebridge.backend.family.entity.CareGroupStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CareGroupRepository extends JpaRepository<CareGroup, UUID> {

    long countByOwnerUserIdAndStatus(UUID ownerUserId, CareGroupStatus status);

    Optional<CareGroup> findByIdAndStatus(UUID id, CareGroupStatus status);

    List<CareGroup> findByOwnerUserIdAndStatus(UUID ownerUserId, CareGroupStatus status);
}
