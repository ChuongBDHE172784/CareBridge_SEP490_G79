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
import org.springframework.stereotype.Repository;

@Repository
public interface ModerationActionRepository extends JpaRepository<ModerationAction, UUID> {

    // CB-MOD-IMP-004 §16 (Moderation History extension): read back APPROVE/HIDE/LOCK actions on
    // content targets (QUESTION/ANSWER) — excludes ACCOUNT actions (belongs to a separate
    // account-violation history view, not this content-moderation history)
    // CB-MOD-IMP-017: AI_FEEDBACK_SUBMITTED history events are excluded from the content
    // moderation history feed — they never mutate content state.
    Page<ModerationAction> findByTargetTypeInAndActionTypeNotOrderByActionAtDesc(
            Collection<ReportTargetType> targetTypes, ModerationActionType excludedType, Pageable pageable);

    Page<ModerationAction> findByTargetTypeAndActionTypeInOrderByActionAtDesc(
            ReportTargetType targetType, Collection<ModerationActionType> actionTypes, Pageable pageable);

    // Dev seed idempotency (DevDataSeeder) — a given target only gets one seeded action of each type
    boolean existsByTargetIdAndActionType(UUID targetId, ModerationActionType actionType);

    // CB-MOD-IMP-009 ADR-002 (guard 1 — "most recent action"): used to reject undoing an action that
    // has since been superseded by a newer one on the same target.
    // CB-MOD-IMP-017: AI feedback events share the target's id/type but never mutate content
    // state — they must not count as the "most recent action" for undo/revert guards.
    Optional<ModerationAction> findTopByTargetIdAndTargetTypeAndActionTypeNotOrderByActionAtDesc(
            UUID targetId, ReportTargetType targetType, ModerationActionType excludedType);

    Page<ModerationAction> findByActionTypeOrderByActionAtDesc(ModerationActionType actionType, Pageable pageable);

    // CB-MOD-IMP-015 (revertReport): finds the ModerationAction created when a report was resolved
    // (reportId != null). Returns Optional.empty() for a report resolved via DISMISS, which creates
    // no ModerationAction (BR-MOD-010).
    // CB-MOD-IMP-017: same exclusion — a feedback event on the case must not shadow the
    // content action that revertReport() needs to undo.
    Optional<ModerationAction> findTopByReportIdAndActionTypeNotOrderByActionAtDesc(
            UUID reportId, ModerationActionType excludedType);
}
