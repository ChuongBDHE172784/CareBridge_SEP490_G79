package com.carebridge.backend.expert.service;

import com.carebridge.backend.expert.dto.request.CreateExpertProfileRequest;
import com.carebridge.backend.expert.dto.request.UpdateExpertProfileRequest;
import com.carebridge.backend.expert.dto.request.UploadCredentialRequest;
import com.carebridge.backend.expert.dto.response.ExpertCredentialResponse;
import com.carebridge.backend.expert.dto.response.ExpertProfileResponse;
import com.carebridge.backend.expert.dto.response.ExpertProfilePublicResponse;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.UUID;

public interface ExpertService {

    ExpertProfileResponse createProfile(UUID userId, CreateExpertProfileRequest request);

    ExpertProfileResponse getOwnProfile(UUID userId);

    ExpertProfileResponse updateProfile(UUID userId, UpdateExpertProfileRequest request);

    ExpertProfilePublicResponse getPublicProfile(UUID expertId);

    ExpertCredentialResponse uploadCredential(UUID userId, MultipartFile file, UploadCredentialRequest request);

    List<ExpertCredentialResponse> getMyCredentials(UUID userId);
}
