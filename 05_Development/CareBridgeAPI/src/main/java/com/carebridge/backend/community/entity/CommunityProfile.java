package com.carebridge.backend.community.entity;

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

/**
 * UC-20/UC-21 — public-facing community display profile. Distinct from
 * {@code com.carebridge.backend.profile.entity.UserProfile} (private account profile,
 * UC-08/UC-09): this table intentionally holds only data safe to show publicly in the
 * community feed (ADR-COMM-020-001 — privacy-by-design separation from {@code users}).
 */
@Entity
@Table(name = "community_profiles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "community_profile_id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID communityProfileId;

    @Column(name = "user_id", columnDefinition = "uuid")
    private UUID userId;

    @Column(name = "display_name", length = 100)
    private String displayName;

    @Column(name = "bio", length = 500)
    private String bio;

    @Column(name = "interest_stage", length = 30)
    private String interestStage;

    @Column(name = "is_visible")
    private boolean visible;

    @Column(name = "public_avatar_url", length = 500)
    private String publicAvatarUrl;

    @Column(name = "region", length = 120)
    private String region;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
