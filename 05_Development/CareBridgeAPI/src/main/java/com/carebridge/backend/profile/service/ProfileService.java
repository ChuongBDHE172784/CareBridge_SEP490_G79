package com.carebridge.backend.profile.service;

import com.carebridge.backend.profile.dto.ProfileResponse;
import com.carebridge.backend.profile.dto.UpdateProfileRequest;
import java.util.UUID;

public interface ProfileService {

    ProfileResponse updateProfile(UUID authenticatedUserId, UpdateProfileRequest request);

    ProfileResponse getProfile(UUID authenticatedUserId);
}
