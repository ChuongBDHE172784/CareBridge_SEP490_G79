package com.carebridge.backend.expert.service.impl;

import com.carebridge.backend.expert.dto.request.CreateExpertProfileRequest;
import com.carebridge.backend.expert.dto.request.UpdateExpertProfileRequest;
import com.carebridge.backend.expert.dto.response.ExpertDirectoryResponse;
import com.carebridge.backend.expert.dto.response.ExpertProfileDetailResponse;
import com.carebridge.backend.expert.dto.response.ExpertProfileResponse;
import com.carebridge.backend.expert.dto.response.VerificationStatusResponse;
import com.carebridge.backend.expert.entity.ExpertProfile;
import com.carebridge.backend.expert.exception.ExpertException;
import com.carebridge.backend.expert.mapper.ExpertProfileMapper;
import com.carebridge.backend.expert.repository.ExpertProfileRepository;
import com.carebridge.backend.expert.service.IExpertProfileService;
import com.carebridge.backend.expert.truststatus.TrustStatus;
import com.carebridge.backend.expert.verificationstatus.VerificationStatus;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.repository.UserRepository;
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
			UserInfo info = resolveUserInfo(userId);
			return expertProfileMapper.toResponse(existing.get(), info.displayName(), info.avatarUrl());
		}
		ExpertProfile profile = expertProfileMapper.toEntity(request, userId);
		ExpertProfile saved = expertProfileRepository.save(profile);
		UserInfo info = resolveUserInfo(userId);
		return expertProfileMapper.toResponse(saved, info.displayName(), info.avatarUrl());
	}

	@Override
	@Transactional(readOnly = true)
	public ExpertProfileDetailResponse getMyProfile(UUID userId) {
		ExpertProfile profile = expertProfileRepository.findByUserId(userId)
			.orElseGet(() -> new ExpertProfile());
		UserInfo info = resolveUserInfo(userId);
		return expertProfileMapper.toDetailResponse(profile, info.displayName(), info.avatarUrl());
	}

	@Override
	public ExpertProfileDetailResponse updateProfile(UUID userId, UpdateExpertProfileRequest request) {
		ExpertProfile profile = expertProfileRepository.findByUserId(userId)
			.orElseThrow(() -> new ExpertException(
				org.springframework.http.HttpStatus.NOT_FOUND,
				"EXPERT-002", "Expert profile not found"));
		expertProfileMapper.updateEntity(profile, request);
		ExpertProfile saved = expertProfileRepository.save(profile);
		UserInfo info = resolveUserInfo(userId);
		return expertProfileMapper.toDetailResponse(saved, info.displayName(), info.avatarUrl());
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
			profile.getVerificationRejectionReason(),
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
		profile.setVerificationRejectionReason(null);
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
			.anyMatch(credential -> !"IDENTITY_DOCUMENT".equals(credential.getCredentialType())
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
		profile.setVerificationRejectionReason(null);
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
		if (profile.getVerificationStatus() == VerificationStatus.REJECTED
				&& reason.trim().equals(profile.getVerificationRejectionReason())) {
			return;
		}
		profile.setVerificationStatus(VerificationStatus.REJECTED);
		profile.setVerifiedAt(LocalDateTime.now());
		profile.setVerifiedBy(adminId);
		profile.setVerificationRejectionReason(reason.trim());
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
