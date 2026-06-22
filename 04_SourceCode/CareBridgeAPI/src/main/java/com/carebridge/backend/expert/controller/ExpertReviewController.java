package com.carebridge.backend.expert.controller;

import com.carebridge.backend.expert.dto.request.CreateReviewRequest;
import com.carebridge.backend.expert.dto.response.ReviewResponse;
import com.carebridge.backend.expert.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ExpertReviewController {

    private final ReviewService reviewService;

    @PostMapping
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ReviewResponse> createReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateReviewRequest request
    ) {
        UUID motherId = UUID.fromString(userDetails.getUsername());
        ReviewResponse response = reviewService.createReview(motherId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/expert/{expertId}")
    public ResponseEntity<List<ReviewResponse>> getReviewsByExpert(@PathVariable UUID expertId) {
        List<ReviewResponse> responses = reviewService.getReviewsByExpert(expertId);
        return ResponseEntity.ok(responses);
    }
}
