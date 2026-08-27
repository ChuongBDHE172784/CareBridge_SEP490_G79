package com.carebridge.backend.content.dto.response;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

public record StaffContentDetailResponse(
        @JsonUnwrapped ContentDetailResponse content,
        ReviewFeedbackResponse latestReviewFeedback) {
}

