package com.carebridge.backend.community.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** UC-20 Create Community Profile (CB-COMMUNITY-IMP-020 §8.1). */
@Getter
@Setter
public class CreateCommunityProfileRequest {

    @NotBlank(message = "Display name is required")
    @Size(max = 100, message = "Display name must not exceed 100 characters")
    private String displayName;

    @Size(max = 500, message = "Bio must not exceed 500 characters")
    private String bio;

    /** nullable — PRE_PREGNANCY, PREGNANCY, POSTPARTUM, BABY_CARE, etc. */
    private String interestStage;

    /** Default true (opt-out model, ADR-COMM-020-002). */
    private boolean visible = true;

    @Size(max = 500)
    private String publicAvatarUrl;

    @Size(max = 120)
    private String region;
}
