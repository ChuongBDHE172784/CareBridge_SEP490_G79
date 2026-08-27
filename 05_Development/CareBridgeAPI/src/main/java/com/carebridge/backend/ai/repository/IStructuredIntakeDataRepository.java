package com.carebridge.backend.ai.repository;

import com.carebridge.backend.ai.entity.StructuredIntakeData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;

public interface IStructuredIntakeDataRepository extends JpaRepository<StructuredIntakeData, UUID> {
    @Query("select data from StructuredIntakeData data where data.id = :sessionId and data.symptomList is not null")
    Optional<StructuredIntakeData> findBySessionId(@Param("sessionId") UUID sessionId);

    @Query("select (count(data) > 0) from StructuredIntakeData data where data.id = :sessionId and data.symptomList is not null")
    boolean existsBySessionId(@Param("sessionId") UUID sessionId);
}
