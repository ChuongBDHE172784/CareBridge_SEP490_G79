package com.carebridge.backend.expert.mapper;

import com.carebridge.backend.expert.dto.response.ExpertProfileResponse;
import com.carebridge.backend.expert.entity.ConsultationModality;
import com.carebridge.backend.expert.entity.ExpertProfile;
import com.carebridge.backend.expert.entity.ExpertProfileStatus;
import org.springframework.stereotype.Component;

@Component
public class ExpertProfileMapper {

    public ExpertProfileResponse toResponse(ExpertProfile entity) {
        return ExpertProfileResponse.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .displayName(entity.getDisplayName())
                .bio(entity.getBio())
                .specialties(entity.getSpecialties())
                .yearsOfExperience(entity.getYearsOfExperience())
                .consultationFeeVnd(entity.getConsultationFeeVnd())
                .consultationModalities(entity.getConsultationModalities())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
