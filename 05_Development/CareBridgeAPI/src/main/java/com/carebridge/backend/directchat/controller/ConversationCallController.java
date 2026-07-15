package com.carebridge.backend.directchat.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.directchat.dto.request.InitiateCallRequest;
import com.carebridge.backend.directchat.dto.response.ConversationCallResponse;
import com.carebridge.backend.directchat.service.IConversationCallService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/direct-conversations/{conversationId}/calls")
@RequiredArgsConstructor
public class ConversationCallController {

    private final IConversationCallService callService;

    @PostMapping
    @PreAuthorize("hasAnyRole('MOTHER', 'EXPERT')")
    public ResponseEntity<ApiResponse<ConversationCallResponse>> initiateCall(
            @PathVariable UUID conversationId,
            @Valid @RequestBody InitiateCallRequest request,
            Principal principal) {
        UUID callerUserId = SecurityUtils.requireCurrentUserId(principal);
        ConversationCallResponse response = callService.initiateCall(conversationId, callerUserId, request.getCallType());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PatchMapping("/{callId}/ringing")
    @PreAuthorize("hasAnyRole('MOTHER', 'EXPERT')")
    public ResponseEntity<ApiResponse<ConversationCallResponse>> markRinging(
            @PathVariable UUID conversationId, @PathVariable UUID callId, Principal principal) {
        UUID currentUserId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(callService.markRinging(conversationId, callId, currentUserId)));
    }

    @PatchMapping("/{callId}/answer")
    @PreAuthorize("hasAnyRole('MOTHER', 'EXPERT')")
    public ResponseEntity<ApiResponse<ConversationCallResponse>> answer(
            @PathVariable UUID conversationId, @PathVariable UUID callId, Principal principal) {
        UUID currentUserId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(callService.answer(conversationId, callId, currentUserId)));
    }

    @PatchMapping("/{callId}/decline")
    @PreAuthorize("hasAnyRole('MOTHER', 'EXPERT')")
    public ResponseEntity<ApiResponse<ConversationCallResponse>> decline(
            @PathVariable UUID conversationId, @PathVariable UUID callId, Principal principal) {
        UUID currentUserId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(callService.decline(conversationId, callId, currentUserId)));
    }

    @PatchMapping("/{callId}/end")
    @PreAuthorize("hasAnyRole('MOTHER', 'EXPERT')")
    public ResponseEntity<ApiResponse<ConversationCallResponse>> end(
            @PathVariable UUID conversationId, @PathVariable UUID callId, Principal principal) {
        UUID currentUserId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(callService.end(conversationId, callId, currentUserId)));
    }
}
