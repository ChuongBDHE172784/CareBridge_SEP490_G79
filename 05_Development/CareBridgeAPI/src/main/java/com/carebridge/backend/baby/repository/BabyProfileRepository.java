package com.carebridge.backend.baby.repository;

import com.carebridge.backend.baby.entity.BabyProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BabyProfileRepository extends JpaRepository<BabyProfile, UUID> {

    long countByOwnerUserId(UUID ownerUserId);
}
