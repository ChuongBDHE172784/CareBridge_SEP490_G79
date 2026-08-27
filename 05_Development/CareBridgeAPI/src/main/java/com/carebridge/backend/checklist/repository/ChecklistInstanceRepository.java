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

    @Query(value = """
            select instance from ChecklistInstance instance
             where instance.contextOwnerUserId = :ownerUserId
               and instance.recipientUserId = :ownerUserId
               and instance.recipientRole = com.carebridge.backend.checklist.model.ChecklistRecipientRole.MOTHER
               and instance.origin = com.carebridge.backend.checklist.model.ChecklistOrigin.SYSTEM_TEMPLATE
               and instance.careGroupId is null
               and instance.historicalAt is not null
               and instance.templateVersionId is not null
               and exists (
                    select template.id from ChecklistTemplate template
                     where template.templateVersionId = instance.templateVersionId
                       and template.status = com.carebridge.backend.content.entity.ChecklistTemplateStatus.APPROVED
                       and template.recipientScope in (
                           com.carebridge.backend.checklist.model.ChecklistRecipientScope.MOTHER,
                           com.carebridge.backend.checklist.model.ChecklistRecipientScope.BOTH)
               )
               and (:targetSubject is null or exists (
                    select task.id from ChecklistTaskInstance task
                     where task.checklistInstanceId = instance.id
                       and task.targetSubject = :targetSubject
               ))
             order by instance.historicalAt desc, instance.updatedAt desc, instance.id desc
            """,
            countQuery = """
    select count(instance) from ChecklistInstance instance
             where instance.contextOwnerUserId = :ownerUserId
               and instance.recipientUserId = :ownerUserId
               and instance.recipientRole = com.carebridge.backend.checklist.model.ChecklistRecipientRole.MOTHER
               and instance.origin = com.carebridge.backend.checklist.model.ChecklistOrigin.SYSTEM_TEMPLATE
               and instance.careGroupId is null
               and instance.historicalAt is not null
               and instance.templateVersionId is not null
               and exists (
                    select template.id from ChecklistTemplate template
                     where template.templateVersionId = instance.templateVersionId
                       and template.status = com.carebridge.backend.content.entity.ChecklistTemplateStatus.APPROVED
                       and template.recipientScope in (
                           com.carebridge.backend.checklist.model.ChecklistRecipientScope.MOTHER,
                           com.carebridge.backend.checklist.model.ChecklistRecipientScope.BOTH)
               )
               and (:targetSubject is null or exists (
                    select task.id from ChecklistTaskInstance task
                     where task.checklistInstanceId = instance.id
                       and task.targetSubject = :targetSubject
               ))
            """)
    Page<ChecklistInstance> findOwnerHistory(
            @Param("ownerUserId") UUID ownerUserId,
            @Param("targetSubject") ChecklistTargetSubject targetSubject,
            Pageable pageable);

    /** Historical mother rows visible through one explicit family group.  The
     * context predicates are repeated in the count query so pagination totals
     * cannot include another journey/baby or an archived template. */
    @Query(value = """
            select instance from ChecklistInstance instance
             where instance.contextOwnerUserId = :ownerUserId
               and instance.recipientUserId = :ownerUserId
               and instance.recipientRole = com.carebridge.backend.checklist.model.ChecklistRecipientRole.MOTHER
               and instance.origin = com.carebridge.backend.checklist.model.ChecklistOrigin.SYSTEM_TEMPLATE
               and instance.careGroupId is null
               and instance.historicalAt is not null
               and ((:journeyId is not null
                     and instance.careContextType = com.carebridge.backend.checklist.model.ChecklistCareContextType.JOURNEY
                     and instance.careContextId = :journeyId)
                    or (:babyId is not null
                     and instance.careContextType = com.carebridge.backend.checklist.model.ChecklistCareContextType.BABY
                     and instance.careContextId = :babyId))
               and instance.templateVersionId is not null
               and exists (
                    select template.id from ChecklistTemplate template
                     where template.templateVersionId = instance.templateVersionId
                       and template.status = com.carebridge.backend.content.entity.ChecklistTemplateStatus.APPROVED
                       and template.recipientScope in (
                           com.carebridge.backend.checklist.model.ChecklistRecipientScope.MOTHER,
                           com.carebridge.backend.checklist.model.ChecklistRecipientScope.BOTH)
               )
               and (:targetSubject is null or exists (
                    select task.id from ChecklistTaskInstance task
                     where task.checklistInstanceId = instance.id
                       and task.targetSubject = :targetSubject
               ))
             order by instance.historicalAt desc, instance.updatedAt desc, instance.id desc
            """,
            countQuery = """
            select count(instance) from ChecklistInstance instance
             where instance.contextOwnerUserId = :ownerUserId
               and instance.recipientUserId = :ownerUserId
               and instance.recipientRole = com.carebridge.backend.checklist.model.ChecklistRecipientRole.MOTHER
               and instance.origin = com.carebridge.backend.checklist.model.ChecklistOrigin.SYSTEM_TEMPLATE
               and instance.careGroupId is null
               and instance.historicalAt is not null
               and ((:journeyId is not null
                     and instance.careContextType = com.carebridge.backend.checklist.model.ChecklistCareContextType.JOURNEY
                     and instance.careContextId = :journeyId)
                    or (:babyId is not null
                     and instance.careContextType = com.carebridge.backend.checklist.model.ChecklistCareContextType.BABY
                     and instance.careContextId = :babyId))
               and instance.templateVersionId is not null
               and exists (
                    select template.id from ChecklistTemplate template
                     where template.templateVersionId = instance.templateVersionId
                       and template.status = com.carebridge.backend.content.entity.ChecklistTemplateStatus.APPROVED
                       and template.recipientScope in (
                           com.carebridge.backend.checklist.model.ChecklistRecipientScope.MOTHER,
                           com.carebridge.backend.checklist.model.ChecklistRecipientScope.BOTH)
               )
               and (:targetSubject is null or exists (
                    select task.id from ChecklistTaskInstance task
                     where task.checklistInstanceId = instance.id
                       and task.targetSubject = :targetSubject
               ))
            """)
    Page<ChecklistInstance> findSharedHistory(
            @Param("ownerUserId") UUID ownerUserId,
            @Param("journeyId") UUID journeyId,
            @Param("babyId") UUID babyId,
            @Param("targetSubject") ChecklistTargetSubject targetSubject,
            Pageable pageable);

    /** Historical Family occurrences bound to the actor's current VIEW epoch. */
    @Query(value = """
            select instance from ChecklistInstance instance
             where instance.contextOwnerUserId = :ownerUserId
               and instance.recipientUserId = :actorUserId
               and instance.recipientRole = com.carebridge.backend.checklist.model.ChecklistRecipientRole.FAMILY
               and instance.origin = com.carebridge.backend.checklist.model.ChecklistOrigin.SYSTEM_TEMPLATE
               and instance.careGroupId = :careGroupId
               and instance.careGroupMemberId is not null
               and instance.checklistAccessEpoch = (
                    select member.checklistAccessEpoch from
                        com.carebridge.backend.family.entity.CareGroupMember member
                     where member.id = instance.careGroupMemberId
                       and member.careGroupId = :careGroupId
                       and member.userId = :actorUserId
                       and member.inviteStatus =
                           com.carebridge.backend.family.entity.InviteStatus.ACCEPTED
               )
               and instance.historicalAt is not null
               and ((:journeyId is not null
                     and instance.careContextType = com.carebridge.backend.checklist.model.ChecklistCareContextType.JOURNEY
                     and instance.careContextId = :journeyId)
                    or (:babyId is not null
                     and instance.careContextType = com.carebridge.backend.checklist.model.ChecklistCareContextType.BABY
                     and instance.careContextId = :babyId))
               and instance.templateVersionId is not null
               and exists (
                    select template.id from ChecklistTemplate template
                     where template.templateVersionId = instance.templateVersionId
                       and template.status = com.carebridge.backend.content.entity.ChecklistTemplateStatus.APPROVED
                       and template.recipientScope in (
                           com.carebridge.backend.checklist.model.ChecklistRecipientScope.FAMILY,
                           com.carebridge.backend.checklist.model.ChecklistRecipientScope.BOTH)
               )
               and (:targetSubject is null or exists (
                    select task.id from ChecklistTaskInstance task
                     where task.checklistInstanceId = instance.id
                       and task.targetSubject = :targetSubject
               ))
             order by instance.historicalAt desc, instance.updatedAt desc, instance.id desc
            """,
            countQuery = """
            select count(instance) from ChecklistInstance instance
             where instance.contextOwnerUserId = :ownerUserId
               and instance.recipientUserId = :actorUserId
               and instance.recipientRole = com.carebridge.backend.checklist.model.ChecklistRecipientRole.FAMILY
               and instance.origin = com.carebridge.backend.checklist.model.ChecklistOrigin.SYSTEM_TEMPLATE
               and instance.careGroupId = :careGroupId
               and instance.careGroupMemberId is not null
               and instance.checklistAccessEpoch = (
                    select member.checklistAccessEpoch from
                        com.carebridge.backend.family.entity.CareGroupMember member
                     where member.id = instance.careGroupMemberId
                       and member.careGroupId = :careGroupId
                       and member.userId = :actorUserId
                       and member.inviteStatus =
                           com.carebridge.backend.family.entity.InviteStatus.ACCEPTED
               )
               and instance.historicalAt is not null
               and ((:journeyId is not null
                     and instance.careContextType = com.carebridge.backend.checklist.model.ChecklistCareContextType.JOURNEY
                     and instance.careContextId = :journeyId)
                    or (:babyId is not null
                     and instance.careContextType = com.carebridge.backend.checklist.model.ChecklistCareContextType.BABY
                     and instance.careContextId = :babyId))
               and instance.templateVersionId is not null
               and exists (
                    select template.id from ChecklistTemplate template
                     where template.templateVersionId = instance.templateVersionId
                       and template.status = com.carebridge.backend.content.entity.ChecklistTemplateStatus.APPROVED
                       and template.recipientScope in (
                           com.carebridge.backend.checklist.model.ChecklistRecipientScope.FAMILY,
                           com.carebridge.backend.checklist.model.ChecklistRecipientScope.BOTH)
               )
               and (:targetSubject is null or exists (
                    select task.id from ChecklistTaskInstance task
                     where task.checklistInstanceId = instance.id
                       and task.targetSubject = :targetSubject
               ))
            """)
    Page<ChecklistInstance> findFamilyHistory(
            @Param("actorUserId") UUID actorUserId,
            @Param("careGroupId") UUID careGroupId,
            @Param("ownerUserId") UUID ownerUserId,
            @Param("journeyId") UUID journeyId,
            @Param("babyId") UUID babyId,
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
