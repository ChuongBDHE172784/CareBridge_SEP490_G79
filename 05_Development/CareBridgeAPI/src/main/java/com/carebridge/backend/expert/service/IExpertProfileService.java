package com.carebridge.backend.expert.service;

import com.carebridge.backend.expert.dto.request.CreateExpertProfileRequest;
import com.carebridge.backend.expert.dto.request.UpdateExpertProfileRequest;
import com.carebridge.backend.expert.dto.response.ExpertDirectoryResponse;
import com.carebridge.backend.expert.dto.response.ExpertProfileDetailResponse;
import com.carebridge.backend.expert.dto.response.ExpertProfileResponse;
import com.carebridge.backend.expert.dto.response.VerificationStatusResponse;
import com.carebridge.backend.expert.truststatus.TrustStatus;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface IExpertProfileService {

    // ── UC-60 ──────────────────────────────────────────────────────────
    ExpertProfileResponse createProfile(UUID userId, CreateExpertProfileRequest request);

    // ── UC-61 ──────────────────────────────────────────────────────────
    ExpertProfileDetailResponse getMyProfile(UUID userId);

    ExpertProfileDetailResponse updateProfile(UUID userId, UpdateExpertProfileRequest request);

    // ── UC-63: View Verification Status & Renew ────────────────────────
    VerificationStatusResponse getMyVerificationStatus(UUID userId);

    void renewVerification(UUID userId);

    // ── UC-65 ──────────────────────────────────────────────────────────
    ExpertDirectoryResponse getPublicDirectory(String specialty, int page, int size);

    ExpertProfileDetailResponse getPublicProfile(UUID expertProfileId);

    List<ExpertProfileResponse> getVerifiedExperts();

    // ── UC-70: Admin approve / reject ──────────────────────────────────
    void approveExpert(UUID expertProfileId, UUID adminId);

    void rejectExpert(UUID expertProfileId, UUID adminId, String reason);

    // ── UC-71: Admin trust action (restrict/suspend/reinstate) ─────────
    void setTrustStatus(UUID expertProfileId, TrustStatus newStatus, UUID adminId);

    // ── UC-71: Admin list all experts ──────────────────────────────────
List<ExpertProfileResponse> getAllExperts();

// ── Shop / customisation ───────────────────────────────────────────
    void saveCategorySelection(UUID expertProfileId, List<String> categoryIds);

    void saveTitleAndPrice(UUID expertProfileId, String customTitle, int customPrice);

    List<Map<String, Object>> getConsultationHistory(UUID expertId);
}
