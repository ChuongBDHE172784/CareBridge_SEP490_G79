package com.carebridge.backend.community.repository;

import com.carebridge.backend.community.entity.CommunityProfile;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommunityProfileRepository extends JpaRepository<CommunityProfile, UUID> {

    boolean existsByUserId(UUID userId);

    Optional<CommunityProfile> findByUserId(UUID userId);
}
