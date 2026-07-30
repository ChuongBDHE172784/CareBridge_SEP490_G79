package com.carebridge.backend.directchat.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.directchat.dto.response.ConversationCallResponse;
import com.carebridge.backend.directchat.service.IConversationCallService;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/direct-conversations/calls")
@RequiredArgsConstructor
public class ActiveConversationCallController {

    private final IConversationCallService callService;

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('MOTHER', 'FAMILY', 'EXPERT')")
    public ResponseEntity<ApiResponse<List<ConversationCallResponse>>> listActiveCalls(
            Principal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                callService.listActiveCalls(SecurityUtils.requireCurrentUserId(principal))));
    }
}
