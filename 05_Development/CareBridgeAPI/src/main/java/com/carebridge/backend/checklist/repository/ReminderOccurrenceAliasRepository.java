package com.carebridge.backend.checklist.repository;

import com.carebridge.backend.checklist.entity.ReminderOccurrenceAlias;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReminderOccurrenceAliasRepository
        extends JpaRepository<ReminderOccurrenceAlias, UUID> {
    Optional<ReminderOccurrenceAlias> findByOccurrenceId(UUID occurrenceId);

    Optional<ReminderOccurrenceAlias> findByOccurrenceIdAndOwnerUserId(
            UUID occurrenceId, UUID ownerUserId);
}
