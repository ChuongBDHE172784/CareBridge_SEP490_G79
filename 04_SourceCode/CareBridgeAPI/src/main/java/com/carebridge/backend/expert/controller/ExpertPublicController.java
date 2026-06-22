package com.carebridge.backend.expert.controller;

import com.carebridge.backend.expert.dto.response.ExpertProfilePublicResponse;
import com.carebridge.backend.expert.service.ExpertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/experts")
@RequiredArgsConstructor
public class ExpertPublicController {

    private final ExpertService expertService;

    @GetMapping("/{expertId}")
    public ResponseEntity<ExpertProfilePublicResponse> getExpertProfile(@PathVariable UUID expertId) {
        ExpertProfilePublicResponse response = expertService.getPublicProfile(expertId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<com.carebridge.backend.expert.dto.response.PageResponse<com.carebridge.backend.expert.dto.response.ExpertSummaryResponse>> searchExperts(
            @RequestParam(required = false) String expertise,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        // This will be implemented with ExpertSearchRepository
        // For now, return empty page
        return ResponseEntity.ok(com.carebridge.backend.expert.dto.response.PageResponse.<com.carebridge.backend.expert.dto.response.ExpertSummaryResponse>builder()
                .content(List.of())
                .page(page)
                .size(size)
                .totalElements(0)
                .totalPages(0)
                .last(true)
                .build());
    }
}
