package com.carebridge.backend.expertavailability.repository;

import com.carebridge.backend.expertavailability.availabilitystatus.AvailabilityStatus;
import com.carebridge.backend.expertavailability.entity.ExpertAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExpertAvailabilityRepository extends JpaRepository<ExpertAvailability, UUID> {
    List<ExpertAvailability> findByExpertProfileId(UUID expertProfileId);
       Optional<ExpertAvailability> findTopByExpertProfileIdOrderByCreatedAtDesc(UUID expertProfileId);
}
