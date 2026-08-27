package com.carebridge.backend.journey.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.journey.dto.CreateJourneyRequest;
import com.carebridge.backend.journey.dto.CreateJourneyResponse;
import com.carebridge.backend.journey.dto.JourneyDashboardResponse;
import com.carebridge.backend.journey.dto.JourneyResponse;
import com.carebridge.backend.journey.dto.JourneyTransitionPageResponse;
import com.carebridge.backend.journey.dto.UpdateJourneyRequest;
import com.carebridge.backend.journey.dto.RecordPregnancyOutcomeRequest;
import com.carebridge.backend.journey.dto.PregnancyOutcomeResponse;
import com.carebridge.backend.journey.service.IJourneyService;
import com.carebridge.backend.journey.service.IJourneyTransitionService;
import com.carebridge.backend.journey.service.IJourneyTimelineService;
import com.carebridge.backend.journey.dto.JourneyTimelinePageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.security.Principal;
import java.util.UUID;

/**
 * =========================================================================================
 * REST CONTROLLER: QUẢN LÝ HÀNH TRÌNH MẸ & TÍNH TOÁN TUẦN THAI (MOTHER JOURNEY
 * CONTROLLER)
 * =========================================================================================
 * 
 * Cung cấp các RESTful API phục vụ:
 * 1. UC22: Khởi tạo hành trình mẹ bầu & thiết lập ngày thai kỳ ban đầu (LMP /
 * EDD).
 * 2. UC23: Cập nhật thông tin hành trình & hiệu chỉnh lại tuổi thai.
 * 3. UC24: Xem Dashboard tổng quan thai kỳ (tuần thai, ngày dự sinh, tam cá
 * nguyệt, WHO Plan).
 * 4. Ghi nhận kết quả thai kỳ (Pregnancy Outcomes), lịch sử chuyển đổi và dòng
 * thời gian (Timeline).
 */
@RestController
@RequestMapping("/api/v1/journeys")
@RequiredArgsConstructor
@Validated
public class JourneyController {

    private final IJourneyService journeyService;
    private final IJourneyTransitionService journeyTransitionService;
    private final IJourneyTimelineService journeyTimelineService;

    /**
     * [UC22: Tạo mới Hành trình Mẹ & Thiết lập tuổi thai]
     * 
     * Endpoint: `POST /api/v1/journeys`
     * Quyền hạn: Người dùng đã đăng nhập (Role: MOTHER)
     * 
     * Dữ liệu nhận:
     * - Body: `CreateJourneyRequest` (chứa `journeyType`, `lastMenstrualDate`,
     * `estimatedDueDate`, `datingBasis`...)
     * - Header: `X-Checklist-Contract-Version` (1 hoặc 2)
     * 
     * @param request         Dữ liệu khởi tạo hành trình
     * @param contractVersion Phiên bản hợp đồng Checklist/Dating
     * @param principal       Thông tin xác thực của người dùng
     * @return CreateJourneyResponse chứa kết quả tính tuần thai và id hành trình
     *         vừa tạo
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CreateJourneyResponse>> createJourney(
            @Valid @RequestBody CreateJourneyRequest request,
            @RequestHeader(name = "X-Checklist-Contract-Version", required = false) String contractVersion,
            Principal principal) {
        // [BƯỚC 1: Tiếp nhận Request & Phân tích phiên bản hợp đồng]
        request.setChecklistContractVersion(parseContractVersion(contractVersion));
        var callerId = SecurityUtils.requireCurrentUserId(principal);

        // [BƯỚC 2..5: Gọi Service xử lý tính toán tuổi thai và lưu DB]
        var response = journeyService.createJourney(request, callerId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Journey created successfully"));
    }

    /**
     * [UC23: Cập nhật Hành trình Mẹ & Điều chỉnh ngày thai kỳ]
     * 
     * Endpoint: `PUT /api/v1/journeys/{journeyId}`
     * Quyền hạn: Vai trò MOTHER sở hữu hành trình
     * 
     * @param journeyId       ID hành trình cần cập nhật
     * @param request         Dữ liệu cập nhật mới (LMP, EDD, ghi chú, trạng
     *                        thái...)
     * @param contractVersion Phiên bản hợp đồng API
     * @param principal       Thông tin người dùng gọi API
     * @return JourneyResponse chứa tuần thai đã được tính toán lại
     */
    @PutMapping("/{journeyId}")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<JourneyResponse>> updateJourney(
            @PathVariable UUID journeyId,
            @Valid @RequestBody UpdateJourneyRequest request,
            @RequestHeader(name = "X-Checklist-Contract-Version", required = false) String contractVersion,
            Principal principal) {
        // [BƯỚC 1: Tiếp nhận Request & Phân quyền]
        request.setChecklistContractVersion(parseContractVersion(contractVersion));
        var ownerId = SecurityUtils.requireCurrentUserId(principal);

        // [BƯỚC 2..5: Gọi Service cập nhật và tính toán lại tuổi thai]
        var response = journeyService.updateJourney(ownerId, journeyId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Journey updated successfully"));
    }

    /**
     * [Ghi nhận kết quả thai kỳ (Pregnancy Outcomes)]
     * Endpoint: `POST /api/v1/journeys/{journeyId}/pregnancy-outcomes`
     */
    @PostMapping("/{journeyId}/pregnancy-outcomes")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<PregnancyOutcomeResponse>> recordPregnancyOutcome(
            @PathVariable UUID journeyId,
            @Valid @RequestBody RecordPregnancyOutcomeRequest request,
            Principal principal) {
        var ownerId = SecurityUtils.requireCurrentUserId(principal);
        var response = journeyTransitionService.recordPregnancyOutcome(
                ownerId, journeyId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Pregnancy outcome recorded"));
    }

    /**
     * [Xem Lịch sử chuyển đổi giai đoạn hành trình]
     * Endpoint: `GET /api/v1/journeys/{journeyId}/history`
     */
    @GetMapping("/{journeyId}/history")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<JourneyTransitionPageResponse>> getHistory(
            @PathVariable UUID journeyId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            Principal principal) {
        var ownerId = SecurityUtils.requireCurrentUserId(principal);
        var history = journeyService.getHistory(
                ownerId, journeyId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    /**
     * [Xem Dòng thời gian các sự kiện quan trọng (Timeline)]
     * Endpoint: `GET /api/v1/journeys/{journeyId}/timeline`
     */
    @GetMapping("/{journeyId}/timeline")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<JourneyTimelinePageResponse>> getTimeline(
            @PathVariable UUID journeyId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            Principal principal) {
        var ownerId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(journeyTimelineService.getTimeline(
                ownerId, journeyId, PageRequest.of(page, size))));
    }

    /**
     * [UC24: Xem Dashboard Hành Trình & Tuần Thai Hiện Tại]
     * 
     * Endpoint: `GET /api/v1/journeys/me/dashboard`
     * Quyền hạn: MOTHER hoặc FAMILY (thành viên nhóm gia đình được chia sẻ)
     * Đặc thù bảo mật: Sử dụng `/me` pattern để ngăn chặn lỗ hổng IDOR
     * (ADR-JOURNEY-003-003).
     * 
     * Output:
     * - `pregnancyWeek`: Tuần thai hiện tại (1-based: ví dụ tuần 12)
     * - `completedGestationalWeek` & `completedGestationalDays`: Số tuần và ngày lẻ
     * (ví dụ: 11 tuần 4 ngày)
     * - `trimester`: Tam cá nguyệt 1, 2, hoặc 3
     * - `daysUntilDue`: Số ngày đếm ngược đến ngày dự sinh
     * - `plan`: Kế hoạch khám thai 8 giai đoạn theo WHO
     * 
     * @param principal Thông tin tài khoản người dùng
     * @return JourneyDashboardResponse chứa đầy đủ các chỉ số thai kỳ
     */
    @GetMapping("/me/dashboard")
    @PreAuthorize("hasAnyRole('MOTHER', 'FAMILY')")
    public ResponseEntity<ApiResponse<JourneyDashboardResponse>> getDashboard(Principal principal) {
        // [BƯỚC 1: Tiếp nhận Request] Lấy userId từ JWT Principal
        var userId = SecurityUtils.requireCurrentUserId(principal);

        // [BƯỚC 2..5: Gọi Service truy vấn hành trình và tính toán toàn bộ tuổi thai]
        var dashboard = journeyService.getDashboard(userId);
        return ResponseEntity.ok(ApiResponse.success(dashboard));
    }

    // [Private Helper] Đọc và chuyển đổi giá trị Header Contract Version (mặc định
    // là phiên bản 1)
    private Integer parseContractVersion(String raw) {
        if (raw == null)
            return 1;
        if (raw.isBlank()) {
            throw unsupportedContractVersion();
        }
        try {
            int value = Integer.parseInt(raw.trim());
            if (value == 1 || value == 2)
                return value;
        } catch (NumberFormatException ignored) {
            // Sử dụng lỗi chuẩn BusinessException bên dưới
        }
        throw unsupportedContractVersion();
    }

    // [Private Helper] Ném lỗi nghiệp vụ khi phiên bản Header hợp đồng không được
    // hỗ trợ
    private BusinessException unsupportedContractVersion() {
        return new BusinessException(
                HttpStatus.BAD_REQUEST,
                "CHECKLIST_CONTRACT_VERSION_UNSUPPORTED",
                "Unsupported checklist contract version");
    }
}
