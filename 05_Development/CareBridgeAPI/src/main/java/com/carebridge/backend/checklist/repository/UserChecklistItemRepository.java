package com.carebridge.backend.checklist.repository;

import com.carebridge.backend.checklist.entity.UserChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserChecklistItemRepository extends JpaRepository<UserChecklistItem, UUID> {

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

    @Modifying
    @Query(value = """
            INSERT INTO user_checklist_items (
                user_checklist_item_id,
                owner_user_id,
                journey_id,
                baby_id,
                template_item_id,
                item_text,
                category,
                is_completed,
                item_order,
                created_at,
                updated_at
            ) VALUES (
                :id,
                :ownerUserId,
                :journeyId,
                :babyId,
                :templateItemId,
                :itemText,
                'GENERAL',
                false,
                :itemOrder,
                now(),
                now()
            )
            ON CONFLICT (
                owner_user_id,
                journey_id,
                baby_id,
                template_item_id
            ) WHERE template_item_id IS NOT NULL
            DO NOTHING
            """, nativeQuery = true)
    int insertImportedIfAbsent(
            @Param("id") UUID id,
            @Param("ownerUserId") UUID ownerUserId,
            @Param("journeyId") UUID journeyId,
            @Param("babyId") UUID babyId,
            @Param("templateItemId") UUID templateItemId,
            @Param("itemText") String itemText,
            @Param("itemOrder") int itemOrder);

    @Query(value = """
            SELECT *
            FROM user_checklist_items
            WHERE owner_user_id = :ownerUserId
              AND journey_id = :journeyId
              AND baby_id IS NOT DISTINCT FROM :babyId
              AND template_item_id = :templateItemId
            """, nativeQuery = true)
    Optional<UserChecklistItem> findImportedByExactScope(
            @Param("ownerUserId") UUID ownerUserId,
            @Param("journeyId") UUID journeyId,
            @Param("babyId") UUID babyId,
            @Param("templateItemId") UUID templateItemId);
}
