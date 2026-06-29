package com.carebridge.backend.expert.repository;

import com.carebridge.backend.expert.entity.Expert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Expert repository.
 * Provides CRUD operations and custom queries for Expert entities.
 */
@Repository
public interface ExpertRepository extends JpaRepository<Expert, Long> {

    /**
     * Find expert by user ID.
     * One-to-one relationship lookup.
     *
     * @param userId the user ID
     * @return Optional of Expert
     */
    Optional<Expert> findByUserId(Long userId);

    /**
     * Find experts by specialty.
     *
     * @param specialty the specialty filter
     * @return List of experts
     */
    @Query("SELECT e FROM Expert e WHERE e.specialty = :specialty AND e.verificationStatus = 'APPROVED'")
    java.util.List<Expert> findBySpecialty(@Param("specialty") String specialty);

    /**
     * Search experts by specialty keyword (partial match).
     *
     * @param keyword the search keyword
     * @return List of matching approved experts
     */
    @Query("SELECT e FROM Expert e WHERE e.verificationStatus = 'APPROVED' AND LOWER(e.specialty) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    java.util.List<Expert> searchBySpecialty(@Param("keyword") String keyword);

    /**
     * Count reviews for an expert.
     *
     * @param expertId the expert ID
     * @return count of reviews
     */
    @Query("SELECT COUNT(r) FROM ExpertReview r WHERE r.expertId = :expertId AND r.moderationStatus = 'APPROVED'")
    long countApprovedReviews(@Param("expertId") Long expertId);

    /**
     * Update expert rating (call after review moderation).
     *
     * @param expertId the expert ID
     * @param avgRating the new average rating
     * @param reviewCount the new review count
     */
    @Query("UPDATE Expert e SET e.ratingAvg = :avgRating, e.reviewCount = :reviewCount WHERE e.expertId = :expertId")
    void updateRating(@Param("expertId") Long expertId, @Param("avgRating") Double avgRating, @Param("reviewCount") Long reviewCount);
}
