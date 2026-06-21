package com.carebridge.backend.identity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "data_permissions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "permission_id")
    private UUID permissionId;

    @Column(name = "owner_user_id")
    private UUID ownerUserId;

    @Column(name = "grantee_user_id")
    private UUID granteeUserId;

    @Column(name = "scope_type", length = 50)
    private String scopeType;

    @Column(name = "scope_reference_id")
    private UUID scopeReferenceId;

    @Column(name = "purpose", length = 255)
    private String purpose;

    @Column(name = "granted_at")
    private Instant grantedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
