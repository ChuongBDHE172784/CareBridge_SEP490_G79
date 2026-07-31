package com.carebridge.backend.content.repository;

import com.carebridge.backend.content.entity.ChecklistItem;
import com.carebridge.backend.content.entity.ChecklistTemplateStatus;
import com.carebridge.backend.content.entity.ContentStage;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ChecklistItemRepository extends JpaRepository<ChecklistItem, UUID> {

    @Query("select i from ChecklistItem i where i.template.id=:templateId and i.isActive=true " +
            "order by case when i.order is null then 1 else 0 end, i.order, i.id")
    List<ChecklistItem> findByTemplate_IdOrderByOrder(@Param("templateId") UUID templateId);

    @Query("select i from ChecklistItem i where i.template.id=:templateId " +
            "order by case when i.order is null then 1 else 0 end, i.order, i.id")
    List<ChecklistItem> findAllByTemplateIdOrderByOrder(@Param("templateId") UUID templateId);

    @Query("select i from ChecklistItem i join fetch i.template t " +
            "where t.id in :templateIds and t.status=:status " +
            "and t.migrationReviewRequired=false and i.isActive=true " +
            "order by t.id, case when i.order is null then 1 else 0 end, i.order, i.id")
    List<ChecklistItem> findAllByApprovedTemplateIds(
            @Param("templateIds") Set<UUID> templateIds,
            @Param("status") ChecklistTemplateStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from ChecklistItem i join fetch i.template t " +
            "where i.id in :ids and i.isActive=true and t.status=:status and t.stage=:stage " +
            "and t.distributionEnabled=true and t.migrationReviewRequired=false order by i.id")
    List<ChecklistItem> findAllAvailableByIdInForUpdate(
            @Param("ids") List<UUID> ids,
            @Param("status") ChecklistTemplateStatus status,
            @Param("stage") ContentStage stage);

    @Query("select i from ChecklistItem i join fetch i.template where i.id in :ids")
    List<ChecklistItem> findAllWithTemplateByIdIn(@Param("ids") List<UUID> ids);

    @Query("select i.template.id as templateId, count(i.id) as itemCount from ChecklistItem i " +
            "where i.template.id in :templateIds and i.isActive=true group by i.template.id")
    List<TemplateItemCount> countByTemplateIds(@Param("templateIds") Set<UUID> templateIds);
}
