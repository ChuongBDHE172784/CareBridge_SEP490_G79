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
@Table(name = "community_profiles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "community_profile_id")
    private UUID communityProfileId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "display_name", length = 100)
    private String displayName;

    @Column(name = "public_avatar_url", length = 500)
    private String publicAvatarUrl;

    @Column(name = "interest_stage", length = 30)
    private String interestStage;

    @Column(name = "region", length = 120)
    private String region;

    @Column(name = "bio", length = 500)
    private String bio;

    @Column(name = "is_visible")
    private Boolean isVisible;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
