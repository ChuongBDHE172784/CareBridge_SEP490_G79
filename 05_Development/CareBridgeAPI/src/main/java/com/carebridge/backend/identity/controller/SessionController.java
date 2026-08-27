package com.carebridge.backend.identity.controller;

import com.carebridge.backend.common.exception.ResourceNotFoundException;
import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.identity.dto.SessionInfo;
import com.carebridge.backend.identity.service.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class SessionController {

    private final SessionService sessionService;

    @GetMapping
    public ApiResponse<List<SessionInfo>> getSessions(Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        List<SessionInfo> sessions = sessionService.getActiveSessions(userId);
        return ApiResponse.success(sessions);
    }

    @GetMapping("/paged")
    public ResponseEntity<ApiResponse<Page<SessionInfo>>> getSessionsPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "lastActivityAt"));
        Page<SessionInfo> sessions = sessionService.getActiveSessionsPaged(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(sessions));
    }

    @DeleteMapping
    public ApiResponse<Void> revokeAllOtherSessions(Principal principal, HttpServletRequest request) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        String ip = request.getRemoteAddr();
        int revoked = sessionService.revokeAllExceptCurrent(userId, ip);
        return ApiResponse.success(null, "Revoked " + revoked + " other session(s)");
    }

    @DeleteMapping("/{sessionId}")
    public ApiResponse<Void> revokeSession(
            @PathVariable UUID sessionId,
            Principal principal,
            HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        try {
            sessionService.revokeSession(sessionId, userId, ip);
            return ApiResponse.success(null);
        } catch (IllegalArgumentException e) {
            if ("Session not found".equals(e.getMessage())) {
                throw new ResourceNotFoundException("Session not found");
            }
            if ("Please use Logout to sign out from this device".equals(e.getMessage())) {
                throw new IllegalArgumentException("Cannot revoke current session. Please use Logout instead.");
            }
            throw e;
        }
    }
}
