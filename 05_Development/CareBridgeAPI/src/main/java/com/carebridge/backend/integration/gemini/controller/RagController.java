package com.carebridge.backend.integration.gemini.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.integration.gemini.dto.RagAnswerRequest;
import com.carebridge.backend.integration.gemini.dto.RagAnswerResponse;
import com.carebridge.backend.integration.gemini.exception.RagException;
import com.carebridge.backend.integration.gemini.service.RagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rag")
@RequiredArgsConstructor
public class RagController {

    private final RagService ragService;

    // PARTNER is excluded: RAG health guidance is for personal-use roles only.
    // Account locked/disabled state is enforced at token issuance (AuthenticationPolicy),
    // not per-request — token TTL is 15 min, bounding any residual window.
    @PreAuthorize("hasAnyRole('MOTHER', 'FAMILY', 'EXPERT', 'MODERATOR', 'CONTENT_ADMIN', 'SYSTEM_ADMIN')")
    @PostMapping("/answer")
    public ResponseEntity<ApiResponse<RagAnswerResponse>> generateAnswer(
            @RequestBody RagAnswerRequest request) {

        String query = request.getQuery();
        if (query == null || query.isBlank() || query.length() < 3 || query.length() > 500) {
            throw RagException.invalidQuery();
        }
        if (request.getMaxContextChunks() != null && request.getMaxContextChunks() > 10) {
            throw RagException.contextChunksExceeded();
        }

        RagAnswerResponse response = ragService.generateAnswer(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
