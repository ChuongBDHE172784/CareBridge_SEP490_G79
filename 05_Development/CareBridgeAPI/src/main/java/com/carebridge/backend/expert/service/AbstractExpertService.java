package com.carebridge.backend.expert.service;

import com.carebridge.backend.common.exception.ResourceAlreadyExistsException;
import com.carebridge.backend.common.exception.ResourceNotFoundException;
import com.carebridge.backend.expert.dto.request.CreateExpertRequest;
import com.carebridge.backend.expert.dto.request.UpdateExpertRequest;
import com.carebridge.backend.expert.dto.response.ExpertProfileDetailResponse;
import com.carebridge.backend.expert.dto.response.ExpertProfilePublicResponse;
import com.carebridge.backend.expert.dto.response.ExpertReviewDTO;
import com.carebridge.backend.expert.entity.Expert;
import com.carebridge.backend.expert.mapper.ExpertMapper;
import com.carebridge.backend.expert.policy.ExpertPolicy;
import com.carebridge.backend.expert.repository.ExpertRepository;
import com.carebridge.backend.expert.repository.ExpertReviewRepository;
import com.carebridge.backend.expert.repository.VerificationDocumentRepository;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.rbac.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Abstract base class for Expert Service.
 * Provides common infrastructure and template method pattern.
 *
 * SOLID Compliance:
 * - Single Responsibility: Abstract class handles common patterns, subclasses handle specific business logic
 * - Open/Closed: Can extend with new implementations without modifying this base
 * - Liskov: Subclasses can substitute this base class
 * - Dependency Inversion: Depends on abstractions (repositories, mappers, policies)
 *
 * Template Method Pattern:
 * - createExpertProfileTemplate() defines skeleton, delegates specific steps to subclasses
 * - loadExpertWithValidation() is a protected helper for reuse
 */
public abstract class AbstractExpertService {

    protected final ExpertRepository expertRepository;
    protected final UserRepository userRepository;
    protected final VerificationDocumentRepository documentRepository;
    protected final ExpertReviewRepository reviewRepository;
    protected final ExpertPolicy expertPolicy;
    protected final ExpertMapper expertMapper;

    protected AbstractExpertService(ExpertRepository expertRepository,
                                    UserRepository userRepository,
                                    VerificationDocumentRepository documentRepository,
                                    ExpertReviewRepository reviewRepository,
                                    ExpertPolicy expertPolicy,
                                    ExpertMapper expertMapper) {
        this.expertRepository = expertRepository;
        this.userRepository = userRepository;
        this.documentRepository = documentRepository;
        this.reviewRepository = reviewRepository;
        this.expertPolicy = expertPolicy;
        this.expertMapper = expertMapper;
    }

    /**
     * Template method for creating expert profile.
     * Common validation and flow, allows subclasses to customize.
     */
    @Transactional
    public ExpertProfilePublicResponse createExpertProfile(Long userId, CreateExpertRequest request) {
        logInfo("Creating expert profile for userId: {}", userId);

        // Validate user
        User user = validateUser(userId);

        // Check duplicate
        checkDuplicateProfile(userId);

        // Build expert entity (hook for subclass customization)
        Expert expert = buildExpertEntity(request);
        expert.setUserId(userId);
        expert.setVerificationStatus(com.carebridge.backend.expert.enums.ExpertVerificationStatus.PENDING_VERIFICATION);

        // Save and post-process
        Expert saved = saveExpert(expert);
        logInfo("Expert profile created with ID: {}", saved.getExpertId());

        return mapToPublicResponse(saved);
    }

    /**
     * Hook method - subclasses can override to customize expert entity building.
     */
    protected Expert buildExpertEntity(CreateExpertRequest request) {
        return expertMapper.toEntity(
                request.getSpecialty(),
                request.getExperienceYears(),
                request.getProfessionalTitle(),
                request.getWorkplace(),
                request.getConsultationScope()
        );
    }

    protected Expert saveExpert(Expert expert) {
        return expertRepository.save(expert);
    }

    protected ExpertProfilePublicResponse mapToPublicResponse(Expert expert) {
        return expertMapper.toPublicResponse(expert);
    }

    protected void logInfo(String message, Object... args) {
        // Can be overridden or use slf4j in concrete class
    }

    private User validateUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private void checkDuplicateProfile(Long userId) {
        expertRepository.findByUserId(userId).ifPresent(expert -> {
            throw new ResourceAlreadyExistsException("Expert profile already exists for this user");
        });
    }

    // Abstract methods that subclasses must implement
    public abstract ExpertProfileDetailResponse getExpertProfileDetail(Long expertId,
                                                                       Long requestingUserId,
                                                                       String requestingRole);

    public abstract ExpertProfilePublicResponse getExpertProfile(Long expertId,
                                                                 Long requestingUserId,
                                                                 String requestingRole);

    public abstract ExpertProfileDetailResponse updateExpertProfile(Long expertId,
                                                                    Long userId,
                                                                    String role,
                                                                    UpdateExpertRequest request);

    public abstract List<ExpertReviewDTO> getExpertReviews(Long expertId,
                                                            Long requestingUserId,
                                                            String requestingRole);
}
