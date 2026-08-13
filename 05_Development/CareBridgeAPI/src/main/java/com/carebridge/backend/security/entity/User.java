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

    // Canonical identity pointer left over from the retired persons table:
    // NOT NULL UNIQUE in the schema, and equal to user_id for canonical rows.
    @Column(name = "person_id", updatable = false)
    private java.util.UUID personId;

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

    @Column(name = "area", length = 100)
    private String area;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "account_status", length = 30)
    private String accountStatus;

    // Deactivation is logical, not a delete: the row stays and carries who did it,
    // when, and why. There is no deletion queue behind this any more.
    @Column(name = "deactivated_at")
    private Instant deactivatedAt;

    @Column(name = "deactivation_reason", columnDefinition = "text")
    private String deactivationReason;

    @Column(name = "deactivated_by")
    private UUID deactivatedBy;

    // Safety monitoring configuration, moved off the safety_configs table (V3 §3.9).
    // Typed columns rather than settings_jsonb keys: a fall-detection hot path reads
    // these, and they carry real CHECK constraints. Sensitivity stays a String here
    // so the security entity does not depend on the safety domain's enum.
    @Builder.Default
    @Column(name = "fall_detection_enabled", nullable = false)
    private boolean fallDetectionEnabled = false;

    @Builder.Default
    @Column(name = "fall_detection_sensitivity_level", nullable = false, length = 10)
    private String fallDetectionSensitivityLevel = "MEDIUM";

    @Builder.Default
    @Column(name = "emergency_auto_alert", nullable = false)
    private boolean emergencyAutoAlert = true;

    @Builder.Default
    @Column(name = "safety_location_sharing_enabled", nullable = false)
    private boolean safetyLocationSharingEnabled = false;

    @Builder.Default
    @Column(name = "emergency_countdown_seconds", nullable = false)
    private int emergencyCountdownSeconds = 30;

    @Builder.Default
    @Column(name = "sensor_permission_granted", nullable = false)
    private boolean sensorPermissionGranted = false;

    @Column(name = "sensor_permission_recorded_at")
    private Instant sensorPermissionRecordedAt;

    @Builder.Default
    @Column(name = "safety_config_updated_at", nullable = false)
    private Instant safetyConfigUpdatedAt = Instant.now();

    @Column(name = "safety_config_updated_by")
    private UUID safetyConfigUpdatedBy;

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

    @Column(name = "locked_at")
    private Instant lockedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "lock_type", length = 30)
    private AccountLockType lockType;

    @Column(name = "lock_reason", length = 500)
    private String lockReason;

    @Column(name = "locked_by")
    private UUID lockedBy;

    @Column(name = "lock_episode_id")
    private UUID lockEpisodeId;

    // Canonical users.suspended_until physical column (nullable). Also mirrored into
    // settings_jsonb->>'suspendedUntil' in canonicalPerson() for jsonb-based queries
    // (e.g. ExpertProfileRepository directory predicates).
    @Column(name = "suspended_until")
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

    // Canonical users.social_identities is NULLABLE (no default); keep the column nullable so
    // the generated H2 schema stays aligned with the canonical PostgreSQL shape.
    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "social_identities", columnDefinition = "jsonb")
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
        if (personId == null) personId = id != null ? id : java.util.UUID.randomUUID();
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
        // locked_at is now canonical; retain a one-way fallback for rows written by
        // earlier application versions that only mirrored it into settings_jsonb.
        if (lockedAt == null) lockedAt = instantSetting("lockedAt");
        if (locked && lockType == null) lockType = AccountLockType.TEMPORARY;
        // suspended_until is a real column now; fall back to the settings_jsonb mirror
        // for legacy rows where only the jsonb copy was written.
        if (suspendedUntil == null) suspendedUntil = instantSetting("suspendedUntil");
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
