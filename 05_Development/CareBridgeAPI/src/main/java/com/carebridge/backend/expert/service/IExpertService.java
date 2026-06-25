package com.carebridge.backend.expert.service;

import com.carebridge.backend.expert.dto.request.CreateExpertRequest;
import com.carebridge.backend.expert.dto.request.UpdateExpertRequest;
import com.carebridge.backend.expert.dto.response.ExpertProfileDetailResponse;
import com.carebridge.backend.expert.dto.response.ExpertProfilePublicResponse;
import com.carebridge.backend.expert.dto.response.ExpertReviewDTO;
import com.carebridge.backend.expert.entity.Expert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Expert Service Interface.
 * Business logic for expert profile management.
 *
 * TV4 Use Cases: UC-87, UC-88 (Create/Update Expert Profile)
 */
public interface IExpertService {

    /**
     * Create a new expert profile.
     * Only users with VERIFIED_EXPERT role can create their own profile.
     *
     * @param userId the user creating the profile
     * @param request the expert profile data
     * @return the created expert profile (public response)
     * @throws IllegalArgumentException if profile already exists
     */
    ExpertProfilePublicResponse createExpertProfile(Long userId, CreateExpertRequest request);

    /**
     * Update an existing expert profile.
     *
     * @param expertId the expert profile ID
     * @param userId the user making the request
     * @param role the user's role
     * @param request the update data
     * @return the updated expert profile
     * @throws ResourceNotFoundException if expert not found
     * @throws AccessDeniedException if user cannot update
     */
    ExpertProfileDetailResponse updateExpertProfile(Long expertId, Long userId, String role, UpdateExpertRequest request);

    /**
     * Get expert profile by ID (public view).
     *
     * @param expertId the expert ID
     * @param requestingUserId the user making the request
     * @param requestingRole the user's role
     * @return public expert profile
     */
    ExpertProfilePublicResponse getExpertProfile(Long expertId, Long requestingUserId, String requestingRole);

    /**
     * Get expert profile detail (owner or admin view).
     *
     * @param expertId the expert ID
     * @param requestingUserId the user making the request
     * @param requestingRole the user's role
     * @return detailed expert profile
     */
    ExpertProfileDetailResponse getExpertProfileDetail(Long expertId, Long requestingUserId, String requestingRole);

    /**
     * Get all reviews for an expert.
     *
     * @param expertId the expert ID
     * @param requestingUserId the user making the request
     * @param requestingRole the user's role
     * @return list of approved reviews
     */
    List<ExpertReviewDTO> getExpertReviews(Long expertId, Long requestingUserId, String requestingRole);

    /**
     * Get Expert entity by ID (for internal use, policy checks).
     *
     * @param expertId the expert ID
     * @return the Expert entity
     * @throws ResourceNotFoundException if not found
     */
    Expert getExpertEntity(Long expertId);
}
