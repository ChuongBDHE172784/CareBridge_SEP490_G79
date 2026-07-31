package com.carebridge.backend.audit.controller;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.constants.AppConstants;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.common.response.PaginatedResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import java.security.Principal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@RequiredArgsConstructor
@Slf4j
public class AuditController {

    private final AuditService auditService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'OPERATIONS')")
    public ResponseEntity<PaginatedResponse<com.carebridge.backend.audit.dto.response.AuditLogResponse>> search(
            @RequestParam(required = false) java.util.UUID userId,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Principal principal) {
        if (page < 0 || size < 1) {
            throw new BusinessException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "PAGINATION_INVALID",
                    "page must be >= 0 and size must be >= 1");
        }
        int pageSize = Math.min(size, AppConstants.MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<com.carebridge.backend.audit.dto.response.AuditLogResponse> result =
                auditService.search(userId, action, fromDate, toDate, pageable);

        // ADR-AUDIT-001: meta-audit every audit-log read. Fail-soft — a meta-audit
        // write failure must never fail the read request itself (§12.2 Rollback &
        // Incident Runbook). Payload is a filter-parameter snapshot only, never the
        // result rows (avoids quadratic PII duplication in the audit trail).
        try {
            java.util.UUID callerId = SecurityUtils.requireCurrentUserId(principal);
            Map<String, Object> filterSnapshot = new LinkedHashMap<>();
            filterSnapshot.put("userId", userId);
            filterSnapshot.put("action", action);
            filterSnapshot.put("fromDate", fromDate);
            filterSnapshot.put("toDate", toDate);
            filterSnapshot.put("page", page);
            filterSnapshot.put("size", pageSize);
            auditService.log(AuditAction.VIEW_AUDIT_LOG, callerId, "AuditLog", null, filterSnapshot);
        } catch (Exception e) {
            log.warn("AuditController: meta-audit (VIEW_AUDIT_LOG) write failed — read response unaffected: {}",
                    e.getMessage());
        }

        return ResponseEntity.ok(PaginatedResponse.of(result));
    }
}
