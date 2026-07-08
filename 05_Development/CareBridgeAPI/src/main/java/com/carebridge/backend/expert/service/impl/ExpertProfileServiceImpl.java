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
import com.carebridge.backend.expert.verificationstatus.VerificationStatus;
import com.carebridge.backend.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ExpertProfileServiceImpl implements IExpertProfileService {

    private final ExpertProfileRepository expertProfileRepository;
    private final UserRepository userRepository;
    private final ExpertProfileMapper expertProfileMapper;

    @Override
    public ExpertProfileResponse createProfile(UUID userId, CreateExpertProfileRequest request) {
        if (expertProfileRepository.existsByUserId(userId)) {
            throw new ExpertException(
                    org.springframework.http.HttpStatus.CONFLICT,
                    "EXPERT-001", "Expert profile already exists for this user");
        }
        ExpertProfile profile = expertProfileMapper.toEntity(request, userId);
        ExpertProfile saved = expertProfileRepository.save(profile);
        return expertProfileMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ExpertProfileDetailResponse getMyProfile(UUID userId) {
        ExpertProfile profile = expertProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ExpertException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "EXPERT-002", "Expert profile not found"));
        return expertProfileMapper.toDetailResponse(profile);
    }

    @Override
    public ExpertProfileDetailResponse updateProfile(UUID userId, UpdateExpertProfileRequest request) {
        ExpertProfile profile = expertProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ExpertException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "EXPERT-002", "Expert profile not found"));
        expertProfileMapper.updateEntity(profile, request);
        return expertProfileMapper.toDetailResponse(expertProfileRepository.save(profile));
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
                null,
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
    public ExpertDirectoryResponse getPublicDirectory(String specialty, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        Page<ExpertProfile> result;
        if (specialty != null && !specialty.isBlank()) {
            result = expertProfileRepository.findVerifiedBySpecialty(specialty)
                    .stream()
                    .map(p -> {
                        ExpertProfile ep = new ExpertProfile();
                        ep.setExpertProfileId(p.getExpertProfileId());
                        ep.setUserId(p.getUserId());
                        ep.setSpecialty(p.getSpecialty());
                        ep.setProfessionalTitle(p.getProfessionalTitle());
                        ep.setExperienceYears(p.getExperienceYears());
                        ep.setWorkplace(p.getWorkplace());
                        ep.setVerificationStatus(p.getVerificationStatus());
                        ep.setRatingAvg(p.getRatingAvg());
                        ep.setCreatedAt(p.getCreatedAt());
                        return ep;
                    })
                    .collect(Collectors.collectingAndThen(Collectors.toList(), PageImpl::new));
        } else {
            result = new PageImpl<>(
                    expertProfileRepository.findVerifiedPublic(), pageable, 0);
        }
        return expertProfileMapper.toDirectoryResponse(result);
    }

    @Override
    @Transactional(readOnly = true)
    public ExpertProfileDetailResponse getPublicProfile(UUID expertProfileId) {
        ExpertProfile profile = expertProfileRepository.findById(expertProfileId)
                .orElseThrow(() -> new ExpertException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "EXPERT-003", "Expert profile not found"));
        if (profile.getVerificationStatus() != VerificationStatus.APPROVED) {
            throw new ExpertException(
                    org.springframework.http.HttpStatus.NOT_FOUND,
                    "EXPERT-004", "Expert profile not available");
        }
        return expertProfileMapper.toDetailResponse(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpertProfileResponse> getVerifiedExperts() {
        return expertProfileRepository.findVerifiedPublic().stream()
                .map(expertProfileMapper::toResponse)
                .toList();
    }

    // ── UC-70: Admin approve / reject ──────────────────────────────────

    @Override
    public void approveExpert(UUID expertProfileId, UUID adminId) {
        ExpertProfile profile = expertProfileRepository.findById(expertProfileId)
                .orElseThrow(() -> new ExpertException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "EXPERT-003", "Expert profile not found"));
        profile.setVerificationStatus(VerificationStatus.APPROVED);
        profile.setVerifiedAt(LocalDateTime.now());
        profile.setVerifiedBy(adminId);
        expertProfileRepository.save(profile);
    }

    @Override
    public void rejectExpert(UUID expertProfileId, UUID adminId, String reason) {
        ExpertProfile profile = expertProfileRepository.findById(expertProfileId)
                .orElseThrow(() -> new ExpertException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "EXPERT-003", "Expert profile not found"));
        profile.setVerificationStatus(VerificationStatus.REJECTED);
        profile.setVerifiedAt(LocalDateTime.now());
        profile.setVerifiedBy(adminId);
        expertProfileRepository.save(profile);
    }

    // ── UC-71: Admin trust action ──────────────────────────────────────

    @Override
    public void setTrustStatus(UUID expertProfileId, VerificationStatus newStatus, UUID adminId) {
        ExpertProfile profile = expertProfileRepository.findById(expertProfileId)
                .orElseThrow(() -> new ExpertException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "EXPERT-003", "Expert profile not found"));
        profile.setVerificationStatus(newStatus);
        if (newStatus == VerificationStatus.SUSPENDED || newStatus == VerificationStatus.APPROVED) {
            profile.setVerifiedAt(LocalDateTime.now());
            profile.setVerifiedBy(adminId);
        }
        expertProfileRepository.save(profile);
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
