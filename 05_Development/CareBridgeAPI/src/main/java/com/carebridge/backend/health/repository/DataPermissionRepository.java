package com.carebridge.backend.health.repository;

import com.carebridge.backend.health.entity.DataPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface DataPermissionRepository extends JpaRepository<DataPermission, UUID> {

    @Query("""
            SELECT COUNT(p) > 0 FROM DataPermission p
            WHERE p.ownerUserId = :ownerUserId
              AND p.granteeUserId = :granteeUserId
              AND p.status = 'ACTIVE'
              AND (p.expiresAt IS NULL OR p.expiresAt > :now)
            """)
    boolean existsValidPermission(
            @Param("ownerUserId") UUID ownerUserId,
            @Param("granteeUserId") UUID granteeUserId,
            @Param("now") Instant now);
}
