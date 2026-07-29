package com.carebridge.backend.community.dto.response;

import com.carebridge.backend.community.entity.PregnancyStage;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** UC-20/UC-21 Community Profile response (CB-COMMUNITY-IMP-020 §8.2). */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityProfileResponse {

    private UUID communityProfileId;
    private UUID userId;
    private String displayName;
    private String bio;
    private PregnancyStage interestStage;
    private boolean visible;
    private String publicAvatarUrl;
    private String region;
    private Instant createdAt;
}
