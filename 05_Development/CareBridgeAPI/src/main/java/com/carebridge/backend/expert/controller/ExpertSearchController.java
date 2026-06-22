package com.carebridge.backend.expert.controller;

import com.carebridge.backend.expert.dto.request.SearchExpertsRequest;
import com.carebridge.backend.expert.dto.response.ExpertSummaryResponse;
import com.carebridge.backend.expert.dto.response.PageResponse;
import com.carebridge.backend.expert.service.ExpertSearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/expert")
@RequiredArgsConstructor
public class ExpertSearchController {

    private final ExpertSearchService searchService;

    @GetMapping("/directory")
    public ResponseEntity<PageResponse<ExpertSummaryResponse>> getVerifiedExperts(
            @RequestParam(required = false) String expertise,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        SearchExpertsRequest request = SearchExpertsRequest.builder()
                .expertise(expertise)
                .page(page)
                .size(size)
                .build();
        PageResponse<ExpertSummaryResponse> response = searchService.searchExperts(request);
        return ResponseEntity.ok(response);
    }
}
