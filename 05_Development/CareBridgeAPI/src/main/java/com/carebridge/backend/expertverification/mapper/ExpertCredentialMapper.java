package com.carebridge.backend.expertverification.mapper;

import com.carebridge.backend.expertverification.dto.request.SubmitCredentialRequest;
import com.carebridge.backend.expertverification.dto.request.ReviewCredentialRequest;
import com.carebridge.backend.expertverification.dto.response.CredentialResponse;
import com.carebridge.backend.expertverification.dto.response.DocumentReviewResponse;
import com.carebridge.backend.expertverification.entity.ExpertCredential;
import com.carebridge.backend.expertverification.reviewstatus.ReviewStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class ExpertCredentialMapper {

    public ExpertCredential toEntity(UUID expertProfileId, SubmitCredentialRequest request) {
        return ExpertCredential.builder()
                .expertProfileId(expertProfileId)
                .credentialType(request.getCredentialType())
                .credentialNumber(request.getCredentialNumber())
                .issuer(request.getIssuer())
                .issuedDate(request.getIssuedDate())
                .expiryDate(request.getExpiryDate())
                .fileUrl(request.getFileUrl())
                .reviewStatus(ReviewStatus.PENDING)
                .build();
    }

    public CredentialResponse toResponse(ExpertCredential entity) {
        return CredentialResponse.builder()
                .credentialId(entity.getCredentialId())
                .expertProfileId(entity.getExpertProfileId())
                .credentialType(entity.getCredentialType())
                .credentialNumber(entity.getCredentialNumber())
                .issuer(entity.getIssuer())
                .issuedDate(entity.getIssuedDate())
                .expiryDate(entity.getExpiryDate())
                .fileUrl(entity.getFileUrl())
                .reviewStatus(entity.getReviewStatus())
                .reviewNote(entity.getReviewNote())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public DocumentReviewResponse toDocumentReviewResponse(ExpertCredential entity) {
        return DocumentReviewResponse.builder()
                .credentialId(entity.getCredentialId())
                .expertProfileId(entity.getExpertProfileId())
                .credentialType(entity.getCredentialType())
                .reviewStatus(entity.getReviewStatus())
                .reviewNote(entity.getReviewNote())
                .reviewedBy(entity.getReviewedBy())
                .reviewedAt(entity.getReviewedAt())
                .build();
    }

    public void applyReview(ExpertCredential entity, ReviewCredentialRequest request, UUID reviewerId) {
        entity.setReviewStatus(request.getReviewStatus());
        entity.setReviewNote(request.getReviewNote());
        entity.setReviewedBy(reviewerId);
        entity.setReviewedAt(LocalDateTime.now());
    }
}
