package com.carebridge.backend.expert.dto.response;

import com.carebridge.backend.expert.enums.ExpertVerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for expert profile (public view).
 * Does not include sensitive verification documents.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpertProfilePublicResponse {

    /**
     * Expert ID.
     */
    private Long expertId;

    /**
     * Expert's specialty.
     */
    private String specialty;

    /**
     * Years of experience.
     */
    private Integer experienceYears;

    /**
     * Professional title.
     */
    private String professionalTitle;

    /**
     * Workplace.
     */
    private String workplace;

    /**
     * Consultation scope.
     */
    private String consultationScope;

    /**
     * Current verification status.
     */
    private ExpertVerificationStatus verificationStatus;

    /**
     * Average rating (1-5).
     */
    private Double averageRating;

    /**
     * Total number of reviews.
     */
    private Integer reviewCount;

    /**
     * Whether the expert is currently available for booking.
     * Derived from availability slots.
     */
    private Boolean isAvailable;
}
