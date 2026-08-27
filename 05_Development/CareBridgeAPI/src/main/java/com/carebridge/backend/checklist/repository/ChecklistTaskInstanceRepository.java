package com.carebridge.backend.checklist.repository;

import com.carebridge.backend.checklist.entity.ChecklistTaskInstance;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface ChecklistTaskInstanceRepository extends JpaRepository<ChecklistTaskInstance, UUID> {
    @Query(value = "SELECT TRUE FROM pg_advisory_xact_lock(hashtextextended(CAST(:key AS text), 0))",
            nativeQuery = true)
    Boolean acquireTaskKeyLock(@Param("key") String key);

    Optional<ChecklistTaskInstance> findByTaskKey(String taskKey);
    List<ChecklistTaskInstance> findByChecklistInstanceIdOrderByDisplayOrder(UUID checklistInstanceId);

    @Query("""
            SELECT task
            FROM ChecklistTaskInstance task
            WHERE task.checklistInstanceId IN :instanceIds
            ORDER BY task.checklistInstanceId ASC, task.displayOrder ASC, task.id ASC
            """)
    List<ChecklistTaskInstance> findAllByChecklistInstanceIds(
            @Param("instanceIds") List<UUID> instanceIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ChecklistTaskInstance> findForUpdateById(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT task
            FROM ChecklistTaskInstance task
            WHERE task.checklistInstanceId = :instanceId
            ORDER BY task.taskKey ASC
            """)
    List<ChecklistTaskInstance> findAllForUpdateByChecklistInstanceIdOrderByTaskKey(
            @Param("instanceId") UUID instanceId);
}
