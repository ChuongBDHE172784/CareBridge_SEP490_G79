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
            INSERT INTO preparation_checklist_items (
                checklist_item_id, owner_user_id, mother_journey_id, baby_id,
                template_entry_id, title, category, status, display_order,
                created_at, updated_at
            ) VALUES (
                :id, :ownerUserId, NULL, :babyId,
                :templateItemId, :itemText, 'GENERAL', 'OPEN', :itemOrder,
                now(), now()
            )
            ON CONFLICT (owner_user_id, baby_id, template_entry_id)
            WHERE baby_id IS NOT NULL AND template_entry_id IS NOT NULL
            DO NOTHING
            """, nativeQuery = true)
    int insertBabyImportedIfAbsent(
            @Param("id") UUID id,
            @Param("ownerUserId") UUID ownerUserId,
            @Param("babyId") UUID babyId,
            @Param("templateItemId") UUID templateItemId,
            @Param("itemText") String itemText,
            @Param("itemOrder") int itemOrder);

    @Query(value = """
            SELECT imported.*
              FROM preparation_checklist_items imported
             WHERE imported.owner_user_id = :ownerUserId
               AND imported.baby_id = :babyId
               AND imported.template_entry_id = :templateItemId
             ORDER BY imported.created_at, imported.checklist_item_id
             LIMIT 1
            """, nativeQuery = true)
    Optional<UserChecklistItem> findBabyImportedByExactScope(
            @Param("ownerUserId") UUID ownerUserId,
            @Param("babyId") UUID babyId,
            @Param("templateItemId") UUID templateItemId);

    @Modifying
    @Query(value = """
            INSERT INTO preparation_checklist_items (
                checklist_item_id, owner_user_id, mother_journey_id, baby_id,
                template_entry_id, title, category, status, display_order,
                created_at, updated_at
            ) VALUES (
                :id, :ownerUserId, :journeyId, NULL,
                :templateItemId, :itemText, 'GENERAL', 'OPEN', :itemOrder,
                now(), now()
            )
            ON CONFLICT (owner_user_id, mother_journey_id, template_entry_id)
            WHERE baby_id IS NULL AND template_entry_id IS NOT NULL
            DO NOTHING
            """, nativeQuery = true)
    int insertJourneyImportedIfAbsent(
            @Param("id") UUID id,
            @Param("ownerUserId") UUID ownerUserId,
            @Param("journeyId") UUID journeyId,
            @Param("templateItemId") UUID templateItemId,
            @Param("itemText") String itemText,
            @Param("itemOrder") int itemOrder);

    @Query(value = """
            SELECT imported.*
              FROM preparation_checklist_items imported
             WHERE imported.owner_user_id = :ownerUserId
               AND imported.baby_id IS NULL
               AND imported.mother_journey_id IS NOT DISTINCT FROM :journeyId
               AND imported.template_entry_id = :templateItemId
             ORDER BY imported.created_at, imported.checklist_item_id
             LIMIT 1
            """, nativeQuery = true)
    Optional<UserChecklistItem> findJourneyImportedByExactScope(
            @Param("ownerUserId") UUID ownerUserId,
            @Param("journeyId") UUID journeyId,
            @Param("templateItemId") UUID templateItemId);

    /** Compatibility entry point; production imports use the explicit scope methods above. */
    default int insertImportedIfAbsent(
            UUID id,
            UUID ownerUserId,
            UUID journeyId,
            UUID babyId,
            UUID templateItemId,
            String itemText,
            int itemOrder) {
        return babyId != null
                ? insertBabyImportedIfAbsent(
                        id, ownerUserId, babyId, templateItemId, itemText, itemOrder)
                : insertJourneyImportedIfAbsent(
                        id, ownerUserId, journeyId, templateItemId, itemText, itemOrder);
    }

    /** Compatibility entry point; production imports use the explicit scope methods above. */
    default Optional<UserChecklistItem> findImportedByExactScope(
            UUID ownerUserId, UUID journeyId, UUID babyId, UUID templateItemId) {
        return babyId != null
                ? findBabyImportedByExactScope(ownerUserId, babyId, templateItemId)
                : findJourneyImportedByExactScope(ownerUserId, journeyId, templateItemId);
    }
}
