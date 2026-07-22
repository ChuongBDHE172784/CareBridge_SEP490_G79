package com.carebridge.backend.content.repository;

import com.carebridge.backend.content.entity.ChecklistTemplate;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.entity.ContentStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ChecklistTemplateRepository extends JpaRepository<ChecklistTemplate, UUID> {

    List<ChecklistTemplate> findByStage(ContentStage stage);

    // UC-243 §14 addendum — admin workspace + approval queue filter, null-safe AND (same pattern
    // as ContentRepository.findByAdminFilters — see its comment for why branching if/else is wrong here)
    @Query("SELECT t FROM ChecklistTemplate t WHERE " +
           "(:stage IS NULL OR t.stage = :stage) AND " +
           "(:status IS NULL OR t.status = :status)")
    Page<ChecklistTemplate> findByAdminFilters(
            @Param("stage") ContentStage stage,
            @Param("status") ContentStatus status,
            Pageable pageable);
}
