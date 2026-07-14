package com.carebridge.backend.expertverification.service.impl;

import com.carebridge.backend.expert.repository.ExpertProfileRepository;
import com.carebridge.backend.expert.exception.ExpertException;
import com.carebridge.backend.expertverification.dto.request.ReviewCredentialRequest;
import com.carebridge.backend.expertverification.dto.request.SubmitCredentialRequest;
import com.carebridge.backend.expertverification.dto.response.CredentialResponse;
import com.carebridge.backend.expertverification.dto.response.DocumentReviewResponse;
import com.carebridge.backend.expertverification.entity.ExpertCredential;
import com.carebridge.backend.expertverification.mapper.ExpertCredentialMapper;
import com.carebridge.backend.expertverification.repository.ExpertCredentialRepository;
import com.carebridge.backend.expertverification.reviewstatus.ReviewStatus;
import com.carebridge.backend.expertverification.service.IExpertCredentialService;
import com.carebridge.backend.file.dto.UploadFileResponse;
import com.carebridge.backend.file.service.IFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
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
    private final IFileService fileService;

    @Override
    public CredentialResponse submitCredential(UUID userId, SubmitCredentialRequest request, MultipartFile file) {
        var profile = expertProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ExpertException(
                        HttpStatus.NOT_FOUND, "EXPERT-004", "Expert profile not found"));

        // Always create a new credential row — never overwrite
        String fileUrl = null;
        if (file != null && !file.isEmpty()) {
            UploadFileResponse uploadResponse = fileService.uploadFile(file, userId);
            fileUrl = uploadResponse.getPresignedUrl();
        }

        LocalDate issuedDate = parseDate(request.getIssuedDate());
        LocalDate expiryDate = parseDate(request.getExpiryDate());

        var credential = credentialMapper.toEntity(
                profile.getExpertProfileId(), request, issuedDate, expiryDate, fileUrl);
        var saved = credentialRepository.save(credential);
        return credentialMapper.toResponse(saved);
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new ExpertException(
                    HttpStatus.BAD_REQUEST, "EXPVER-005", "Invalid date format: " + value);
        }
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
        List<Object[]> rows = credentialRepository.findPendingWithExpert(ReviewStatus.PENDING);
        return rows.stream()
                .map(row -> {
                    ExpertCredential cred = (ExpertCredential) row[0];
                    var profile = (com.carebridge.backend.expert.entity.ExpertProfile) row[1];
                    var user = (com.carebridge.backend.security.entity.User) row[2];
                    var res = credentialMapper.toDocumentReviewResponse(cred);
                    if (profile != null) {
                        res.setExpertName(user != null ? user.getName() : null);
                        res.setSpecialty(profile.getSpecialty());
                        res.setProfessionalTitle(profile.getProfessionalTitle());
                        res.setExperienceYears(profile.getExperienceYears());
                        res.setWorkplace(profile.getWorkplace());
                        res.setPhone(user != null ? user.getPhone() : null);
                        res.setEmail(user != null ? user.getEmail() : null);
                        res.setRatingAvg(profile.getRatingAvg());
                        res.setAvatarUrl(user != null ? user.getAvatarUrl() : null);
                    }
                    return res;
                })
                .filter(r -> credentialType == null || credentialType.isBlank() || credentialType.equals(r.getCredentialType()))
                .collect(Collectors.toList());
    }
}