package com.carebridge.backend.identity.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.identity.dto.SessionInfo;
import com.carebridge.backend.identity.service.SessionService;
import com.carebridge.backend.security.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class SessionController {

    private final SessionService sessionService;

    @GetMapping
    public ApiResponse<List<SessionInfo>> getSessions(@AuthenticationPrincipal User user) {
        List<SessionInfo> sessions = sessionService.getActiveSessions(user.getId());
        return ApiResponse.success(sessions);
    }

    @DeleteMapping("/{sessionId}")
    public ApiResponse<Void> revokeSession(
            @PathVariable UUID sessionId,
            @AuthenticationPrincipal User user,
            HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        sessionService.revokeSession(sessionId, user.getId(), ip);
        return ApiResponse.success(null);
    }
}
