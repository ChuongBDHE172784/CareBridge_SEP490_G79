package com.carebridge.backend.content.repository;

import com.carebridge.backend.content.entity.ModerationAction;
import com.carebridge.backend.content.entity.ReportTargetType;
import java.util.Collection;
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
}
