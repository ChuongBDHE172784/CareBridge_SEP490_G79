package com.carebridge.backend.community.service;

import com.carebridge.backend.community.dto.request.CreateCommunityProfileRequest;
import com.carebridge.backend.community.dto.request.UpdateCommunityProfileRequest;
import com.carebridge.backend.community.dto.response.CommunityProfileResponse;
import java.util.UUID;

/** UC-20 Create Community Profile / UC-21 Update Community Profile. */
public interface CommunityProfileService {

    /**
     * Creates a new community profile for {@code userId}.
     *
     * @throws com.carebridge.backend.community.exception.CommunityProfileAlreadyExistsException
     *         COMM-001 if a profile already exists for this user
     */
    CommunityProfileResponse createProfile(UUID userId, CreateCommunityProfileRequest request);

    /**
     * Replaces (PUT semantics) the community profile for {@code userId}.
     *
     * @throws com.carebridge.backend.community.exception.CommunityProfileNotFoundException
     *         COMM-011 if no profile exists yet for this user
     */
    CommunityProfileResponse updateProfile(UUID userId, UpdateCommunityProfileRequest request);
}
