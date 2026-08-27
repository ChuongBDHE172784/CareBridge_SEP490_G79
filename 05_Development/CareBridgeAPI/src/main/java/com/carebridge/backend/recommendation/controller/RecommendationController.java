package com.carebridge.backend.recommendation.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.recommendation.dto.RecommendationContentResponse;
import com.carebridge.backend.recommendation.dto.RecommendationProfileResponse;
import com.carebridge.backend.recommendation.service.RecommendationService;
import com.carebridge.backend.recommendation.exception.RecommendationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.Principal;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller quản lý tính năng Gợi ý bài viết y khoa (Article Recommendation)
 * và Hồ sơ khảo sát cá nhân hóa (Recommendation Profile) cho thai kỳ / sau sinh.
 * Base Path: `/api/v1/recommendations`
 */
@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
public class RecommendationController {
    private final RecommendationService recommendationService;
    private final ObjectMapper objectMapper;

    /**
     * [API GET /api/v1/recommendations/profile]
     * Lấy thông tin hồ sơ cá nhân hóa hiện tại của người mẹ.
     * Quyền truy cập: Chỉ dành cho người dùng có vai trò 'MOTHER'.
     * Trả về: Trạng thái hồ sơ (NOT_STARTED, ACTIVE, REVIEW_REQUIRED...), tóm tắt Consent và dữ liệu khảo sát.
     */
    @GetMapping("/profile")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<RecommendationProfileResponse>> getProfile(Principal principal) {
        // (1) Lấy User ID của người mẹ từ token xác thực
        UUID owner = SecurityUtils.requireCurrentUserId(principal);

        // (2) Gọi Service truy vấn hồ sơ và trả về kết quả
        return ResponseEntity.ok(ApiResponse.success(recommendationService.getProfile(owner)));
    }

    /**
     * [API PUT /api/v1/recommendations/profile]
     * Cập nhật khảo sát cá nhân hóa hoặc từ chối cấp quyền đồng ý (Consent).
     * Quyền truy cập: Chỉ dành cho vai trò 'MOTHER'.
     * Body: JSON chứa submissionId, policyVersion, consentAccepted và dữ liệu profile.
     */
    @PutMapping("/profile")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<RecommendationProfileResponse>> putProfile(
            @RequestBody Map<String, Object> request, Principal principal) {
        // (1) Lấy User ID của người mẹ đang đăng nhập
        UUID owner = SecurityUtils.requireCurrentUserId(principal);

        // (2) Chuyển đổi Request Body thành JSON Node và ủy quyền cho Service xử lý validate/lưu trữ
        return ResponseEntity.ok(ApiResponse.success(
                recommendationService.putProfile(owner, objectMapper.valueToTree(request))));
    }

    /**
     * [BƯỚC 1: TIẾP NHẬN REQUEST GỢI Ý BÀI VIẾT TỪ FRONTEND]
     * [API GET /api/v1/recommendations/content]
     * Lấy danh sách bài viết kiến thức y tế gợi ý theo tuần thai và hồ sơ sức khỏe.
     * Quyền truy cập: 'MOTHER' (người mẹ) hoặc 'FAMILY' (thành viên gia đình trong CareGroup).
     * 
     * @param rawLimit Số lượng bài viết cần lấy (mặc định là 10, từ 1 đến 10)
     * @param careGroupId ID nhóm gia đình (tùy chọn, dùng khi người xem là người nhà)
     * @return DTO RecommendationContentResponse chứa danh sách bài viết và chế độ gợi ý
     */
    @GetMapping("/content")
    @PreAuthorize("hasAnyRole('MOTHER', 'FAMILY')")
    public ResponseEntity<ApiResponse<RecommendationContentResponse>> getContent(
            @RequestParam(name = "limit", defaultValue = "10") String rawLimit,
            @RequestParam(name = "careGroupId", required = false) UUID careGroupId,
            Principal principal) {
        // (1) Parse và validate định dạng số nguyên cho tham số limit
        final int limit;
        try {
            limit = Integer.parseInt(rawLimit);
        } catch (NumberFormatException ex) {
            throw new RecommendationException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "RECOMMENDATION_LIMIT_INVALID", "limit must be an integer");
        }

        // (2) Lấy User ID của người đang thực hiện request từ token bảo mật
        UUID owner = SecurityUtils.requireCurrentUserId(principal);

        // (3) Chuyển tiếp sang RecommendationService để thực thi thuật toán xếp hạng và lọc bài viết
        return ResponseEntity.ok(ApiResponse.success(recommendationService.getContent(owner, careGroupId, limit)));
    }
}
