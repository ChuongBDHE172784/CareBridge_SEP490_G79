package com.carebridge.backend.community.service;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.community.dto.request.CreateCommunityProfileRequest;
import com.carebridge.backend.community.dto.request.UpdateCommunityProfileRequest;
import com.carebridge.backend.community.dto.response.CommunityProfileResponse;
import com.carebridge.backend.community.entity.CommunityProfile;
import com.carebridge.backend.community.entity.PregnancyStage;
import com.carebridge.backend.community.exception.CommunityProfileAlreadyExistsException;
import com.carebridge.backend.community.exception.CommunityProfileNotFoundException;
import com.carebridge.backend.community.repository.CommunityProfileRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-20 Create Community Profile / UC-21 Update Community Profile
 * (CB-COMMUNITY-IMP-020/021 §17). Ownership is always the {@code userId} parameter
 * (from JWT, per the controller) — never trusted from the request body or a path variable.
 */
@Service
@RequiredArgsConstructor
public class CommunityProfileServiceImpl implements CommunityProfileService {

    private final CommunityProfileRepository profileRepository;
    private final AuditService auditService;

    @Override
    @Transactional
    public CommunityProfileResponse createProfile(UUID userId, CreateCommunityProfileRequest request) {
        if (profileRepository.existsByUserId(userId)) {
            throw new CommunityProfileAlreadyExistsException(
                    "Community profile already exists for user: " + userId);
        }

        Instant now = Instant.now();
        CommunityProfile profile = profileRepository.findAccountByUserId(userId)
                .orElseThrow(() -> new CommunityProfileNotFoundException(
                        "Account not found for user: " + userId));
        profile.setDisplayName(request.getDisplayName());
        profile.setBio(request.getBio());
        profile.setInterestStage(stageName(request.getInterestStage()));
        profile.setVisible(request.isVisible());
        profile.setPublicAvatarUrl(request.getPublicAvatarUrl());
        profile.setRegion(request.getRegion());
        profile.setUpdatedAt(now);

        CommunityProfile saved = profileRepository.save(profile);

        auditService.log(AuditAction.COMMUNITY_PROFILE_CREATED, userId, "CommunityProfile",
                saved.getCommunityProfileId().toString(), null);

        return toResponse(saved);
    }

    @Override
    @Transactional
    public CommunityProfileResponse updateProfile(UUID userId, UpdateCommunityProfileRequest request) {
        CommunityProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new CommunityProfileNotFoundException(
                        "Community profile not found for user: " + userId));

        // PUT semantics (ADR-COMM-021-001) — replace all fields; not-sent fields become null.
        profile.setDisplayName(request.getDisplayName());
        profile.setBio(request.getBio());
        profile.setInterestStage(stageName(request.getInterestStage()));
        profile.setVisible(request.isVisible());
        profile.setPublicAvatarUrl(request.getPublicAvatarUrl());
        profile.setRegion(request.getRegion());
        profile.setUpdatedAt(Instant.now());

        CommunityProfile saved = profileRepository.save(profile);

        auditService.log(AuditAction.COMMUNITY_PROFILE_UPDATED, userId, "CommunityProfile",
                saved.getCommunityProfileId().toString(), "isVisible=" + saved.isVisible());

        return toResponse(saved);
    }

    private CommunityProfileResponse toResponse(CommunityProfile profile) {
        return CommunityProfileResponse.builder()
                .communityProfileId(profile.getCommunityProfileId())
                .userId(profile.getUserId())
                .displayName(profile.getDisplayName())
                .bio(profile.getBio())
                .interestStage(parseStage(profile.getInterestStage()))
                .visible(profile.isVisible())
                .publicAvatarUrl(profile.getPublicAvatarUrl())
                .region(profile.getRegion())
                .createdAt(profile.getCreatedAt())
                .build();
    }

    private String stageName(PregnancyStage stage) {
        return stage == null ? null : stage.name();
    }

    private PregnancyStage parseStage(String stage) {
        return stage == null ? null : PregnancyStage.fromApiValue(stage);
    }
}
