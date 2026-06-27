package com.carebridge.backend.baby.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.baby.dto.BabyProfileDetailResponse;
import com.carebridge.backend.baby.dto.CreateBabyProfileRequest;
import com.carebridge.backend.baby.dto.CreateBabyProfileResponse;
import com.carebridge.backend.baby.entity.BabyProfile;
import com.carebridge.backend.baby.policy.BabyAccessPolicy;
import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.baby.service.IBabyService;
import com.carebridge.backend.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class BabyServiceImpl implements IBabyService {

    private final BabyProfileRepository babyRepository;
    private final BabyAccessPolicy accessPolicy;
    private final AuditService auditService;

    @Override
    public CreateBabyProfileResponse createBabyProfile(CreateBabyProfileRequest request, UUID callerId) {
        // C4: accountId from JWT (callerId)
        BabyProfile profile = BabyProfile.builder()
                .ownerUserId(callerId)
                .nickname(request.getNickname())
                .birthDate(request.getBirthDate())
                .gender(request.getGender())
                .birthWeightKg(request.getBirthWeightKg())
                .birthLengthCm(request.getBirthLengthCm())
                .relatedJourneyId(request.getRelatedJourneyId())
                .build();

        BabyProfile saved = babyRepository.save(profile);

        // C3: emit audit event
        auditService.log(AuditAction.BABY_PROFILE_CREATED, callerId,
                "BabyProfile", saved.getId().toString(), "created");

        return CreateBabyProfileResponse.builder()
                .id(saved.getId())
                .nickname(saved.getNickname())
                .birthDate(saved.getBirthDate())
                .gender(saved.getGender() != null ? saved.getGender().name() : null)
                .birthWeightKg(saved.getBirthWeightKg())
                .birthLengthCm(saved.getBirthLengthCm())
                .status(saved.getStatus().name())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public BabyProfileDetailResponse getBabyProfile(UUID profileId, UUID callerId) {
        BabyProfile profile = babyRepository.findById(profileId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "BABY-001",
                        "Baby profile not found: " + profileId));

        // C1: BabyAccessPolicy — ownership OR ACCEPTED care group member
        if (!accessPolicy.canView(profile, callerId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "BABY-003",
                    "Access denied to baby profile");
        }

        // C2: ARCHIVED still returns 200 (BR-BABY-011)
        return BabyProfileDetailResponse.builder()
                .id(profile.getId())
                .nickname(profile.getNickname())
                .birthDate(profile.getBirthDate())
                .gender(profile.getGender() != null ? profile.getGender().name() : null)
                .birthWeightKg(profile.getBirthWeightKg())
                .birthLengthCm(profile.getBirthLengthCm())
                .status(profile.getStatus().name())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }
}
