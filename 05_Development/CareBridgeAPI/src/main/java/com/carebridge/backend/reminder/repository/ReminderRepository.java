package com.carebridge.backend.reminder.repository;

import com.carebridge.backend.reminder.entity.Reminder;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReminderRepository extends JpaRepository<Reminder, UUID> {

    Optional<Reminder> findByIdAndOwnerUserId(UUID id, UUID ownerUserId);

    Optional<Reminder> findById(UUID id);
}
