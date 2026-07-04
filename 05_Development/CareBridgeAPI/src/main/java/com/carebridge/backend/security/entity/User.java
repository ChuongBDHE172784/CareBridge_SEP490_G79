package com.carebridge.backend.security.entity;

import com.carebridge.backend.security.rbac.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id", updatable = false, nullable = false)
    private java.util.UUID id;

    @Column(unique = true, length = 20)
    private String phone;

    @Column(length = 255)
    private String email;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "full_name", length = 120)
    private String name;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "account_status", length = 30)
    private String accountStatus;

    @Column(name = "email_verified")
    private Boolean emailVerified;

    @Column(name = "phone_verified")
    private Boolean phoneVerified;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 50, nullable = false)
    private Role role = Role.MOTHER;

    @Builder.Default
    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Builder.Default
    @Column(name = "locked", nullable = false)
    private boolean locked = false;

    @Column(name = "locked_at")
    private Instant lockedAt;

    @Column(name = "suspended_until")
    private Instant suspendedUntil;

    @Builder.Default
    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
