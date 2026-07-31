package com.carebridge.backend.checklist.repository;

import com.carebridge.backend.checklist.entity.ChecklistActionCommand;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChecklistActionCommandRepository extends JpaRepository<ChecklistActionCommand, UUID> {
    @Query(value = "SELECT TRUE FROM pg_advisory_xact_lock(hashtextextended(CAST(:scope AS text), 0))",
            nativeQuery = true)
    Boolean acquireTaskActionLock(@Param("scope") String scope);

    @Query(value = "SELECT TRUE FROM pg_advisory_xact_lock(hashtextextended(CAST(:scope AS text), 0))",
            nativeQuery = true)
    Boolean acquireIdempotencyClaimLock(@Param("scope") String scope);

    Optional<ChecklistActionCommand> findByActorUserIdAndTaskKindAndTaskIdAndClientRequestId(
            UUID actorUserId, String taskKind, UUID taskId, UUID clientRequestId);

    boolean existsByTaskKindAndTaskIdIn(String taskKind, Collection<UUID> taskIds);
}
