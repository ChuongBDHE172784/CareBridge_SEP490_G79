package com.carebridge.backend.safety.repository;

import com.carebridge.backend.safety.entity.SafetyEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ISafetyEventRepository extends JpaRepository<SafetyEvent, UUID> {
    Page<SafetyEvent> findByUserIdOrderByDetectedAtDesc(UUID userId, Pageable pageable);
}
