package com.carebridge.backend.expert.service;

import com.carebridge.backend.expert.dto.request.CreateExpertProfileRequest;
import com.carebridge.backend.expert.dto.request.UpdateExpertProfileRequest;
import com.carebridge.backend.expert.dto.response.ExpertProfileDetailResponse;
import com.carebridge.backend.expert.dto.response.ExpertProfileResponse;
import com.carebridge.backend.expert.dto.response.ExpertDirectoryResponse;
import java.util.List;
import java.util.UUID;

public interface IExpertProfileService {

    ExpertProfileResponse createProfile(UUID userId, CreateExpertProfileRequest request);

    ExpertProfileDetailResponse getMyProfile(UUID userId);

    ExpertProfileDetailResponse updateProfile(UUID userId, UpdateExpertProfileRequest request);

    ExpertDirectoryResponse getPublicDirectory(String specialty, int page, int size);

    ExpertProfileDetailResponse getPublicProfile(UUID expertProfileId);

    List<ExpertProfileResponse> getVerifiedExperts();

    void approveExpert(UUID expertProfileId, UUID adminId);

    void rejectExpert(UUID expertProfileId, UUID adminId, String reason);
}
