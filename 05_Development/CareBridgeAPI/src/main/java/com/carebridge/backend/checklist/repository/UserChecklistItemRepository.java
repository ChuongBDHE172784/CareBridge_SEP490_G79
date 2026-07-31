package com.carebridge.backend.checklist.repository;

import com.carebridge.backend.checklist.entity.UserChecklistItem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Read-only compatibility repository. Legacy checklist writes were retired by CHK-025. */
public interface UserChecklistItemRepository extends Repository<UserChecklistItem, UUID> {
    Optional<UserChecklistItem> findById(UUID id);

    Optional<UserChecklistItem> findByIdAndOwnerUserId(UUID id, UUID ownerUserId);

    @Query("""
            SELECT i FROM UserChecklistItem i
            WHERE i.ownerUserId = :ownerUserId
              AND (:journeyId IS NULL OR i.journeyId = :journeyId)
              AND (:babyId IS NULL OR i.babyId = :babyId)
            ORDER BY i.itemOrder ASC, i.createdAt ASC
            """)
    List<UserChecklistItem> findByOwnerFiltered(
            @Param("ownerUserId") UUID ownerUserId,
            @Param("journeyId") UUID journeyId,
            @Param("babyId") UUID babyId);
}
