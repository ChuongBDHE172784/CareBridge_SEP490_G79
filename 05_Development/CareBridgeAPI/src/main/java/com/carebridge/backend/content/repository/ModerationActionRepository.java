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
    Page<ModerationAction> findByTargetTypeInOrderByActionAtDesc(
            Collection<ReportTargetType> targetTypes, Pageable pageable);

    // Dev seed idempotency (DevDataSeeder) — a given target only gets one seeded action of each type
    boolean existsByTargetIdAndActionType(UUID targetId, ModerationActionType actionType);

    // CB-MOD-IMP-009 ADR-002 (guard 1 — "most recent action"): used to reject undoing an action that
    // has since been superseded by a newer one on the same target.
    Optional<ModerationAction> findTopByTargetIdAndTargetTypeOrderByActionAtDesc(
            UUID targetId, ReportTargetType targetType);

    Page<ModerationAction> findByActionTypeOrderByActionAtDesc(ModerationActionType actionType, Pageable pageable);
}
