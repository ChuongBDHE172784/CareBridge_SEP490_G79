package com.carebridge.backend.consultation.dto.request;

import jakarta.validation.constensions.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for searching experts.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpertSearchRequest {

    /**
     * Search keyword in specialty.
     */
    private String keyword;

    /**
     * Filter by specialty (exact match).
     */
    private String specialty;

    /**
     * Filter by minimum rating (1-5).
     */
    private Double minRating;

    /**
     * Whether to only show verified experts.
     */
    @Builder.Default
    private Boolean verifiedOnly = true;

    /**
     * Page number (1-based).
     */
    @Builder.Default
    private Integer page = 1;

    /**
     * Page size.
     */
    @Builder.Default
    private Integer size = 20;
}
