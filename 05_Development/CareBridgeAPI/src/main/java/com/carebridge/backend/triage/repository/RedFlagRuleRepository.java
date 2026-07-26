package com.carebridge.backend.triage.repository;

import com.carebridge.backend.triage.entity.RedFlagRule;
import com.carebridge.backend.triage.entity.RedFlagSeverity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RedFlagRuleRepository extends JpaRepository<RedFlagRule, UUID> {

    /**
     * Used by TriageRedFlagPolicy.isRedFlag() — the runtime safety read-path (ADR-001/ADR-004).
     * Must remain a simple, low-latency query (no joins) since it is on the AI/RAG hot path.
     * Named "Active" (not "IsActive") to match the JPA/JavaBean property derived from the entity's
     * {@code isActive()} getter — verified: Spring Data property-path resolution rejects "isActive"
     * as a literal segment name for a boolean property exposed via an {@code isXxx()} getter.
     */
    List<RedFlagRule> findBySeverityAndActiveTrue(RedFlagSeverity severity);

    boolean existsByKeywordIgnoreCase(String keyword);

    Page<RedFlagRule> findBySeverityAndActive(RedFlagSeverity severity, Boolean active, Pageable pageable);

    /**
     * Single query for the intake pre-screen (CB-TRIAGE-IMP-003 §8.2 — all severities needed:
     * RED for short-circuit, YELLOW/RED+WARN for annotation — ADR-002/ADR-004, read-through,
     * no cache). Property name is "Active" (not "IsActive") — same naming constraint as
     * {@link #findBySeverityAndActiveTrue(RedFlagSeverity)} above.
     */
    List<RedFlagRule> findByActiveTrue();
}
