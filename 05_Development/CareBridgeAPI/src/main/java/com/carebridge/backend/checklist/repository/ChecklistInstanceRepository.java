package com.carebridge.backend.checklist.repository;

import com.carebridge.backend.checklist.entity.ChecklistInstance;
import com.carebridge.backend.checklist.model.ChecklistCareContextType;
import com.carebridge.backend.checklist.model.ChecklistOrigin;
import com.carebridge.backend.checklist.model.ChecklistRecipientRole;
import com.carebridge.backend.checklist.model.ChecklistTargetSubject;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChecklistInstanceRepository extends JpaRepository<ChecklistInstance, UUID> {
    @Query(value = "SELECT TRUE FROM pg_advisory_xact_lock(hashtextextended(CAST(:key AS text), 0))",
            nativeQuery = true)
    Boolean acquireDistributionKeyLock(@Param("key") String key);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select instance from ChecklistInstance instance where instance.id = :id")
    Optional<ChecklistInstance> findForUpdateById(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ChecklistInstance> findByDistributionKey(String distributionKey);

    List<ChecklistInstance> findByRecipientUserId(UUID recipientUserId);

    boolean existsByTemplateLineageId(UUID templateLineageId);

    List<ChecklistInstance> findByRecipientUserIdAndHistoricalAtIsNull(UUID recipientUserId);

    List<ChecklistInstance> findByContextOwnerUserIdAndRecipientRoleAndOriginAndHistoricalAtIsNull(
            UUID contextOwnerUserId,
            ChecklistRecipientRole recipientRole,
            ChecklistOrigin origin);

    @Query("""
            select instance from ChecklistInstance instance
             where instance.contextOwnerUserId = :ownerUserId
               and instance.recipientUserId = :ownerUserId
               and instance.recipientRole = com.carebridge.backend.checklist.model.ChecklistRecipientRole.MOTHER
               and instance.origin = com.carebridge.backend.checklist.model.ChecklistOrigin.SYSTEM_TEMPLATE
               and instance.historicalAt is not null
               and (:targetSubject is null or exists (
                    select task.id from ChecklistTaskInstance task
                     where task.checklistInstanceId = instance.id
                       and task.targetSubject = :targetSubject
               ))
             order by instance.historicalAt desc, instance.updatedAt desc, instance.id desc
            """)
    Page<ChecklistInstance> findOwnerHistory(
            @Param("ownerUserId") UUID ownerUserId,
            @Param("targetSubject") ChecklistTargetSubject targetSubject,
            Pageable pageable);

    List<ChecklistInstance>
            findAllByRecipientUserIdAndRecipientRoleAndCareGroupIdAndCareContextTypeAndCareContextIdAndTemplateVersionId(
                    UUID recipientUserId,
                    ChecklistRecipientRole recipientRole,
                    UUID careGroupId,
                    ChecklistCareContextType careContextType,
                    UUID careContextId,
                    UUID templateVersionId);

    @Query("""
            select instance from ChecklistInstance instance
             where instance.recipientUserId = :recipientUserId
               and instance.recipientRole = :recipientRole
               and instance.careContextType = :careContextType
               and instance.careContextId = :careContextId
               and instance.origin = :origin
               and ((:templateVersionId is null and instance.templateVersionId is null)
                    or instance.templateVersionId = :templateVersionId)
            """)
    List<ChecklistInstance> findAllByLogicalPersonalIdentity(
            @Param("recipientUserId") UUID recipientUserId,
            @Param("recipientRole") ChecklistRecipientRole recipientRole,
            @Param("careContextType") ChecklistCareContextType careContextType,
            @Param("careContextId") UUID careContextId,
            @Param("templateVersionId") UUID templateVersionId,
            @Param("origin") ChecklistOrigin origin);
}
