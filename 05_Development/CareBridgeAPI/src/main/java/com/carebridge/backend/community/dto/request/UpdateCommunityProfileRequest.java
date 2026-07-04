package com.carebridge.backend.community.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * UC-21 Update Community Profile (CB-COMMUNITY-IMP-021 §8.1). PUT semantics — full
 * replacement (ADR-COMM-021-001): fields not sent are cleared to null/default, not merged.
 */
@Getter
@Setter
public class UpdateCommunityProfileRequest {

    @Size(max = 100, message = "Display name must not exceed 100 characters")
    private String displayName;

    @Size(max = 500, message = "Bio must not exceed 500 characters")
    private String bio;

    private String interestStage;

    private boolean visible;

    @Size(max = 500)
    private String publicAvatarUrl;

    @Size(max = 120)
    private String region;
}
