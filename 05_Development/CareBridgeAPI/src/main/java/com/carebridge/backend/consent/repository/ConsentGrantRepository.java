package com.carebridge.backend.consent.repository;

import com.carebridge.backend.consent.entity.ConsentDataType;
import com.carebridge.backend.consent.entity.ConsentGrant;
import com.carebridge.backend.consent.entity.ConsentPurpose;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ConsentGrantRepository extends JpaRepository<ConsentGrant, Long> {

    @Query("""
            select c from ConsentGrant c
             where c.userId = :userId
               and c.dataType = com.carebridge.backend.consent.entity.ConsentDataType.SENSITIVE_DATA
               and c.purpose = com.carebridge.backend.consent.entity.ConsentPurpose.PERSONALIZE
               and c.scope = :scope
               and c.revokedAt is null
               and c.status = 'ACTIVE'
             order by c.consentGivenAt desc, c.id desc
            """)
    List<ConsentGrant> findRecommendationGrants(
            @Param("userId") UUID userId, @Param("scope") String scope);

    @Query("""
            select c from ConsentGrant c
             where c.userId = :userId
               and c.dataType = com.carebridge.backend.consent.entity.ConsentDataType.SENSITIVE_DATA
               and c.purpose = com.carebridge.backend.consent.entity.ConsentPurpose.PERSONALIZE
               and c.scope = :scope
             order by c.consentGivenAt desc, c.id desc
            """)
    List<ConsentGrant> findLatestRecommendationGrant(
            @Param("userId") UUID userId, @Param("scope") String scope,
            org.springframework.data.domain.Pageable pageable);

    @Query("""
            select c from ConsentGrant c
             where c.userId = :userId
               and c.evidenceKey = :evidenceKey
               and c.dataType = com.carebridge.backend.consent.entity.ConsentDataType.SENSITIVE_DATA
               and c.purpose = com.carebridge.backend.consent.entity.ConsentPurpose.PERSONALIZE
               and c.scope = :scope
            """)
    Optional<ConsentGrant> findRecommendationGrantByEvidence(
            @Param("userId") UUID userId,
            @Param("evidenceKey") UUID evidenceKey,
            @Param("scope") String scope);

    @Query(value = """
            SELECT 1
            FROM pg_advisory_xact_lock(
                hashtextextended(CAST(:userId AS text), 0))
            """, nativeQuery = true)
    Integer acquireLifecycleOwnerLock(@Param("userId") java.util.UUID userId);

    List<ConsentGrant> findByUserIdOrderByConsentGivenAtDesc(java.util.UUID userId);

    Optional<ConsentGrant> findByIdAndUserId(Long id, java.util.UUID userId);

    @Query("""
            select c from ConsentGrant c
            where c.userId = :userId
              and c.evidenceKey = :evidenceKey
              and c.dataType = com.carebridge.backend.consent.entity.ConsentDataType.MOTHER_BASELINE
              and c.purpose = com.carebridge.backend.consent.entity.ConsentPurpose.PERSONALIZE
            """)
    Optional<ConsentGrant> findLifecycleEvidenceByKey(
            @Param("userId") java.util.UUID userId,
            @Param("evidenceKey") java.util.UUID evidenceKey);

    @Query("""
            select c from ConsentGrant c
            where c.userId = :userId
              and c.dataType = com.carebridge.backend.consent.entity.ConsentDataType.MOTHER_BASELINE
              and c.purpose = com.carebridge.backend.consent.entity.ConsentPurpose.PERSONALIZE
              and c.policyVersion = :policyVersion
              and c.scope = :scope
              and c.revokedAt is null
              and c.expiryAt > :now
            order by c.consentGivenAt desc
            """)
    List<ConsentGrant> findValidLifecycleEvidence(
            @Param("userId") java.util.UUID userId,
            @Param("policyVersion") String policyVersion,
            @Param("scope") String scope,
            @Param("now") Instant now);

    @Query("""
            select count(c) > 0
            from ConsentGrant c
            where c.userId = :userId
              and c.evidenceKey is not null
              and c.policyVersion is not null
            """)
    boolean existsLifecycleEvidence(@Param("userId") java.util.UUID userId);

    @Query("""
            select count(c) > 0
            from ConsentGrant c
            where c.userId = :userId
              and c.dataType = :dataType
              and c.purpose = :purpose
              and c.revokedAt is null
              and c.expiryAt > :now
            """)
    boolean existsValidConsent(
            @Param("userId") java.util.UUID userId,
            @Param("dataType") ConsentDataType dataType,
            @Param("purpose") ConsentPurpose purpose,
            @Param("now") Instant now);

    @Query(value = """
            SELECT EXISTS (
                SELECT 1
                  FROM public.data_permissions permission
                 WHERE permission.permission_id = :permissionId
                   AND permission.owner_user_id = :userId
                   AND permission.permission_kind = 'CONSENT_GRANT'
                   AND permission.scope_type = :dataType
                   AND permission.purpose = :purpose
                   AND permission.status = 'ACTIVE'
                   AND permission.granted_at IS NOT NULL
                   AND permission.granted_at <= :now
                   AND permission.revoked_at IS NULL
                   AND permission.expires_at >= :requiredUntil
            )
            """, nativeQuery = true)
    boolean existsValidConsentByPermissionIdCoveringInterval(
            @Param("permissionId") java.util.UUID permissionId,
            @Param("userId") java.util.UUID userId,
            @Param("dataType") String dataType,
            @Param("purpose") String purpose,
            @Param("now") Instant now,
            @Param("requiredUntil") Instant requiredUntil);
}
