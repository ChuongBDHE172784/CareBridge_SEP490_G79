package com.carebridge.backend.content.repository;

import com.carebridge.backend.content.entity.ChecklistTemplate;
import com.carebridge.backend.content.entity.ChecklistTemplateStatus;
import com.carebridge.backend.content.entity.ContentStage;
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

    List<ChecklistTemplate> findByStatusOrderByUpdatedAtDesc(ChecklistTemplateStatus status);

    List<ChecklistTemplate> findByStageAndStatusOrderByUpdatedAtDesc(
            ContentStage stage, ChecklistTemplateStatus status);

    @Query("select t from ChecklistTemplate t where (:stage is null or t.stage=:stage) " +
            "and (:status is null or t.status=:status) " +
            "and (:keyword is null or lower(t.name) like lower(concat('%', cast(:keyword as string), '%')) " +
            "or lower(t.description) like lower(concat('%', cast(:keyword as string), '%'))) " +
            "order by t.updatedAt desc nulls last, t.id desc")
    Page<ChecklistTemplate> findAdminByOptionalStageAndStatus(
            @Param("stage") ContentStage stage,
            @Param("status") ChecklistTemplateStatus status,
            @Param("keyword") String keyword,
            Pageable pageable);
}
