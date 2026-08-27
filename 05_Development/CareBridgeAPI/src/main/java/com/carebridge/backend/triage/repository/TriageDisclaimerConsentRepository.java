package com.carebridge.backend.triage.repository;

import com.carebridge.backend.triage.entity.TriageDisclaimerConsent;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * CB-TRIAGE-CONSENT-IMP-001 §8.2 — persistence only, no business decisions.
 *
 * <p>The entity carries {@code @SQLRestriction("permission_kind = 'AI_TRIAGE_DISCLAIMER'")} so
 * every derived query below is automatically fenced to this kind (same pattern as
 * {@code ConsentGrant}). NO delete methods — append-style lifecycle (BR-TDC-005); status
 * transitions happen via entity save only.
 */
@Repository
public interface TriageDisclaimerConsentRepository
        extends JpaRepository<TriageDisclaimerConsent, UUID> {

    /**
     * Serializes accept/revoke per user — ADR-TDC-004
     * (pattern: {@code ConsentGrantRepository.acquireLifecycleOwnerLock}; the suffix keys this
     * lock away from the lifecycle-consent lock space). PostgreSQL-only by design.
     */
    @Query(value = """
            SELECT 1 FROM pg_advisory_xact_lock(
                hashtextextended(CAST(:userId AS text) || ':AI_TRIAGE_DISCLAIMER', 0))
            """, nativeQuery = true)
    Integer acquireDisclaimerConsentLock(@Param("userId") UUID userId);

    boolean existsByOwnerUserIdAndPolicyVersionAndStatus(UUID ownerUserId, String policyVersion, String status);

    Optional<TriageDisclaimerConsent> findFirstByOwnerUserIdAndPolicyVersionAndStatus(
            UUID ownerUserId, String policyVersion, String status);

    Optional<TriageDisclaimerConsent> findFirstByOwnerUserIdAndStatusOrderByGrantedAtDesc(
            UUID ownerUserId, String status);

    Optional<TriageDisclaimerConsent> findFirstByOwnerUserIdOrderByGrantedAtDesc(UUID ownerUserId);
}
