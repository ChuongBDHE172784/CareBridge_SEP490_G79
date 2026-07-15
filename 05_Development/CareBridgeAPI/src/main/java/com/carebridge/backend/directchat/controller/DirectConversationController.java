package com.carebridge.backend.directchat.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.directchat.dto.response.DirectConversationResponse;
import com.carebridge.backend.directchat.dto.response.DirectConversationSummaryResponse;
import com.carebridge.backend.directchat.service.FindOrCreateConversationResult;
import com.carebridge.backend.directchat.service.IDirectConversationService;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/direct-conversations")
@RequiredArgsConstructor
public class DirectConversationController {

    private final IDirectConversationService conversationService;

    @PostMapping("/expert/{expertId}")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<DirectConversationResponse>> findOrCreate(
            @PathVariable UUID expertId, Principal principal) {
        UUID motherUserId = SecurityUtils.requireCurrentUserId(principal);
        FindOrCreateConversationResult result = conversationService.findOrCreate(motherUserId, expertId);
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(ApiResponse.success(result.conversation()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MOTHER', 'EXPERT')")
    public ResponseEntity<ApiResponse<List<DirectConversationSummaryResponse>>> listMyConversations(Principal principal) {
        UUID currentUserId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(conversationService.listMyConversations(currentUserId)));
    }

    @GetMapping("/{conversationId}")
    @PreAuthorize("hasAnyRole('MOTHER', 'EXPERT')")
    public ResponseEntity<ApiResponse<DirectConversationResponse>> getConversation(
            @PathVariable UUID conversationId, Principal principal) {
        UUID currentUserId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(conversationService.getConversation(conversationId, currentUserId)));
    }
}
