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

    List<ConsentGrant> findByUserIdOrderByConsentGivenAtDesc(UUID userId);

    Optional<ConsentGrant> findByIdAndUserId(Long id, UUID userId);

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
            @Param("userId") UUID userId,
            @Param("dataType") ConsentDataType dataType,
            @Param("purpose") ConsentPurpose purpose,
            @Param("now") Instant now);
}
