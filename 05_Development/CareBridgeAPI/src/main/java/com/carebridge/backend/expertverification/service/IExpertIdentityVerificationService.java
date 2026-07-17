package com.carebridge.backend.expertverification.service;

import com.carebridge.backend.expertverification.dto.request.ReviewIdentityRequest;
import com.carebridge.backend.expertverification.dto.response.ExpertOnboardingResponse;
import com.carebridge.backend.expertverification.dto.response.IdentityVerificationResponse;
import com.carebridge.backend.file.dto.ViewFileResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface IExpertIdentityVerificationService {
    IdentityVerificationResponse submit(
            UUID userId, MultipartFile selfie, MultipartFile identityFront, MultipartFile identityBack);

    ExpertOnboardingResponse getOnboarding(UUID userId);

    ViewFileResponse getAuthorizedFileUrl(UUID fileId, UUID callerId);

    List<IdentityVerificationResponse> getPendingReviews();

    IdentityVerificationResponse review(UUID attemptId, ReviewIdentityRequest request, UUID reviewerId);
}
