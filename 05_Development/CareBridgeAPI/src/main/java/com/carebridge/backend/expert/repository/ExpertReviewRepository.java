package com.carebridge.backend.expert.repository;

import com.carebridge.backend.expert.entity.ExpertReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Expert review repository.
 */
@Repository
public interface ExpertReviewRepository extends JpaRepository<ExpertReview, Long> {

    /**
     * Find reviews by expert ID.
     *
     * @param expertId the expert ID
     * @return list of approved reviews
     */
    @Query("SELECT r FROM ExpertReview r WHERE r.expertId = :expertId AND r.moderationStatus = 'APPROVED' ORDER BY r.createdAt DESC")
    List<ExpertReview> findByExpertId(@Param("expertId") Long expertId);

    /**
     * Find review by booking ID.
     * Enforces one review per booking/consultation.
     *
     * @param bookingId the booking ID
     * @return Optional of review
     */
    ExpertReview findByBookingId(Long bookingId);

    /**
     * Calculate average rating for an expert.
     *
     * @param expertId the expert ID
     * @return average rating or null if no reviews
     */
    @Query("SELECT AVG(r.rating) FROM ExpertReview r WHERE r.expertId = :expertId AND r.moderationStatus = 'APPROVED'")
    Double findAverageRatingByExpertId(@Param("expertId") Long expertId);

    /**
     * Count approved reviews for an expert.
     *
     * @param expertId the expert ID
     * @return count of reviews
     */
    long countByExpertIdAndModerationStatus(Long expertId, ExpertReview.ModerationStatus moderationStatus);
}
