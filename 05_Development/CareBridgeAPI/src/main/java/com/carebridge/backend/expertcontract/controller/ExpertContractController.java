package com.carebridge.backend.expertcontract.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.expertcontract.service.ExpertContractService;
import com.carebridge.backend.expertcontract.service.ExpertContractService.AcceptRequest;
import com.carebridge.backend.expertcontract.service.ExpertContractService.Acceptance;
import com.carebridge.backend.expertcontract.service.ExpertContractService.Offer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/expert/contract")
@RequiredArgsConstructor
public class ExpertContractController {

    private final ExpertContractService contractService;

    /** Bản đề nghị hợp tác — chỉ trả khi hồ sơ đã được duyệt và đang ở PENDING_CONTRACT. */
    @GetMapping("/offer")
    @PreAuthorize("hasRole('EXPERT')")
    public ResponseEntity<ApiResponse<Offer>> getOffer(Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(contractService.getOffer(userId)));
    }

    /** Admin xem trước toàn văn thoả thuận sẽ phát hành cho một chuyên gia cụ thể. */
    @GetMapping("/{expertProfileId}/preview")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Offer>> previewForExpert(
            @PathVariable UUID expertProfileId) {
        return ResponseEntity.ok(ApiResponse.success(contractService.previewFor(expertProfileId)));
    }

    /** Chấp nhận thoả thuận (click-wrap). IP/User-Agent đọc từ request, không nhận từ client. */
    @PostMapping("/accept")
    @PreAuthorize("hasRole('EXPERT')")
    public ResponseEntity<ApiResponse<Acceptance>> accept(
            @Valid @RequestBody AcceptRequest request,
            HttpServletRequest httpRequest,
            Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        Acceptance response = contractService.accept(
                userId, request, clientIp(httpRequest), httpRequest.getHeader("User-Agent"));
        return ResponseEntity.ok(ApiResponse.success(response, "Đã ghi nhận chấp nhận Thoả thuận"));
    }

    /**
     * Ứng dụng chạy sau reverse proxy (server.forward-headers-strategy=framework), nên IP thật
     * nằm ở X-Forwarded-For; getRemoteAddr() lúc đó chỉ là IP của proxy.
     */
    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
