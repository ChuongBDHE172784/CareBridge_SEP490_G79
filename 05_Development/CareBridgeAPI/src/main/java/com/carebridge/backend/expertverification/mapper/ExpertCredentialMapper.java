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

	public ExpertCredential toEntity(UUID expertProfileId, SubmitCredentialRequest request, java.time.LocalDate issuedDate, java.time.LocalDate expiryDate, UUID fileId) {
		return ExpertCredential.builder()
			.expertProfileId(expertProfileId)
			.credentialType(request.getCredentialType())
			.credentialNumber(request.getCredentialNumber())
			.issuer(request.getIssuer())
			.issuedDate(issuedDate)
			.expiryDate(expiryDate)
			.fileId(fileId)
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
			.fileId(entity.getFileId())
			.reviewStatus(entity.getReviewStatus())
			.reviewNote(entity.getReviewNote())
			.createdAt(entity.getCreatedAt())
			.updatedAt(entity.getUpdatedAt())
			.build();
	}

	public DocumentReviewResponse toDocumentReviewResponse(ExpertCredential entity) {
		var res = new DocumentReviewResponse();
		res.setCredentialId(entity.getCredentialId());
		res.setExpertProfileId(entity.getExpertProfileId());
		res.setCredentialType(entity.getCredentialType());
		res.setCredentialNumber(entity.getCredentialNumber());
		res.setIssuer(entity.getIssuer());
		res.setIssuedDate(entity.getIssuedDate());
		res.setExpiryDate(entity.getExpiryDate());
		res.setFileUrl(entity.getFileUrl());
		res.setFileId(entity.getFileId());
		res.setCreatedAt(entity.getCreatedAt());
		res.setReviewStatus(entity.getReviewStatus());
		res.setReviewNote(entity.getReviewNote());
		res.setReviewedBy(entity.getReviewedBy());
		res.setReviewedAt(entity.getReviewedAt());
		return res;
	}

	public void applyReview(ExpertCredential entity, ReviewCredentialRequest request, UUID reviewerId) {
		entity.setReviewStatus(request.getReviewStatus());
		entity.setReviewNote(request.getReviewNote());
		entity.setReviewedBy(reviewerId);
		entity.setReviewedAt(LocalDateTime.now());
	}
}
