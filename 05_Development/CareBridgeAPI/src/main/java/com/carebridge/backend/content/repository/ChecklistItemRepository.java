package com.carebridge.backend.content.repository;

import com.carebridge.backend.content.entity.ChecklistItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.carebridge.backend.content.entity.ChecklistTemplateStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.util.Set;

@Repository
public interface ChecklistItemRepository extends JpaRepository<ChecklistItem, UUID> {

    List<ChecklistItem> findByTemplate_IdOrderByOrder(UUID templateId);

    @Query("select i from ChecklistItem i join fetch i.template t " +
            "where t.id in :templateIds and t.status=:status " +
            "order by t.id, case when i.order is null then 1 else 0 end, i.order, i.id")
    List<ChecklistItem> findAllByApprovedTemplateIds(
            @Param("templateIds") Set<UUID> templateIds,
            @Param("status") ChecklistTemplateStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from ChecklistItem i join fetch i.template t " +
            "where i.id in :ids and t.status=:status and t.stage=:stage order by i.id")
    List<ChecklistItem> findAllAvailableByIdInForUpdate(
            @Param("ids") List<UUID> ids,
            @Param("status") ChecklistTemplateStatus status,
            @Param("stage") com.carebridge.backend.content.entity.ContentStage stage);

    @Query("select i.template.id as templateId, count(i.id) as itemCount from ChecklistItem i " +
            "where i.template.id in :templateIds group by i.template.id")
    List<TemplateItemCount> countByTemplateIds(@Param("templateIds") Set<UUID> templateIds);
}
