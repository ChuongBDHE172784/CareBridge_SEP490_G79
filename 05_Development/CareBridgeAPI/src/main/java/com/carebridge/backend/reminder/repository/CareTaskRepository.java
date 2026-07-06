package com.carebridge.backend.reminder.repository;

import com.carebridge.backend.reminder.entity.CareTask;
import com.carebridge.backend.reminder.entity.CareTaskStatus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface CareTaskRepository extends JpaRepository<CareTask, UUID> {

    List<CareTask> findByAssignedToAndStatusAndDueAtBetween(
            UUID assignedTo, CareTaskStatus status, Instant start, Instant end);
}
