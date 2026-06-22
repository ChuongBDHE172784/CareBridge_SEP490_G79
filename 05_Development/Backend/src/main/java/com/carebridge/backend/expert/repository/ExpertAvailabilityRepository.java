package com.carebridge.backend.expert.repository;

import com.carebridge.backend.expert.entity.ExpertAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ExpertAvailabilityRepository extends JpaRepository<ExpertAvailability, UUID> {

    List<ExpertAvailability> findByExpertProfileIdAndIsActiveTrue(UUID expertProfileId);

    List<ExpertAvailability> findByExpertProfileId(UUID expertProfileId);
}
