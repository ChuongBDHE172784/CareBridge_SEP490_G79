package com.carebridge.backend.expert.service;

import com.carebridge.backend.expert.dto.request.CreateExpertProfileRequest;
import com.carebridge.backend.expert.dto.response.ExpertProfileResponse;
import java.util.UUID;

public interface IExpertProfileService {

    ExpertProfileResponse createProfile(CreateExpertProfileRequest request, UUID userId);
}
