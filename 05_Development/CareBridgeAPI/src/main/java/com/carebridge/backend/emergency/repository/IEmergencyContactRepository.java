package com.carebridge.backend.emergency.repository;

import com.carebridge.backend.emergency.entity.EmergencyContact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IEmergencyContactRepository extends JpaRepository<EmergencyContact, UUID> {
    Optional<EmergencyContact> findByUserId(UUID userId);
}
