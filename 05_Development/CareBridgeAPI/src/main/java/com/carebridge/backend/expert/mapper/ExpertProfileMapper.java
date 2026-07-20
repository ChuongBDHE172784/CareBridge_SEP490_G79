package com.carebridge.backend.expert.mapper;

import com.carebridge.backend.expert.dto.request.CreateExpertProfileRequest;
import com.carebridge.backend.expert.dto.request.UpdateExpertProfileRequest;
import com.carebridge.backend.expert.dto.response.ExpertProfileDetailResponse;
import com.carebridge.backend.expert.dto.response.ExpertProfileResponse;
import com.carebridge.backend.expert.dto.response.ExpertDirectoryResponse;
import com.carebridge.backend.expert.verificationstatus.VerificationStatus;
import com.carebridge.backend.expert.entity.ExpertProfile;
import com.carebridge.backend.security.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ExpertProfileMapper {

	// ADR-MEDI-001 mục 1 — displayName/avatarUrl always resolved together from the caller
	// (batch for directory listings, single-row lookup elsewhere). No overload silently
	// defaults either field to null anymore (that was the source of the avatar=null bug).
	public ExpertProfileResponse toResponse(ExpertProfile entity, String displayName, String avatarUrl) {
		return ExpertProfileResponse.builder()
			.expertProfileId(entity.getExpertProfileId())
			.userId(entity.getUserId())
			.displayName(displayName)
			.specialty(entity.getSpecialty())
			.specialtyId(entity.getSpecialtyId())
			.professionalTitle(entity.getProfessionalTitle())
			.experienceYears(entity.getExperienceYears())
			.workplace(entity.getWorkplace())
			.hospitalId(entity.getHospitalId())
			.consultationScope(entity.getConsultationScope())
			.verificationStatus(entity.getVerificationStatus())
			.isConsultationEligible(entity.isEligibleForConsultation())
			.verifiedAt(entity.getVerifiedAt())
			.verifiedBy(entity.getVerifiedBy())
			.ratingAvg(entity.getRatingAvg())
			.avatarUrl(avatarUrl)
			.createdAt(entity.getCreatedAt())
			.updatedAt(entity.getUpdatedAt())
			.build();
	}

	public ExpertProfileDetailResponse toDetailResponse(ExpertProfile entity, String displayName, String avatarUrl) {
		return ExpertProfileDetailResponse.builder()
			.expertProfileId(entity.getExpertProfileId())
			.userId(entity.getUserId())
			.displayName(displayName)
			.specialty(entity.getSpecialty())
			.specialtyId(entity.getSpecialtyId())
			.professionalTitle(entity.getProfessionalTitle())
			.experienceYears(entity.getExperienceYears())
			.workplace(entity.getWorkplace())
			.hospitalId(entity.getHospitalId())
			.consultationScope(entity.getConsultationScope())
			.verificationStatus(entity.getVerificationStatus())
			.isConsultationEligible(entity.isEligibleForConsultation())
			.verifiedAt(entity.getVerifiedAt())
			.verifiedBy(entity.getVerifiedBy())
			.ratingAvg(entity.getRatingAvg())
			.avatarUrl(avatarUrl)
			.createdAt(entity.getCreatedAt())
			.updatedAt(entity.getUpdatedAt())
			.build();
	}

	public ExpertProfile toEntity(CreateExpertProfileRequest request, UUID userId) {
		return ExpertProfile.builder()
			.userId(userId)
			.specialty(request.getSpecialty())
			.specialtyId(request.getSpecialtyId())
			.professionalTitle(request.getProfessionalTitle())
			.experienceYears(request.getExperienceYears())
			.workplace(request.getWorkplace())
			.hospitalId(request.getHospitalId())
			.consultationScope(request.getConsultationScope())
			.ratingAvg(request.getRatingAvg())
			.verificationStatus(VerificationStatus.PENDING)
			.build();
	}

	public void updateEntity(ExpertProfile entity, UpdateExpertProfileRequest request) {
		if (request.getSpecialty() != null) entity.setSpecialty(request.getSpecialty());
		if (request.getSpecialtyId() != null) entity.setSpecialtyId(request.getSpecialtyId());
		if (request.getProfessionalTitle() != null) entity.setProfessionalTitle(request.getProfessionalTitle());
		if (request.getExperienceYears() != null) entity.setExperienceYears(request.getExperienceYears());
		if (request.getWorkplace() != null) entity.setWorkplace(request.getWorkplace());
		if (request.getHospitalId() != null) entity.setHospitalId(request.getHospitalId());
		if (request.getConsultationScope() != null) entity.setConsultationScope(request.getConsultationScope());
		if (request.getRatingAvg() != null) entity.setRatingAvg(request.getRatingAvg());
	}

	// ADR-MEDI-001 mục 3 — usersById resolved by the caller via 1 batch userRepository.findAllById(...)
	// for the whole page; never queried per-row here.
	public ExpertDirectoryResponse toDirectoryResponse(
		Page<ExpertProfile> page, Map<UUID, User> usersById, List<String> specialties) {
		List<ExpertProfileResponse> experts = page.getContent().stream()
			.map(ep -> {
				User u = usersById.get(ep.getUserId());
				return toResponse(ep, u != null ? u.getName() : null, u != null ? u.getAvatarUrl() : null);
			})
			.collect(Collectors.toList());
		return new ExpertDirectoryResponse(
			experts,
			page.getNumber(),
			page.getSize(),
			page.getTotalElements(),
			page.getTotalPages(),
			specialties
		);
	}
}
