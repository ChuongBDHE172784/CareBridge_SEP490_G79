package com.carebridge.backend.expert.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.expert.dto.request.CreateExpertProfileRequest;
import com.carebridge.backend.expert.dto.response.ExpertProfileResponse;
import com.carebridge.backend.expert.entity.ExpertProfile;
import com.carebridge.backend.expert.entity.ExpertProfileStatus;
import com.carebridge.backend.expert.mapper.ExpertProfileMapper;
import com.carebridge.backend.expert.repository.IExpertProfileRepository;
import com.carebridge.backend.expert.service.IExpertProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ExpertProfileServiceImpl implements IExpertProfileService {

    private final IExpertProfileRepository profileRepository;
    private final ExpertProfileMapper mapper;
    private final AuditService auditService;

    @Override
    public ExpertProfileResponse createProfile(CreateExpertProfileRequest request, UUID userId) {
        if (profileRepository.existsByUserId(userId)) {
            throw new BusinessException(HttpStatus.CONFLICT, "EXP-002", "Expert profile already exists for this account");
        }

        ExpertProfile profile = ExpertProfile.builder()
                .userId(userId)
                .displayName(request.getDisplayName())
                .bio(request.getBio())
                .specialties(request.getSpecialties())
                .yearsOfExperience(request.getYearsOfExperience())
                .consultationFeeVnd(request.getConsultationFeeVnd())
                .consultationModalities(request.getConsultationModalities())
                .status(ExpertProfileStatus.PENDING_VERIFICATION)
                .build();

        ExpertProfile saved = profileRepository.save(profile);

        auditService.log(
                AuditAction.EXPERT_PROFILE_CREATED,
                userId,
                "ExpertProfile",
                saved.getId().toString(),
                saved
        );

        return mapper.toResponse(saved);
    }
}
