package com.carebridge.backend.directchat.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.directchat.dto.request.SendDirectMessageRequest;
import com.carebridge.backend.directchat.dto.response.TimelineItemResponse;
import com.carebridge.backend.directchat.dto.response.TimelinePageResponse;
import com.carebridge.backend.directchat.service.IDirectMessageService;
import com.carebridge.backend.directchat.service.DirectChatAttachmentAccessService;
import com.carebridge.backend.file.dto.ViewFileResponse;
import com.carebridge.backend.file.dto.UploadFileResponse;
import com.carebridge.backend.file.enums.FileKind;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/direct-conversations/{conversationId}")
@RequiredArgsConstructor
public class DirectMessageController {

    private final IDirectMessageService messageService;
    private final DirectChatAttachmentAccessService attachmentAccessService;

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

    @PatchMapping("/messages/{messageId}/recall")
    @PreAuthorize("hasAnyRole('MOTHER', 'FAMILY', 'EXPERT')")
    public ResponseEntity<ApiResponse<Void>> recallMessage(
            @PathVariable UUID conversationId, @PathVariable UUID messageId, Principal principal) {
        messageService.recallMessage(conversationId, messageId, SecurityUtils.requireCurrentUserId(principal));
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/messages/{messageId}/attachment")
    @PreAuthorize("hasAnyRole('MOTHER', 'FAMILY', 'EXPERT')")
    public ResponseEntity<ApiResponse<ViewFileResponse>> viewAttachment(
            @PathVariable UUID conversationId, @PathVariable UUID messageId, Principal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                attachmentAccessService.view(conversationId, messageId, SecurityUtils.requireCurrentUserId(principal))));
    }

    @PostMapping(value = "/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('MOTHER', 'FAMILY', 'EXPERT')")
    public ResponseEntity<ApiResponse<UploadFileResponse>> uploadAttachment(
            @PathVariable UUID conversationId,
            @RequestPart("file") MultipartFile file,
            @RequestParam("kind") FileKind kind,
            Principal principal) {
        if (kind != FileKind.IMAGE && kind != FileKind.DOCUMENT) {
            throw new IllegalArgumentException("Direct chat only supports images and documents");
        }
        var response = attachmentAccessService.upload(
                conversationId, SecurityUtils.requireCurrentUserId(principal), file, kind);
        return ResponseEntity.status(201).body(ApiResponse.success(response, "File uploaded successfully"));
    }
}
