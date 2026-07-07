package com.carebridge.backend.health.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

// Minimal read-only mapping for UC44 consent gate.
// data_permissions is owned by the privacy/consent domain.
@Entity
@Table(name = "data_permissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DataPermission {

    @Id
    @Column(name = "permission_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "owner_user_id")
    private UUID ownerUserId;

    @Column(name = "grantee_user_id")
    private UUID granteeUserId;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
