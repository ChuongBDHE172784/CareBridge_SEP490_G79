package com.carebridge.backend.expert.service.impl;

import com.carebridge.backend.expert.dto.request.CreateExpertProfileRequest;
import com.carebridge.backend.expert.dto.request.UpdateExpertProfileRequest;
import com.carebridge.backend.expert.dto.response.ExpertDirectoryResponse;
import com.carebridge.backend.expert.dto.response.ExpertProfileDetailResponse;
import com.carebridge.backend.expert.dto.response.ExpertProfileResponse;
import com.carebridge.backend.expert.dto.response.VerificationStatusResponse;
import com.carebridge.backend.expert.entity.ExpertProfile;
import com.carebridge.backend.expert.entity.ProfessionalSpecialty;
import com.carebridge.backend.expert.exception.ExpertException;
import com.carebridge.backend.expert.mapper.ExpertProfileMapper;
import com.carebridge.backend.expert.repository.ExpertProfileRepository;
import com.carebridge.backend.expert.repository.ProfessionalSpecialtyRepository;
import com.carebridge.backend.expert.service.IExpertProfileService;
import com.carebridge.backend.expert.truststatus.TrustStatus;
import com.carebridge.backend.expert.verificationstatus.VerificationStatus;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.masterdata.repository.SpecialtyRepository;
import com.carebridge.backend.map.entity.CareFacility;
import com.carebridge.backend.map.repository.CareFacilityRepository;
import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.expertverification.enums.IdentityReviewStatus;
import com.carebridge.backend.expertverification.repository.ExpertCredentialRepository;
import com.carebridge.backend.expertverification.repository.ExpertIdentityVerificationRepository;
import com.carebridge.backend.expertverification.reviewstatus.ReviewStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ExpertProfileServiceImpl implements IExpertProfileService {

	private final ExpertProfileRepository expertProfileRepository;
	private final UserRepository userRepository;
	private final ExpertProfileMapper expertProfileMapper;
	private final ExpertIdentityVerificationRepository identityVerificationRepository;
	private final ExpertCredentialRepository credentialRepository;
	private final AuditService auditService;
	private final SpecialtyRepository specialtyRepository;
	private final CareFacilityRepository careFacilityRepository;
	private final ProfessionalSpecialtyRepository professionalSpecialtyRepository;

	// ADR-MEDI-001 mục 4 — displayName resolved alongside avatarUrl from the same users row,
	// 1 lookup, for every response that uses ExpertProfileResponse/ExpertProfileDetailResponse.
	private record UserInfo(String displayName, String avatarUrl) {}

	private UserInfo resolveUserInfo(UUID userId) {
		return userRepository.findById(userId)
			.map(u -> new UserInfo(u.getName(), u.getAvatarUrl()))
			.orElse(new UserInfo(null, null));
	}

	@Override
	public ExpertProfileResponse createProfile(UUID userId, CreateExpertProfileRequest request) {
		var existing = expertProfileRepository.findByUserId(userId);
		if (existing.isPresent()) {
			List<ProfessionalSpecialty> mappings = professionalSpecialtyRepository
				.findByProfessionalProfileIdOrderByPrimaryDesc(existing.get().getExpertProfileId());
			if (mappings.isEmpty()) {
				MasterDataSelection selection = normalizeMasterData(request);
				synchronizeSpecialties(existing.get().getExpertProfileId(), selection.specialties());
				mappings = professionalSpecialtyRepository
					.findByProfessionalProfileIdOrderByPrimaryDesc(existing.get().getExpertProfileId());
			}
			UserInfo info = resolveUserInfo(userId);
			ExpertProfileResponse response =
				expertProfileMapper.toResponse(existing.get(), info.displayName(), info.avatarUrl());
			applySpecialties(response, mappings);
			return response;
		}
		MasterDataSelection selection = normalizeMasterData(request);
		ExpertProfile profile = expertProfileMapper.toEntity(request, userId);
		profile.setFacilityId(selection.facilityId());
		ExpertProfile saved = expertProfileRepository.save(profile);
		synchronizeSpecialties(saved.getExpertProfileId(), selection.specialties());
		UserInfo info = resolveUserInfo(userId);
		ExpertProfileResponse response = expertProfileMapper.toResponse(saved, info.displayName(), info.avatarUrl());
		List<String> specialtyIds = selection.specialties().stream()
			.map(specialty -> specialty.getSpecialtyId().toString())
			.toList();
		response.setSpecialtyIds(specialtyIds);
		response.setSpecialtyId(specialtyIds.getFirst());
		return response;
	}

	@Override
	@Transactional(readOnly = true)
	public ExpertProfileDetailResponse getMyProfile(UUID userId) {
		ExpertProfile profile = expertProfileRepository.findByUserId(userId)
			.orElseGet(() -> new ExpertProfile());
		UserInfo info = resolveUserInfo(userId);
		ExpertProfileDetailResponse response =
			expertProfileMapper.toDetailResponse(profile, info.displayName(), info.avatarUrl());
		List<ProfessionalSpecialty> mappings = profile.getExpertProfileId() == null
			? List.of()
			: professionalSpecialtyRepository
				.findByProfessionalProfileIdOrderByPrimaryDesc(profile.getExpertProfileId());
		applySpecialties(response, mappings);
		return response;
	}

	@Override
	public ExpertProfileDetailResponse updateProfile(UUID userId, UpdateExpertProfileRequest request) {
		ExpertProfile profile = expertProfileRepository.findByUserIdForUpdate(userId)
			.orElseThrow(() -> new ExpertException(
				org.springframework.http.HttpStatus.NOT_FOUND,
				"EXPERT-002", "Expert profile not found"));
		MasterDataSelection selection = normalizeMasterData(request);
		expertProfileMapper.updateEntity(profile, request);
		if (request.getHospitalId() != null) {
			profile.setFacilityId(selection.facilityId());
		}
		ExpertProfile saved = expertProfileRepository.save(profile);
		if (!selection.specialties().isEmpty()) {
			synchronizeSpecialties(saved.getExpertProfileId(), selection.specialties());
		}
		UserInfo info = resolveUserInfo(userId);
		ExpertProfileDetailResponse response =
			expertProfileMapper.toDetailResponse(saved, info.displayName(), info.avatarUrl());
		if (!selection.specialties().isEmpty()) {
			List<String> specialtyIds = selection.specialties().stream()
				.map(specialty -> specialty.getSpecialtyId().toString())
				.toList();
			response.setSpecialtyIds(specialtyIds);
			response.setSpecialtyId(specialtyIds.getFirst());
		}
		return response;
	}

	private void applySpecialties(ExpertProfileResponse response, List<ProfessionalSpecialty> mappings) {
		List<String> ids = mappings.stream()
			.map(mapping -> mapping.getSpecialtyId().toString())
			.toList();
		response.setSpecialtyIds(ids);
		response.setSpecialtyId(ids.isEmpty() ? null : ids.getFirst());
	}

	private void applySpecialties(ExpertProfileDetailResponse response, List<ProfessionalSpecialty> mappings) {
		List<String> ids = mappings.stream()
			.map(mapping -> mapping.getSpecialtyId().toString())
			.toList();
		response.setSpecialtyIds(ids);
		response.setSpecialtyId(ids.isEmpty() ? null : ids.getFirst());
	}

	// ── UC-63: View Verification Status & Renew ────────────────────────

	@Override
	@Transactional(readOnly = true)
	public VerificationStatusResponse getMyVerificationStatus(UUID userId) {
		ExpertProfile profile = expertProfileRepository.findByUserId(userId)
			.orElseThrow(() -> new ExpertException(
				org.springframework.http.HttpStatus.NOT_FOUND,
				"EXPERT-002", "Expert profile not found"));

		boolean canRenew = profile.getVerificationStatus() == VerificationStatus.REJECTED
			|| profile.getVerificationStatus() == VerificationStatus.EXPIRED;
		String nextStep = switch (profile.getVerificationStatus()) {
			case PENDING, UNDER_REVIEW -> "Đang chờ xét duyệt";
			case APPROVED -> "Đã được phê duyệt";
			case REJECTED, EXPIRED -> "Vui lòng gửi lại hồ sơ";
			case SUSPENDED -> "Tài khoản đã bị tạm ngưng — liên hệ quản trị viên";
		};

		return new VerificationStatusResponse(
			profile.getVerificationStatus(),
			profile.getVerifiedAt(),
			profile.getVerifiedBy(),
			findLatestRejectionReason(profile.getExpertProfileId()),
			canRenew,
			nextStep
		);
	}

	@Override
	public void renewVerification(UUID userId) {
		ExpertProfile profile = expertProfileRepository.findByUserId(userId)
			.orElseThrow(() -> new ExpertException(
				org.springframework.http.HttpStatus.NOT_FOUND,
				"EXPERT-002", "Expert profile not found"));

		if (profile.getVerificationStatus() != VerificationStatus.REJECTED
			&& profile.getVerificationStatus() != VerificationStatus.EXPIRED) {
			throw new ExpertException(
				org.springframework.http.HttpStatus.BAD_REQUEST, "EXPERT-006",
				"Không thể gia hạn ở trạng thái hiện tại");
		}

		profile.setVerificationStatus(VerificationStatus.PENDING);
		profile.setVerifiedAt(null);
		profile.setVerifiedBy(null);
		expertProfileRepository.save(profile);
	}

	// ── UC-65: Public directory ────────────────────────────────────────

	@Override
	@Transactional(readOnly = true)
	public ExpertDirectoryResponse getPublicDirectory(String specialty, String q, int page, int size) {
		Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
		Page<ExpertProfile> result = expertProfileRepository.searchDirectory(
				blankToNull(specialty), blankToNull(q), pageable);
		Set<UUID> userIds = result.getContent().stream().map(ExpertProfile::getUserId).collect(Collectors.toSet());
		Map<UUID, User> usersById = userIds.isEmpty() ? Map.of()
				: userRepository.findAllById(userIds).stream().collect(Collectors.toMap(User::getId, u -> u));
		return expertProfileMapper.toDirectoryResponse(
				result, usersById, expertProfileRepository.findApprovedSpecialties());
	}

	private static String blankToNull(String s) {
		return (s == null || s.isBlank()) ? null : s.trim();
	}

	private record MasterDataSelection(UUID facilityId,
		List<com.carebridge.backend.masterdata.entity.Specialty> specialties) {}

	private MasterDataSelection normalizeMasterData(CreateExpertProfileRequest request) {
		var specialties = resolveActiveSpecialties(request.getSpecialtyId(), request.getSpecialtyIds());
		var hospital = findActiveFacility(request.getHospitalId())
			.orElseThrow(() -> new ExpertException(org.springframework.http.HttpStatus.BAD_REQUEST,
				"EXPERT-HOSPITAL-INVALID", "Cơ sở y tế không hợp lệ hoặc đã ngừng sử dụng"));
		request.setSpecialty(specialties.getFirst().getName());
		request.setWorkplace(hospital.getName());
		return new MasterDataSelection(hospital.getFacilityId(), specialties);
	}

	private MasterDataSelection normalizeMasterData(UpdateExpertProfileRequest request) {
		UUID facilityId = null;
		List<com.carebridge.backend.masterdata.entity.Specialty> specialties = List.of();
		if (request.getSpecialtyId() != null
				|| (request.getSpecialtyIds() != null && !request.getSpecialtyIds().isEmpty())) {
			specialties = resolveActiveSpecialties(request.getSpecialtyId(), request.getSpecialtyIds());
			request.setSpecialty(specialties.getFirst().getName());
		}
		if (request.getHospitalId() != null) {
			var hospital = findActiveFacility(request.getHospitalId())
				.orElseThrow(() -> new ExpertException(org.springframework.http.HttpStatus.BAD_REQUEST,
					"EXPERT-HOSPITAL-INVALID", "Cơ sở y tế không hợp lệ hoặc đã ngừng sử dụng"));
			request.setWorkplace(hospital.getName());
			facilityId = hospital.getFacilityId();
		}
		return new MasterDataSelection(facilityId, specialties);
	}

	private List<com.carebridge.backend.masterdata.entity.Specialty> resolveActiveSpecialties(
			String primaryIdentifier, List<String> additionalIdentifiers) {
		List<String> identifiers = new ArrayList<>();
		if (primaryIdentifier != null && !primaryIdentifier.isBlank()) {
			identifiers.add(primaryIdentifier);
		}
		if (additionalIdentifiers != null) {
			identifiers.addAll(additionalIdentifiers);
		}
		if (identifiers.isEmpty()) {
			throw new ExpertException(org.springframework.http.HttpStatus.BAD_REQUEST,
				"EXPERT-SPECIALTY-INVALID", "Phải chọn ít nhất một chuyên khoa");
		}

		Map<UUID, com.carebridge.backend.masterdata.entity.Specialty> unique = new LinkedHashMap<>();
		for (String identifier : identifiers) {
			var specialty = specialtyRepository.findByIdentifier(identifier)
				.filter(item -> Boolean.TRUE.equals(item.getIsActive()))
				.orElseThrow(() -> new ExpertException(org.springframework.http.HttpStatus.BAD_REQUEST,
					"EXPERT-SPECIALTY-INVALID", "Chuyên khoa không hợp lệ hoặc đã ngừng sử dụng"));
			unique.putIfAbsent(specialty.getSpecialtyId(), specialty);
		}
		if (unique.size() > 20) {
			throw new ExpertException(org.springframework.http.HttpStatus.BAD_REQUEST,
				"EXPERT-SPECIALTY-LIMIT", "Chỉ được chọn tối đa 20 chuyên khoa");
		}
		return List.copyOf(unique.values());
	}

	private void synchronizeSpecialties(UUID expertProfileId,
			List<com.carebridge.backend.masterdata.entity.Specialty> specialties) {
		professionalSpecialtyRepository.deleteByProfessionalProfileId(expertProfileId);
		OffsetDateTime now = OffsetDateTime.now();
		List<ProfessionalSpecialty> mappings = new ArrayList<>(specialties.size());
		for (int index = 0; index < specialties.size(); index++) {
			mappings.add(ProfessionalSpecialty.builder()
				.professionalProfileId(expertProfileId)
				.specialtyId(specialties.get(index).getSpecialtyId())
				.primary(index == 0)
				.createdAt(now)
				.build());
		}
		professionalSpecialtyRepository.saveAll(mappings);
	}

	private java.util.Optional<CareFacility> findActiveFacility(String identifier) {
		try {
			return careFacilityRepository.findByFacilityIdAndActiveTrue(UUID.fromString(identifier));
		} catch (IllegalArgumentException ignored) {
			return careFacilityRepository.findByExternalSourceIdAndActiveTrue(identifier);
		}
	}

	private String findLatestRejectionReason(UUID expertProfileId) {
		if (expertProfileId == null) {
			return null;
		}
		return expertProfileRepository.findLatestProfileRejectionReason(expertProfileId).orElse(null);
	}

	@Override
	@Transactional(readOnly = true)
	public ExpertProfileDetailResponse getPublicProfile(UUID expertProfileId) {
		ExpertProfile profile = expertProfileRepository.findById(expertProfileId)
			.orElseThrow(() -> new ExpertException(
				org.springframework.http.HttpStatus.NOT_FOUND,
				"EXPERT-003", "Expert profile not found"));
		if (!profile.isEligibleForConsultation()) {
			throw new ExpertException(
				org.springframework.http.HttpStatus.NOT_FOUND,
				"EXPERT-004", "Expert profile not available");
		}
		UserInfo info = resolveUserInfo(profile.getUserId());
		return expertProfileMapper.toDetailResponse(profile, info.displayName(), info.avatarUrl());
	}

	@Override
	@Transactional(readOnly = true)
	public List<ExpertProfileResponse> getVerifiedExperts() {
		return expertProfileRepository.findVerifiedPublic().stream()
			.map(p -> {
				UserInfo info = resolveUserInfo(p.getUserId());
				return expertProfileMapper.toResponse(p, info.displayName(), info.avatarUrl());
			})
			.toList();
	}

	// ── UC-70: Admin approve / reject ──────────────────────────────────

	@Override
	public void approveExpert(UUID expertProfileId, UUID adminId) {
		ExpertProfile profile = expertProfileRepository.findByIdForUpdate(expertProfileId)
			.orElseThrow(() -> new ExpertException(
				org.springframework.http.HttpStatus.NOT_FOUND,
				"EXPERT-003", "Expert profile not found"));
		if (profile.getVerificationStatus() == VerificationStatus.APPROVED) {
			return;
		}
		var latestIdentity = identityVerificationRepository
			.findFirstByExpertProfileIdOrderByCreatedAtDesc(expertProfileId)
			.orElseThrow(() -> new ExpertException(
				org.springframework.http.HttpStatus.CONFLICT, "EXPERT-IDENTITY-REQUIRED",
				"Approved identity evidence is required"));
		if (latestIdentity.getReviewStatus() != IdentityReviewStatus.APPROVED) {
			throw new ExpertException(
				org.springframework.http.HttpStatus.CONFLICT, "EXPERT-IDENTITY-NOT-APPROVED",
				"The latest identity verification must be approved");
		}
		boolean hasApprovedProfessionalCredential = credentialRepository
			.findByExpertProfileIdAndReviewStatus(expertProfileId, ReviewStatus.APPROVED)
			.stream()
			.anyMatch(credential -> !credential.getCredentialType().startsWith("IDENTITY_")
				&& (credential.getExpiryDate() == null
					|| !credential.getExpiryDate().isBefore(java.time.LocalDate.now())));
		if (!hasApprovedProfessionalCredential) {
			throw new ExpertException(
				org.springframework.http.HttpStatus.CONFLICT, "EXPERT-CREDENTIAL-REQUIRED",
				"At least one current approved professional credential is required");
		}
		profile.setVerificationStatus(VerificationStatus.APPROVED);
		profile.setVerifiedAt(LocalDateTime.now());
		profile.setVerifiedBy(adminId);
		expertProfileRepository.save(profile);
		auditService.log(AuditAction.EXPERT_VERIFICATION, adminId,
			"ExpertProfile", expertProfileId.toString(),
			Map.of("event", "FINAL_DECISION", "decision", "APPROVED"));
	}

	@Override
	public void rejectExpert(UUID expertProfileId, UUID adminId, String reason) {
		ExpertProfile profile = expertProfileRepository.findByIdForUpdate(expertProfileId)
			.orElseThrow(() -> new ExpertException(
				org.springframework.http.HttpStatus.NOT_FOUND,
				"EXPERT-003", "Expert profile not found"));
		if (reason == null || reason.isBlank()) {
			throw new ExpertException(
				org.springframework.http.HttpStatus.BAD_REQUEST, "EXPERT-REJECTION-REASON-REQUIRED",
				"An actionable rejection reason is required");
		}
		if (profile.getVerificationStatus() == VerificationStatus.REJECTED) {
			return;
		}
		profile.setVerificationStatus(VerificationStatus.REJECTED);
		profile.setVerifiedAt(LocalDateTime.now());
		profile.setVerifiedBy(adminId);
		expertProfileRepository.save(profile);
		auditService.log(AuditAction.EXPERT_VERIFICATION, adminId,
			"ExpertProfile", expertProfileId.toString(),
			Map.of("event", "FINAL_DECISION", "decision", "REJECTED",
				"reason", reason.trim()));
	}

	// ── UC-71: Admin trust action ──────────────────────────────────────

	@Override
	public void setTrustStatus(UUID expertProfileId, TrustStatus newStatus, UUID adminId) {
		ExpertProfile profile = expertProfileRepository.findByIdForUpdate(expertProfileId)
			.orElseThrow(() -> new ExpertException(
				org.springframework.http.HttpStatus.NOT_FOUND,
				"EXPERT-003", "Expert profile not found"));
		profile.setTrustStatus(newStatus);
		if (newStatus == TrustStatus.SUSPENDED || newStatus == TrustStatus.ACTIVE) {
			profile.setVerifiedAt(LocalDateTime.now());
			profile.setVerifiedBy(adminId);
		}
		expertProfileRepository.save(profile);
	}

	@Override
	public List<ExpertProfileResponse> getAllExperts() {
		return expertProfileRepository.findAll().stream()
			.map(p -> {
				UserInfo info = resolveUserInfo(p.getUserId());
				return expertProfileMapper.toResponse(p, info.displayName(), info.avatarUrl());
			})
			.toList();
	}

	// ── Shop / customisation ───────────────────────────────────────────

	@Override
	public void saveCategorySelection(UUID expertProfileId, List<String> categoryIds) {
		ExpertProfile profile = expertProfileRepository.findById(expertProfileId)
			.orElseThrow(() -> new ExpertException(
				org.springframework.http.HttpStatus.NOT_FOUND, "EXPERT-003", "Expert profile not found"));
		profile.setConsultationScope(java.util.Objects.toString(categoryIds));
		expertProfileRepository.save(profile);
	}

	@Override
	public void saveTitleAndPrice(UUID expertProfileId, String customTitle, int customPrice) {
		ExpertProfile profile = expertProfileRepository.findById(expertProfileId)
			.orElseThrow(() -> new ExpertException(
				org.springframework.http.HttpStatus.NOT_FOUND, "EXPERT-003", "Expert profile not found"));
		profile.setProfessionalTitle(customTitle);
		profile.setRatingAvg(java.math.BigDecimal.valueOf(customPrice));
		expertProfileRepository.save(profile);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Map<String, Object>> getConsultationHistory(UUID expertId) {
		return List.of();
	}
}
