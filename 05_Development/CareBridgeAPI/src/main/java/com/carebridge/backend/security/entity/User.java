package com.carebridge.backend.security.entity;

import com.carebridge.backend.security.rbac.Role;
import com.carebridge.backend.profile.entity.Person;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.PostLoad;
import jakarta.persistence.Transient;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

    @Transient
    private Person person;

    @Column(unique = true, length = 20)
    private String phone;

    @Column(length = 255)
    private String email;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "full_name", length = 120)
    private String name;

    @Column(name = "display_name", length = 200)
    private String displayName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "account_status", length = 30)
    private String accountStatus;

    @Column(name = "email_verified")
    private Boolean emailVerified;

    @Column(name = "phone_verified")
    private Boolean phoneVerified;

    @Transient
    private Instant lastLoginAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 50)
    private Role role;

    @Builder.Default
    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Builder.Default
    @Column(name = "locked", nullable = false)
    private boolean locked = false;

    @Transient
    private Instant lockedAt;

    @Transient
    private Instant suspendedUntil;

    @Transient
    private Instant communityPostingRestrictedUntil;

    @Builder.Default
    @Transient
    private boolean mustChangePassword = false;

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "settings_jsonb", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> settings = new HashMap<>();

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "social_identities", nullable = false, columnDefinition = "jsonb")
    private List<Map<String, Object>> socialIdentities = new java.util.ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void canonicalPerson() {
        if (displayName == null || displayName.isBlank()) displayName = name;
        if (person != null) {
            if (person.getDisplayName() != null) {
                displayName = person.getDisplayName();
                name = person.getDisplayName();
            }
            if (person.getDateOfBirth() != null) dateOfBirth = person.getDateOfBirth();
        }
        putSetting("lastLoginAt", lastLoginAt);
        putSetting("lockedAt", lockedAt);
        putSetting("suspendedUntil", suspendedUntil);
        putSetting("communityPostingRestrictedUntil", communityPostingRestrictedUntil);
        settings.put("mustChangePassword", mustChangePassword);
    }

    @PostLoad
    void hydratePersonAdapter() {
        person = Person.builder()
                .id(id)
                .displayName(displayName != null ? displayName : name)
                .dateOfBirth(dateOfBirth)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
        lastLoginAt = instantSetting("lastLoginAt");
        lockedAt = instantSetting("lockedAt");
        suspendedUntil = instantSetting("suspendedUntil");
        communityPostingRestrictedUntil = instantSetting("communityPostingRestrictedUntil");
        mustChangePassword = Boolean.TRUE.equals(settings.get("mustChangePassword"));
    }

    private void putSetting(String key, Instant value) {
        if (settings == null) settings = new HashMap<>();
        if (value == null) settings.remove(key);
        else settings.put(key, value.toString());
    }

    private Instant instantSetting(String key) {
        if (settings == null) return null;
        Object value = settings.get(key);
        if (value == null) return null;
        try {
            return Instant.parse(value.toString());
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
