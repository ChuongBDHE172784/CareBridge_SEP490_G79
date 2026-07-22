package com.carebridge.backend.content.controller;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.community.dto.request.CreateCommunityTopicRequest;
import com.carebridge.backend.community.dto.request.UpdateCommunityTopicRequest;
import com.carebridge.backend.community.dto.response.CommunityTopicResponse;
import com.carebridge.backend.community.entity.TopicType;
import com.carebridge.backend.community.service.CommunityTopicService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/content/categories")
@PreAuthorize("hasRole('CONTENT_ADMIN')")
@RequiredArgsConstructor
public class ContentCategoryController {
    private final CommunityTopicService topicService;
    private final AuditService auditService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CommunityTopicResponse>>> listCategories(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "false") boolean includeHidden, Principal principal) {
        UUID actorId = SecurityUtils.requireCurrentUserId(principal);
        // This controller manages the CATEGORY taxonomy only — filter explicitly so it never
        // shows TOPIC/TAG rows created via CommunityTopicController's fuller management page.
        List<CommunityTopicResponse> result = keyword == null || keyword.isBlank()
                ? topicService.getTopics(includeHidden, TopicType.CATEGORY, actorId)
                : topicService.searchTopics(keyword, includeHidden, TopicType.CATEGORY, actorId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CommunityTopicResponse>> createCategory(
            @Valid @RequestBody CreateCommunityTopicRequest request, Principal principal) {
        UUID actorId = SecurityUtils.requireCurrentUserId(principal);
        // This route creates CATEGORY entries only — never trust/derive type from the client body.
        request.setType(TopicType.CATEGORY);
        CommunityTopicResponse result = topicService.createTopic(actorId, request);
        auditService.log(AuditAction.CONTENT_CATEGORY_MANAGED, actorId, "COMMUNITY_TOPIC",
                result.getId().toString(), "action=CREATE");
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(result, "Category created"));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<CommunityTopicResponse>> updateCategory(
            @PathVariable UUID id, @Valid @RequestBody UpdateCommunityTopicRequest request, Principal principal) {
        UUID actorId = SecurityUtils.requireCurrentUserId(principal);
        CommunityTopicResponse result = topicService.updateTopic(id, actorId, request);
        auditService.log(AuditAction.CONTENT_CATEGORY_MANAGED, actorId, "COMMUNITY_TOPIC", id.toString(), "action=UPDATE");
        return ResponseEntity.ok(ApiResponse.success(result, "Category updated"));
    }
}
