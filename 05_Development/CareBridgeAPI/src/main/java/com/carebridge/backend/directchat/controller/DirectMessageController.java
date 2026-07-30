package com.carebridge.backend.directchat.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.directchat.dto.request.SendDirectMessageRequest;
import com.carebridge.backend.directchat.dto.response.TimelineItemResponse;
import com.carebridge.backend.directchat.dto.response.TimelinePageResponse;
import com.carebridge.backend.directchat.service.IDirectMessageService;
import com.carebridge.backend.directchat.service.SendDirectMessageResult;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/direct-conversations/{conversationId}")
@RequiredArgsConstructor
public class DirectMessageController {

    private final IDirectMessageService messageService;

    @PostMapping("/messages")
    @PreAuthorize("hasAnyRole('MOTHER', 'FAMILY', 'EXPERT')")
    public ResponseEntity<ApiResponse<TimelineItemResponse>> sendMessage(
            @PathVariable UUID conversationId,
            @Valid @RequestBody SendDirectMessageRequest request,
            Principal principal) {
        UUID senderUserId = SecurityUtils.requireCurrentUserId(principal);
        SendDirectMessageResult result = messageService.sendMessage(conversationId, senderUserId, request);
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(ApiResponse.success(result.message()));
    }

    @GetMapping("/timeline")
    @PreAuthorize("hasAnyRole('MOTHER', 'FAMILY', 'EXPERT')")
    public ResponseEntity<ApiResponse<TimelinePageResponse>> getTimeline(
            @PathVariable UUID conversationId,
            @RequestParam(required = false) String after,
            @RequestParam(required = false) String before,
            @RequestParam(defaultValue = "30") int limit,
            Principal principal) {
        UUID currentUserId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(
                messageService.getTimeline(conversationId, currentUserId, after, before, limit)));
    }
}
