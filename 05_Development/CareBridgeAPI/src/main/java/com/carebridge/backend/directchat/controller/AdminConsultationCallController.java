package com.carebridge.backend.directchat.controller;

import com.carebridge.backend.common.constants.AppConstants;
import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.response.PaginatedResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.directchat.dto.request.AdminConsultationCallSearchQuery;
import com.carebridge.backend.directchat.dto.response.AdminConsultationCallSummaryResponse;
import com.carebridge.backend.directchat.entity.CallStatus;
import com.carebridge.backend.directchat.entity.CallType;
import com.carebridge.backend.directchat.service.IAdminConsultationCallService;
import java.security.Principal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/consultation-calls")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN')")
public class AdminConsultationCallController {

    private final IAdminConsultationCallService adminCallService;

    @GetMapping
    public ResponseEntity<PaginatedResponse<AdminConsultationCallSummaryResponse>> searchCalls(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) CallType callType,
            @RequestParam(required = false) CallStatus callStatus,
            @RequestParam(required = false) Boolean hasRecording,
            @RequestParam(required = false) Instant fromDate,
            @RequestParam(required = false) Instant toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        AdminConsultationCallSearchQuery query = new AdminConsultationCallSearchQuery();
        query.setKeyword(keyword);
        query.setCallType(callType);
        query.setCallStatus(callStatus);
        query.setHasRecording(hasRecording);
        query.setFromDate(fromDate);
        query.setToDate(toDate);

        int pageSize = Math.min(size, AppConstants.MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "initiatedAt"));

        return ResponseEntity.ok(PaginatedResponse.of(adminCallService.searchCalls(query, pageable)));
    }

    @GetMapping("/{callId}")
    public ResponseEntity<ApiResponse<AdminConsultationCallSummaryResponse>> getCallDetail(
            @PathVariable UUID callId) {
        return ResponseEntity.ok(ApiResponse.success(adminCallService.getCallDetail(callId)));
    }

    @GetMapping("/{callId}/recording-url")
    public ResponseEntity<ApiResponse<Map<String, String>>> getRecordingPresignedUrl(
            @PathVariable UUID callId,
            Principal principal) {
        UUID adminUserId = SecurityUtils.requireCurrentUserId(principal);
        String url = adminCallService.getRecordingPresignedUrl(callId, adminUserId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("url", url)));
    }

    @DeleteMapping("/{callId}/recording")
    public ResponseEntity<Void> deleteRecording(
            @PathVariable UUID callId,
            Principal principal) {
        UUID adminUserId = SecurityUtils.requireCurrentUserId(principal);
        adminCallService.deleteRecording(callId, adminUserId);
        return ResponseEntity.noContent().build();
    }
}
