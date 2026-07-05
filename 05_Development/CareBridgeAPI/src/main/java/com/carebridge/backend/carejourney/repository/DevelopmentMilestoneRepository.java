package com.carebridge.backend.carejourney.repository;

import com.carebridge.backend.carejourney.entity.DevelopmentMilestone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DevelopmentMilestoneRepository extends JpaRepository<DevelopmentMilestone, UUID> {

    List<DevelopmentMilestone> findByBabyIdOrderByAchievedDateDesc(UUID babyId);
}
