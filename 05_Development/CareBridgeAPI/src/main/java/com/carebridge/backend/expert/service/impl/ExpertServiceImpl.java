package com.carebridge.backend.expert.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.expert.dto.request.CreateExpertProfileRequest;
import com.carebridge.backend.expert.dto.request.UpdateExpertProfileRequest;
import com.carebridge.backend.expert.dto.request.UploadCredentialRequest;
import com.carebridge.backend.expert.dto.response.ExpertCredentialResponse;
import com.carebridge.backend.expert.dto.response.ExpertProfileResponse;
import com.carebridge.backend.expert.dto.response.ExpertProfilePublicResponse;
import com.carebridge.backend.expert.entity.ExpertCredential;
import com.carebridge.backend.expert.entity.ExpertProfile;
import com.carebridge.backend.expert.mapper.ExpertCredentialMapper;
import com.carebridge.backend.expert.mapper.ExpertProfileMapper;
import com.carebridge.backend.expert.policy.ExpertProfilePolicy;
import com.carebridge.backend.expert.repository.ExpertCredentialRepository;
import com.carebridge.backend.expert.repository.ExpertProfileRepository;
import com.carebridge.backend.expert.service.ExpertService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExpertServiceImpl implements ExpertService {

    private final ExpertProfileRepository expertProfileRepository;
    private final ExpertCredentialRepository expertCredentialRepository;
    private final ExpertProfileMapper expertProfileMapper;
    private final ExpertCredentialMapper expertCredentialMapper;
    private final ExpertProfilePolicy expertProfilePolicy;
    private final AuditService auditService;

    @Override
    public ExpertProfileResponse createProfile(UUID userId, CreateExpertProfileRequest request) {
        // Check for duplicate profile
        if (expertProfileRepository.existsByUserId(userId)) {
            throw new IllegalArgumentException("Expert profile already exists for this user");
        }

        // Build entity
        ExpertProfile profile = ExpertProfile.builder()
                .userId(userId)
                .bio(request.getBio())
                .expertiseAreas(request.getExpertiseAreas())
                .yearsExperience(request.getYearsExperience())
                .qualifications(request.getQualifications())
                .hourlyRate(request.getHourlyRate())
                .avgRating(java.math.BigDecimal.ZERO)
                .totalReviews(0)
                .isVerified(false)
                .isAvailable(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        ExpertProfile saved = expertProfileRepository.save(profile);

        // Audit log
        auditService.log(AuditAction.EXPERT_PROFILE_CREATED, userId, "expert_profile", saved.getId().toString(), null);

        return expertProfileMapper.toResponse(saved);
    }

    @Override
    public ExpertProfileResponse getOwnProfile(UUID userId) {
        ExpertProfile profile = expertProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Expert profile not found"));
        return expertProfileMapper.toResponse(profile);
    }

    @Override
    public ExpertProfileResponse updateProfile(UUID userId, UpdateExpertProfileRequest request) {
        ExpertProfile profile = expertProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Expert profile not found"));

        // Policy check
        expertProfilePolicy.checkCanEditProfile(userId, profile.getId());

        // Update fields (partial update)
        if (request.getBio() != null) profile.setBio(request.getBio());
        if (request.getExpertiseAreas() != null) profile.setExpertiseAreas(request.getExpertiseAreas());
        if (request.getYearsExperience() != null) profile.setYearsExperience(request.getYearsExperience());
        if (request.getQualifications() != null) profile.setQualifications(request.getQualifications());
        if (request.getHourlyRate() != null) profile.setHourlyRate(request.getHourlyRate());
        profile.setUpdatedAt(Instant.now());

        ExpertProfile saved = expertProfileRepository.save(profile);

        // Audit log
        auditService.log(AuditAction.EXPERT_PROFILE_UPDATED, userId, "expert_profile", saved.getId().toString(), null);

        return expertProfileMapper.toResponse(saved);
    }

    @Override
    public ExpertProfilePublicResponse getPublicProfile(UUID expertId) {
        ExpertProfile profile = expertProfileRepository.findById(expertId)
                .orElseThrow(() -> new IllegalArgumentException("Expert profile not found"));

        if (!profile.getIsVerified()) {
            throw new IllegalArgumentException("Expert profile is not verified");
        }

        return expertProfileMapper.toPublicResponse(profile);
    }

    @Override
    public ExpertCredentialResponse uploadCredential(UUID userId, MultipartFile file, UploadCredentialRequest request) {
        // Get expert profile
        ExpertProfile profile = expertProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Expert profile not found"));

        // Policy check
        expertProfilePolicy.checkCanEditProfile(userId, profile.getId());

        // Mock file storage: save to local filesystem (in production, use cloud storage)
        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        String fileUrl = "/uploads/credentials/" + fileName;

        try {
            Path uploadDir = Paths.get("uploads/credentials");
            Files.createDirectories(uploadDir);
            Files.copy(file.getInputStream(), uploadDir.resolve(fileName));
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }

        // Create credential entity
        ExpertCredential credential = ExpertCredential.builder()
                .expertProfileId(profile.getId())
                .credentialType(request.getCredentialType())
                .fileUrl(fileUrl)
                .fileName(fileName)
                .issuingAuthority(request.getIssuingAuthority())
                .issueDate(request.getIssueDate())
                .expiryDate(request.getExpiryDate())
                .verificationStatus("PENDING")
                .createdAt(Instant.now())
                .build();

        ExpertCredential saved = expertCredentialRepository.save(credential);

        // Audit log
        auditService.log(AuditAction.EXPERT_CREDENTIAL_UPLOADED, userId, "expert_credential", saved.getId().toString(), null);

        return expertCredentialMapper.toResponse(saved);
    }

    @Override
    public List<ExpertCredentialResponse> getMyCredentials(UUID userId) {
        ExpertProfile profile = expertProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Expert profile not found"));

        List<ExpertCredential> credentials = expertCredentialRepository.findByExpertProfileId(profile.getId());
        return expertCredentialMapper.toResponseList(credentials);
    }
}
