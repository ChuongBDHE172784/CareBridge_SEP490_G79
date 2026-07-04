package com.carebridge.backend.expertavailability.repository;

import com.carebridge.backend.expertavailability.entity.ExpertLocationShare;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface ExpertLocationShareRepository extends JpaRepository<ExpertLocationShare, UUID> {
    Optional<ExpertLocationShare> findTopByExpertProfileIdOrderByCreatedAtDesc(UUID expertProfileId);
}
