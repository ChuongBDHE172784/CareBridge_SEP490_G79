package com.carebridge.backend.content.repository;

import com.carebridge.backend.content.entity.ModerationAction;
import com.carebridge.backend.content.entity.ModerationActionType;
import com.carebridge.backend.content.entity.ReportTargetType;
import java.util.Collection;
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
    @Query("""
            select m from ModerationAction m
            where m.targetType in :targetTypes
            order by m.actionAt desc, m.id desc
            """)
    Page<ModerationAction> findByTargetTypeInOrderByActionAtDesc(
            @Param("targetTypes") Collection<ReportTargetType> targetTypes, Pageable pageable);

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
    Optional<ModerationAction> findTopByTargetIdAndTargetTypeOrderByActionAtDesc(
            UUID targetId, ReportTargetType targetType);

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

    // CB-MOD-IMP-015 (revertReport): finds the ModerationAction created when a report was resolved
    // (reportId != null). Returns Optional.empty() for a report resolved via DISMISS, which creates
    // no ModerationAction (BR-MOD-010).
    Optional<ModerationAction> findTopByReportIdOrderByActionAtDesc(UUID reportId);

    private static String category(ModerationActionType actionType) {
        return "MODERATION_" + actionType.name();
    }

    private static Collection<String> categories(Collection<ModerationActionType> actionTypes) {
        return actionTypes.stream().map(ModerationActionRepository::category).toList();
    }
}
