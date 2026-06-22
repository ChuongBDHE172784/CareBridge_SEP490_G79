package com.carebridge.backend.expert.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.consultation.entity.ConsultationBooking;
import com.carebridge.backend.consultation.repository.ConsultationBookingRepository;
import com.carebridge.backend.expert.dto.request.CreateReviewRequest;
import com.carebridge.backend.expert.dto.response.ReviewResponse;
import com.carebridge.backend.expert.entity.ExpertProfile;
import com.carebridge.backend.expert.entity.ExpertReview;
import com.carebridge.backend.expert.mapper.ExpertReviewMapper;
import com.carebridge.backend.expert.policy.ReviewPolicy;
import com.carebridge.backend.expert.repository.ExpertProfileRepository;
import com.carebridge.backend.expert.repository.ExpertReviewRepository;
import com.carebridge.backend.expert.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ExpertReviewRepository reviewRepository;
    private final ExpertProfileRepository profileRepository;
    private final ConsultationBookingRepository bookingRepository;
    private final ExpertReviewMapper reviewMapper;
    private final AuditService auditService;

    @Override
    public ReviewResponse createReview(UUID motherId, CreateReviewRequest request) {
        // Validate booking exists and is completed
        ConsultationBooking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        if (!booking.getRequesterUserId().equals(motherId)) {
            throw new IllegalArgumentException("Cannot review a booking you didn't make");
        }

        if (!"COMPLETED".equals(booking.getStatus())) {
            throw new IllegalArgumentException("Can only review completed consultations");
        }

        // Check if review already exists
        ExpertProfile expertProfile = profileRepository.findById(booking.getExpertProfileId())
                .orElseThrow(() -> new IllegalArgumentException("Expert not found"));

        boolean exists = reviewRepository.existsByExpertIdAndMotherIdAndBookingId(
                expertProfile.getId(), motherId, request.getBookingId()
        );
        if (exists) {
            throw new IllegalArgumentException("Review already exists for this consultation");
        }

        // Create review
        ExpertReview review = ExpertReview.builder()
                .expertId(expertProfile.getId())
                .motherId(motherId)
                .bookingId(request.getBookingId())
                .rating(request.getRating())
                .comment(request.getComment())
                .createdAt(Instant.now())
                .build();

        ExpertReview saved = reviewRepository.save(review);

        // Update expert's average rating
        updateExpertRating(expertProfile.getId());

        auditService.log(AuditAction.REVIEW_SUBMITTED, motherId, "expert_review", saved.getId().toString(), null);

        return reviewMapper.toResponse(saved);
    }

    @Override
    public List<ReviewResponse> getReviewsByExpert(UUID expertId) {
        List<ExpertReview> reviews = reviewRepository.findByExpertId(expertId);
        return reviewMapper.toResponseList(reviews);
    }

    private void updateExpertRating(UUID expertId) {
        List<ExpertReview> reviews = reviewRepository.findByExpertId(expertId);
        if (reviews.isEmpty()) return;

        double avg = reviews.stream()
                .mapToInt(ExpertReview::getRating)
                .average()
                .orElse(0.0);

        ExpertProfile profile = profileRepository.findById(expertId)
                .orElseThrow(() -> new IllegalArgumentException("Expert not found"));
        profile.setAvgRating(java.math.BigDecimal.valueOf(avg));
        profile.setTotalReviews(reviews.size());
        profileRepository.save(profile);
    }
}
