package com.carebridge.backend.expert.dto.response;

import com.carebridge.backend.expert.entity.ExpertReview;
import com.carebridge.backend.expert.enums.ExpertReview.ModerationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Expert review DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpertReviewDTO {

    private Long reviewId;
    private Long bookingId;
    private Long reviewerUserId;
    private Integer rating;
    private String comment;
    private ModerationStatus moderationStatus;
    private Instant createdAt;

    public static ExpertReviewDTO fromEntity(ExpertReview review) {
        return ExpertReviewDTO.builder()
                .reviewId(review.getReviewId())
                .bookingId(review.getBookingId())
                .reviewerUserId(review.getReviewerUserId())
                .rating(review.getRating())
                .comment(review.getComment())
                .moderationStatus(review.getModerationStatus())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
