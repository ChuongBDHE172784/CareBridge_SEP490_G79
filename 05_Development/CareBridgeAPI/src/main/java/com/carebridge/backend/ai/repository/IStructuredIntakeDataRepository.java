package com.carebridge.backend.ai.repository;

import com.carebridge.backend.ai.entity.StructuredIntakeData;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface IStructuredIntakeDataRepository extends JpaRepository<StructuredIntakeData, UUID> {
    Optional<StructuredIntakeData> findBySessionId(UUID sessionId);
    boolean existsBySessionId(UUID sessionId);
}
