package com.carebridge.backend.expert.mapper;

import com.carebridge.backend.expert.dto.response.ExpertProfileDetailResponse;
import com.carebridge.backend.expert.dto.response.ExpertProfileResponse;
import com.carebridge.backend.expert.dto.response.ExpertDirectoryResponse;
import com.carebridge.backend.expert.entity.ExpertProfile;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ExpertProfileMapper {

	public ExpertProfileResponse toResponse(ExpertProfile entity, String avatarUrl) {
		return ExpertProfileResponse.builder()
			.expertProfileId(entity.getExpertProfileId())
			.userId(entity.getUserId())
			.specialty(entity.getSpecialty())
			.professionalTitle(entity.getProfessionalTitle())
			.experienceYears(entity.getExperienceYears())
			.workplace(entity.getWorkplace())
			.verificationStatus(entity.getVerificationStatus())
			.verifiedAt(entity.getVerifiedAt())
			.verifiedBy(entity.getVerifiedBy())
			.ratingAvg(entity.getRatingAvg())
			.avatarUrl(avatarUrl)
			.createdAt(entity.getCreatedAt())
			.updatedAt(entity.getUpdatedAt())
			.build();
	}

	public ExpertProfileResponse toResponse(ExpertProfile entity) {
		return toResponse(entity, null);
	}

	public ExpertProfileDetailResponse toDetailResponse(ExpertProfile entity, String avatarUrl) {
		return ExpertProfileDetailResponse.builder()
			.expertProfileId(entity.getExpertProfileId())
			.userId(entity.getUserId())
			.specialty(entity.getSpecialty())
			.professionalTitle(entity.getProfessionalTitle())
			.experienceYears(entity.getExperienceYears())
			.workplace(entity.getWorkplace())
			.verificationStatus(entity.getVerificationStatus())
			.verifiedAt(entity.getVerifiedAt())
			.verifiedBy(entity.getVerifiedBy())
			.ratingAvg(entity.getRatingAvg())
			.avatarUrl(avatarUrl)
			.createdAt(entity.getCreatedAt())
			.updatedAt(entity.getUpdatedAt())
			.build();
	}

	public ExpertProfileDetailResponse toDetailResponse(ExpertProfile entity) {
		return toDetailResponse(entity, null);
	}

	public ExpertDirectoryResponse toDirectoryResponse(Page<ExpertProfile> page) {
		List<ExpertProfileResponse> experts = page.getContent().stream()
			.map(this::toResponse)
			.collect(Collectors.toList());
		return new ExpertDirectoryResponse(
			experts,
			page.getNumber(),
			page.getSize(),
			page.getTotalElements(),
			page.getTotalPages()
		);
	}
}
