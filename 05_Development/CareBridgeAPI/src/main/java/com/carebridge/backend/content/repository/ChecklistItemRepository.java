package com.carebridge.backend.content.repository;

import com.carebridge.backend.content.entity.ChecklistItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChecklistItemRepository extends JpaRepository<ChecklistItem, UUID> {

    List<ChecklistItem> findByTemplate_IdOrderByOrder(UUID templateId);
}
