package com.carebridge.backend.privacy.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "privacy_settings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrivacySettings {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "profile_visibility", nullable = false, length = 20)
    private ProfileVisibility profileVisibility = ProfileVisibility.FRIENDS_ONLY;

    @Builder.Default
    @Column(name = "location_sharing_enabled", nullable = false)
    private boolean locationSharingEnabled = false;

    @Builder.Default
    @Column(name = "analytics_consent", nullable = false)
    private boolean analyticsConsent = false;

    @Builder.Default
    @Column(name = "data_export_opt_out", nullable = false)
    private boolean dataExportOptOut = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static PrivacySettings defaultSettings(UUID userId) {
        return PrivacySettings.builder()
                .userId(userId)
                .profileVisibility(ProfileVisibility.FRIENDS_ONLY)
                .locationSharingEnabled(false)
                .analyticsConsent(false)
                .dataExportOptOut(false)
                .build();
    }
}
