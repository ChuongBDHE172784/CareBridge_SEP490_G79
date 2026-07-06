package com.carebridge.backend.expertverification.service.impl;

import com.carebridge.backend.expert.exception.ExpertException;
import com.carebridge.backend.expert.repository.ExpertProfileRepository;
import com.carebridge.backend.expertverification.dto.request.ReviewCredentialRequest;
import com.carebridge.backend.expertverification.dto.request.SubmitCredentialRequest;
import com.carebridge.backend.expertverification.dto.response.CredentialResponse;
import com.carebridge.backend.expertverification.dto.response.DocumentReviewResponse;
import com.carebridge.backend.expertverification.entity.ExpertCredential;
import com.carebridge.backend.expertverification.mapper.ExpertCredentialMapper;
import com.carebridge.backend.expertverification.repository.ExpertCredentialRepository;
import com.carebridge.backend.expertverification.reviewstatus.ReviewStatus;
import com.carebridge.backend.expertverification.service.IExpertCredentialService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ExpertCredentialServiceImpl implements IExpertCredentialService {

    private final ExpertCredentialRepository credentialRepository;
    private final ExpertProfileRepository expertProfileRepository;
    private final ExpertCredentialMapper credentialMapper;

    @Override
    public CredentialResponse submitCredential(UUID userId, SubmitCredentialRequest request) {
        var profile = expertProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ExpertException(
                        HttpStatus.NOT_FOUND, "EXPERT-004", "Expert profile not found"));

        if (credentialRepository.existsByExpertProfileIdAndCredentialType(
                profile.getExpertProfileId(), request.getCredentialType())) {
            throw new ExpertException(
                    HttpStatus.CONFLICT, "EXPERT-003",
                    "Credential of this type already exists");
        }

        var credential = credentialMapper.toEntity(profile.getExpertProfileId(), request);
        var saved = credentialRepository.save(credential);
        return credentialMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CredentialResponse> getMyCredentials(UUID userId) {
        var profile = expertProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ExpertException(
                        HttpStatus.NOT_FOUND, "EXPERT-004", "Expert profile not found"));

        return credentialRepository.findByExpertProfileId(profile.getExpertProfileId()).stream()
                .map(credentialMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CredentialResponse getCredentialDetail(UUID credentialId, UUID userId) {
        var credential = credentialRepository.findByCredentialId(credentialId)
                .orElseThrow(() -> new ExpertException(
                        HttpStatus.NOT_FOUND, "EXPVER-004", "Credential not found"));

        var profile = expertProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ExpertException(
                        HttpStatus.NOT_FOUND, "EXPERT-004", "Expert profile not found"));

        if (!credential.getExpertProfileId().equals(profile.getExpertProfileId())) {
            throw new ExpertException(
                    HttpStatus.FORBIDDEN, "EXPERT-005", "Insufficient permissions");
        }

        return credentialMapper.toResponse(credential);
    }

    @Override
    public void deleteCredential(UUID credentialId, UUID userId) {
        var credential = credentialRepository.findByCredentialId(credentialId)
                .orElseThrow(() -> new ExpertException(
                        HttpStatus.NOT_FOUND, "EXPVER-004", "Credential not found"));

        var profile = expertProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ExpertException(
                        HttpStatus.NOT_FOUND, "EXPERT-004", "Expert profile not found"));

        if (!credential.getExpertProfileId().equals(profile.getExpertProfileId())) {
            throw new ExpertException(
                    HttpStatus.FORBIDDEN, "EXPERT-005", "Insufficient permissions");
        }

        credentialRepository.delete(credential);
    }

    @Override
    public DocumentReviewResponse reviewCredential(UUID credentialId, ReviewCredentialRequest request, UUID reviewerId) {
        var credential = credentialRepository.findByCredentialId(credentialId)
                .orElseThrow(() -> new ExpertException(
                        HttpStatus.NOT_FOUND, "EXPVER-004", "Credential not found"));

        credentialMapper.applyReview(credential, request, reviewerId);
        var saved = credentialRepository.save(credential);
        return credentialMapper.toDocumentReviewResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentReviewResponse> getPendingReviews(String credentialType) {
        List<ExpertCredential> credentials = credentialRepository.findByReviewStatus(ReviewStatus.PENDING);
        if (credentialType != null && !credentialType.isBlank()) {
            credentials = credentials.stream()
                    .filter(c -> credentialType.equals(c.getCredentialType()))
                    .collect(Collectors.toList());
        }
        return credentials.stream()
                .map(credentialMapper::toDocumentReviewResponse)
                .collect(Collectors.toList());
    }
}
