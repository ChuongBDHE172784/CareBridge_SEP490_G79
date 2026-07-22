package com.carebridge.backend.expertverification.service;

import com.carebridge.backend.expertverification.dto.request.SubmitCredentialRequest;
import com.carebridge.backend.expertverification.dto.request.ReviewCredentialRequest;
import com.carebridge.backend.expertverification.dto.response.CredentialResponse;
import com.carebridge.backend.expertverification.dto.response.DocumentReviewResponse;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.UUID;

public interface IExpertCredentialService {

 CredentialResponse submitCredential(UUID userId, SubmitCredentialRequest request, MultipartFile file);

 List<CredentialResponse> getMyCredentials(UUID userId);

 CredentialResponse getCredentialDetail(UUID credentialId, UUID userId);

 void deleteCredential(UUID credentialId, UUID userId);

 DocumentReviewResponse reviewCredential(UUID credentialId, ReviewCredentialRequest request, UUID reviewerId);

 List<DocumentReviewResponse> getPendingReviews(String credentialType, UUID reviewerId);
}
