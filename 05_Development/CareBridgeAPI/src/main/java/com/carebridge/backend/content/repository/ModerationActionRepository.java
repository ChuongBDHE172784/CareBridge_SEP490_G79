package com.carebridge.backend.content.repository;

import com.carebridge.backend.content.entity.ModerationAction;
import com.carebridge.backend.content.entity.ModerationActionType;
import com.carebridge.backend.content.entity.ReportTargetType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ModerationActionRepository extends JpaRepository<ModerationAction, UUID> {

    // CB-MOD-IMP-004 §16 (Moderation History extension): read back APPROVE/HIDE/LOCK actions on
    // content targets (QUESTION/ANSWER) — excludes ACCOUNT actions (belongs to a separate
    // account-violation history view, not this content-moderation history)
    // CB-MOD-IMP-017: AI_FEEDBACK_SUBMITTED history events are excluded from the content
    // moderation history feed — they never mutate content state.
    default Page<ModerationAction> findByTargetTypeInAndActionTypeNotOrderByActionAtDesc(
            Collection<ReportTargetType> targetTypes, ModerationActionType excludedType, Pageable pageable) {
        return findByTargetTypeInAndEventCategoryNotOrderByActionAtDesc(
                targetTypes, category(excludedType), pageable);
    }

    @Query("""
            select m from ModerationAction m
            where m.targetType in :targetTypes and m.eventCategory <> :excludedCategory
            order by m.actionAt desc, m.id desc
            """)
    Page<ModerationAction> findByTargetTypeInAndEventCategoryNotOrderByActionAtDesc(
            @Param("targetTypes") Collection<ReportTargetType> targetTypes,
            @Param("excludedCategory") String excludedCategory,
            Pageable pageable);

    default Page<ModerationAction> findByTargetTypeAndActionTypeInOrderByActionAtDesc(
            ReportTargetType targetType, Collection<ModerationActionType> actionTypes, Pageable pageable) {
        return findByTargetTypeAndEventCategoryInOrderByActionAtDesc(
                targetType, categories(actionTypes), pageable);
    }

    @Query("""
            select m from ModerationAction m
            where m.targetType = :targetType and m.eventCategory in :eventCategories
            order by m.actionAt desc, m.id desc
            """)
    Page<ModerationAction> findByTargetTypeAndEventCategoryInOrderByActionAtDesc(
            @Param("targetType") ReportTargetType targetType,
            @Param("eventCategories") Collection<String> eventCategories,
            Pageable pageable);

    default Page<UUID> findDistinctAccountTargetIds(
            Collection<ModerationActionType> actionTypes, Pageable pageable) {
        return findDistinctTargetIdsByTargetTypeAndEventCategoryIn(
                ReportTargetType.ACCOUNT, categories(actionTypes), pageable);
    }

    @Query(value = """
            select m.targetId from ModerationAction m
            where m.targetType = :targetType and m.eventCategory in :eventCategories
            group by m.targetId
            order by max(m.actionAt) desc, m.targetId desc
            """, countQuery = """
            select count(distinct m.targetId) from ModerationAction m
            where m.targetType = :targetType and m.eventCategory in :eventCategories
            """)
    Page<UUID> findDistinctTargetIdsByTargetTypeAndEventCategoryIn(
            @Param("targetType") ReportTargetType targetType,
            @Param("eventCategories") Collection<String> eventCategories,
            Pageable pageable);

    default List<ModerationAction> findAccountActionsByTargetIds(
            Collection<UUID> targetIds, Collection<ModerationActionType> actionTypes) {
        return findByTargetIdInAndTargetTypeAndEventCategoryInOrderByActionAtDesc(
                targetIds, ReportTargetType.ACCOUNT, categories(actionTypes));
    }

    @Query("""
            select m from ModerationAction m
            where m.targetId in :targetIds and m.targetType = :targetType
              and m.eventCategory in :eventCategories
            order by m.actionAt desc, m.id desc
            """)
    List<ModerationAction> findByTargetIdInAndTargetTypeAndEventCategoryInOrderByActionAtDesc(
            @Param("targetIds") Collection<UUID> targetIds,
            @Param("targetType") ReportTargetType targetType,
            @Param("eventCategories") Collection<String> eventCategories);

    default Page<ModerationAction> findAccountActionsByTargetId(
            UUID targetId, Collection<ModerationActionType> actionTypes, Pageable pageable) {
        return findByTargetIdAndTargetTypeAndEventCategoryInOrderByActionAtDesc(
                targetId, ReportTargetType.ACCOUNT, categories(actionTypes), pageable);
    }

    @Query("""
            select m from ModerationAction m
            where m.targetId = :targetId and m.targetType = :targetType
              and m.eventCategory in :eventCategories
            order by m.actionAt desc, m.id desc
            """)
    Page<ModerationAction> findByTargetIdAndTargetTypeAndEventCategoryInOrderByActionAtDesc(
            @Param("targetId") UUID targetId,
            @Param("targetType") ReportTargetType targetType,
            @Param("eventCategories") Collection<String> eventCategories,
            Pageable pageable);

    // Dev seed idempotency (DevDataSeeder) — a given target only gets one seeded action of each type
    default boolean existsByTargetIdAndActionType(UUID targetId, ModerationActionType actionType) {
        return existsByTargetIdAndEventCategory(targetId, category(actionType));
    }

    @Query("""
            select (count(m) > 0) from ModerationAction m
            where m.targetId = :targetId and m.eventCategory = :eventCategory
            """)
    boolean existsByTargetIdAndEventCategory(
            @Param("targetId") UUID targetId, @Param("eventCategory") String eventCategory);

    // CB-MOD-IMP-009 ADR-002 (guard 1 — "most recent action"): used to reject undoing an action that
    // has since been superseded by a newer one on the same target.
    // CB-MOD-IMP-017: AI feedback events share the target's id/type but never mutate content
    // state — they must not count as the "most recent action" for direct-action undo guards.
    default Optional<ModerationAction> findTopByTargetIdAndTargetTypeAndActionTypeNotOrderByActionAtDesc(
            UUID targetId, ReportTargetType targetType, ModerationActionType excludedType) {
        return findTopByTargetIdAndTargetTypeAndEventCategoryNot(
                targetId, targetType, category(excludedType));
    }

    @Query("""
            select m from ModerationAction m
            where m.targetId = :targetId and m.targetType = :targetType
              and m.eventCategory <> :excludedCategory
            order by m.actionAt desc, m.id desc
            limit 1
            """)
    Optional<ModerationAction> findTopByTargetIdAndTargetTypeAndEventCategoryNot(
            @Param("targetId") UUID targetId,
            @Param("targetType") ReportTargetType targetType,
            @Param("excludedCategory") String excludedCategory);

    default Page<ModerationAction> findByActionTypeOrderByActionAtDesc(
            ModerationActionType actionType, Pageable pageable) {
        return findByEventCategoryOrderByActionAtDesc(category(actionType), pageable);
    }

    @Query("""
            select m from ModerationAction m
            where m.eventCategory = :eventCategory
            order by m.actionAt desc, m.id desc
            """)
    Page<ModerationAction> findByEventCategoryOrderByActionAtDesc(
            @Param("eventCategory") String eventCategory, Pageable pageable);

    private static String category(ModerationActionType actionType) {
        return "MODERATION_" + actionType.name();
    }

    private static Collection<String> categories(Collection<ModerationActionType> actionTypes) {
        return actionTypes.stream().map(ModerationActionRepository::category).toList();
    }
}
