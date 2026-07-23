package com.carebridge.backend.content.repository;

import com.carebridge.backend.content.entity.ChecklistItem;
import com.carebridge.backend.content.entity.ContentStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

@Repository
public interface ChecklistItemRepository extends JpaRepository<ChecklistItem, UUID> {

    List<ChecklistItem> findByTemplate_IdOrderByOrder(UUID templateId);

    @Lock(LockModeType.PESSIMISTIC_READ)
    Optional<ChecklistItem> findByIdAndTemplate_Status(UUID id, ContentStatus status);
}
