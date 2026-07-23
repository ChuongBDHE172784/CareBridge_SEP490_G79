package com.carebridge.backend.safety.repository;

import com.carebridge.backend.safety.entity.SafetyEventResponseRecord;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SafetyEventResponseRepository extends JpaRepository<SafetyEventResponseRecord, UUID> {
    boolean existsBySafetyEventId(UUID safetyEventId);
}
