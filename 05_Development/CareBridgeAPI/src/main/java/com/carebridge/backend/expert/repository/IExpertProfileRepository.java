package com.carebridge.backend.expert.repository;

import com.carebridge.backend.expert.entity.ExpertProfile;
import com.carebridge.backend.expert.entity.ExpertProfileStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IExpertProfileRepository extends JpaRepository<ExpertProfile, UUID> {

    boolean existsByUserId(UUID userId);

    Optional<ExpertProfile> findByUserId(UUID userId);

    List<ExpertProfile> findByStatus(ExpertProfileStatus status);

    List<ExpertProfile> findByStatusAndSpecialtiesContaining(ExpertProfileStatus status, String specialty);
}
