package com.carebridge.backend.emergency.repository;

import com.carebridge.backend.emergency.entity.EmergencyMapHandoff;
import com.carebridge.backend.emergency.handoffstatus.HandoffStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface EmergencyMapHandoffRepository extends JpaRepository<EmergencyMapHandoff, UUID> {
    List<EmergencyMapHandoff> findByUserId(UUID userId);
    List<EmergencyMapHandoff> findByStatus(HandoffStatus status);
}
