package com.carebridge.backend.profile.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.ResourceNotFoundException;
import com.carebridge.backend.common.exception.ValidationException;
import com.carebridge.backend.profile.dto.ProfileResponse;
import com.carebridge.backend.profile.dto.UpdateProfileRequest;
import com.carebridge.backend.profile.entity.UserProfile;
import com.carebridge.backend.profile.repository.ProfileRepository;
import com.carebridge.backend.profile.service.ProfileService;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileServiceImpl implements ProfileService {

    private final ProfileRepository profileRepository;
    private final AuditService auditService;

    @Override
    @Transactional
    public ProfileResponse updateProfile(UUID authenticatedUserId, UpdateProfileRequest request) {
        // C4: validate dateOfBirth — must be >= 1900-01-01 AND < today
        if (request.dateOfBirth() != null) {
            validateDateOfBirth(request.dateOfBirth());
        }

        UserProfile profile = profileRepository.findByUserId(authenticatedUserId)
                .orElse(UserProfile.builder().userId(authenticatedUserId).build());

        // C2: sanitize displayName (strip HTML tags)
        if (request.displayName() != null) {
            profile.setDisplayName(sanitizeDisplayName(request.displayName()));
        }
        if (request.avatarUrl() != null) {
            profile.setAvatarUrl(request.avatarUrl());
        }
        if (request.phoneNumber() != null) {
            profile.setPhoneNumber(request.phoneNumber());
        }
        if (request.dateOfBirth() != null) {
            profile.setDateOfBirth(request.dateOfBirth());
        }
        if (request.area() != null) {
            profile.setArea(request.area());
        }

        UserProfile saved = profileRepository.save(profile);

        // C5: audit in same @Transactional
        auditService.log(
                AuditAction.PROFILE_UPDATED,
                authenticatedUserId,
                "UserProfile",
                saved.getProfileId().toString(),
                "Profile updated by user"
        );

        log.info("Profile updated: userId={}", authenticatedUserId);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getProfile(UUID authenticatedUserId) {
        UserProfile profile = profileRepository.findByUserId(authenticatedUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));
        return toResponse(profile);
    }

    private void validateDateOfBirth(LocalDate dob) {
        LocalDate minDate = LocalDate.of(1900, 1, 1);
        LocalDate today = LocalDate.now();
        if (dob.isBefore(minDate) || !dob.isBefore(today)) {
            throw new ValidationException("Date of birth must be between 1900-01-01 and today");
        }
    }

    private String sanitizeDisplayName(String name) {
        if (name == null) return null;
        return name.replaceAll("<[^>]*>", "").trim();
    }

    private ProfileResponse toResponse(UserProfile profile) {
        return new ProfileResponse(
                profile.getUserId(),
                profile.getDisplayName(),
                profile.getAvatarUrl(),
                profile.getPhoneNumber(),
                profile.getDateOfBirth(),
                profile.getArea(),
                profile.getUpdatedAt()
        );
    }
}
