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
            ) SELECT
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
            WHERE NOT EXISTS (
                SELECT 1
                FROM user_checklist_items existing
                WHERE existing.owner_user_id = :ownerUserId
                  AND existing.template_item_id = :templateItemId
                  AND (
                      (:babyId IS NOT NULL AND existing.baby_id = :babyId)
                      OR (
                          :babyId IS NULL
                          AND existing.baby_id IS NULL
                          AND existing.journey_id IS NOT DISTINCT FROM :journeyId
                      )
                  )
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
            SELECT imported.*
            FROM user_checklist_items imported
            WHERE imported.owner_user_id = :ownerUserId
              AND imported.template_item_id = :templateItemId
              AND (
                  (:babyId IS NOT NULL AND imported.baby_id = :babyId)
                  OR (
                      :babyId IS NULL
                      AND imported.baby_id IS NULL
                      AND imported.journey_id IS NOT DISTINCT FROM :journeyId
                  )
              )
            ORDER BY
                CASE WHEN imported.journey_id IS NULL THEN 0 ELSE 1 END,
                imported.created_at,
                imported.user_checklist_item_id
            LIMIT 1
            """, nativeQuery = true)
    Optional<UserChecklistItem> findImportedByExactScope(
            @Param("ownerUserId") UUID ownerUserId,
            @Param("journeyId") UUID journeyId,
            @Param("babyId") UUID babyId,
            @Param("templateItemId") UUID templateItemId);
}
