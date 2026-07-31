package com.carebridge.backend.checklist.repository;

import com.carebridge.backend.checklist.entity.ChecklistInstance;
import com.carebridge.backend.checklist.model.ChecklistCareContextType;
import com.carebridge.backend.checklist.model.ChecklistOrigin;
import com.carebridge.backend.checklist.model.ChecklistRecipientRole;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
