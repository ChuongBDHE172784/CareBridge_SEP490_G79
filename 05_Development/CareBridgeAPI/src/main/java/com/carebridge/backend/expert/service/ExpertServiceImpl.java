package com.carebridge.backend.expert.service;

import com.carebridge.backend.common.exception.ResourceAlreadyExistsException;
import com.carebridge.backend.common.exception.ResourceNotFoundException;
import com.carebridge.backend.expert.dto.request.CreateExpertRequest;
import com.carebridge.backend.expert.dto.request.UpdateExpertRequest;
import com.carebridge.backend.expert.dto.response.ExpertProfileDetailResponse;
import com.carebridge.backend.expert.dto.response.ExpertProfilePublicResponse;
import com.carebridge.backend.expert.dto.response.ExpertReviewDTO;
import com.carebridge.backend.expert.entity.Expert;
import com.carebridge.backend.expert.entity.ExpertReview;
import com.carebridge.backend.expert.entity.VerificationDocument;
import com.carebridge.backend.expert.mapper.ExpertMapper;
import com.carebridge.backend.expert.policy.ExpertPolicy;
import com.carebridge.backend.expert.repository.ExpertRepository;
import com.carebridge.backend.expert.repository.ExpertReviewRepository;
import com.carebridge.backend.expert.repository.VerificationDocumentRepository;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.rbac.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Expert Service Implementation.
 * Extends AbstractExpertService and implements specific business logic.
 *
 * SOLID Compliance:
 * - SRP: Only handles expert profile business logic, inherits common patterns
 * - OCP: Extends base without modifying it
 * - LSP: Fully substitutable for AbstractExpertService
 * - DIP: Depends on abstractions via constructor injection
 *
 * Features:
 * - Expert profile CRUD with RBAC
 * - Verification document management
 * - Review aggregation
 */
@Service("expertService")
@RequiredArgsConstructor
@Slf4j
public class ExpertServiceImpl extends AbstractExpertService implements IExpertService {

    private final VerificationDocumentRepository documentRepository;
    private final ExpertReviewRepository reviewRepository;

    public ExpertServiceImpl(ExpertRepository expertRepository,
                             com.carebridge.backend.security.repository.UserRepository userRepository,
                             VerificationDocumentRepository documentRepository,
                             ExpertReviewRepository reviewRepository,
                             ExpertPolicy expertPolicy,
                             ExpertMapper expertMapper) {
        super(expertRepository, userRepository, documentRepository, reviewRepository, expertPolicy, expertMapper);
        this.documentRepository = documentRepository;
        this.reviewRepository = reviewRepository;
    }

    @Override
    @Transactional
    public ExpertProfilePublicResponse createExpertProfile(Long userId, CreateExpertRequest request) {
        // Use template method from base class
        return super.createExpertProfile(userId, request);
    }

    @Override
    @Transactional(readOnly = true)
    public ExpertProfileDetailResponse getExpertProfileDetail(Long expertId,
                                                              Long requestingUserId,
                                                              String requestingRole) {
        log.debug("Getting expert profile detail for expertId: {}", expertId);

        Expert expert = super.loadExpertWithValidation(expertId, requestingUserId, Role.valueOf(requestingRole));

        // Get verification documents (only for expert owner or admin)
        List<VerificationDocument> documents = List.of();
        Role role = Role.valueOf(requestingRole);
        if ((role == Role.EXPERT && expert.getUserId().equals(requestingUserId)) || role == Role.SYSTEM_ADMIN) {
            documents = documentRepository.findByExpertId(expertId);
        }

        // Get reviews (only approved ones)
        List<ExpertReview> reviews = reviewRepository.findByExpertIdAndModerationStatus(
                expertId, ExpertReview.ModerationStatus.APPROVED);

        return expertMapper.toDetailResponse(expert, documents,
                reviews.stream().map(expertMapper::toReviewDTO).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ExpertProfilePublicResponse getExpertProfile(Long expertId,
                                                        Long requestingUserId,
                                                        String requestingRole) {
        Expert expert = super.loadExpertWithValidation(expertId, requestingUserId, Role.valueOf(requestingRole));
        return super.mapToPublicResponse(expert);
    }

    @Override
    @Transactional
    public ExpertProfileDetailResponse updateExpertProfile(Long expertId,
                                                            Long userId,
                                                            String roleStr,
                                                            UpdateExpertRequest request) {
        log.info("Updating expert profile: expertId={}, userId={}", expertId, userId);

        Expert expert = expertRepository.findById(expertId)
                .orElseThrow(() -> new ResourceNotFoundException("Expert not found"));

        Role role = Role.valueOf(roleStr);
        expertPolicy.ensureCanUpdateExpert(expert, userId, role);

        // Apply updates (Hook method can be overridden for custom behavior)
        applyUpdateRequest(expert, request);

        Expert saved = expertRepository.save(expert);
        log.info("Expert profile updated: expertId={}", saved.getExpertId());

        // Return detail view
        return getExpertProfileDetail(expertId, userId, roleStr);
    }

    /**
     * Hook: Apply update request to entity.
     * Can be overridden by subclasses for custom update logic.
     */
    protected void applyUpdateRequest(Expert expert, UpdateExpertRequest request) {
        if (request.getSpecialty() != null) {
            expert.setSpecialty(request.getSpecialty());
        }
        if (request.getExperienceYears() != null) {
            expert.setExperienceYears(request.getExperienceYears());
        }
        if (request.getProfessionalTitle() != null) {
            expert.setProfessionalTitle(request.getProfessionalTitle());
        }
        if (request.getWorkplace() != null) {
            expert.setWorkplace(request.getWorkplace());
        }
        if (request.getConsultationScope() != null) {
            expert.setConsultationScope(request.getConsultationScope());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpertReviewDTO> getExpertReviews(Long expertId,
                                                   Long requestingUserId,
                                                   String requestingRole) {
        log.debug("Getting reviews for expertId: {}", expertId);

        Expert expert = super.loadExpertWithValidation(expertId, requestingUserId, Role.valueOf(requestingRole));

        return reviewRepository.findByExpertIdAndModerationStatus(expertId, ExpertReview.ModerationStatus.APPROVED)
                .stream()
                .map(expertMapper::toReviewDTO)
                .toList();
    }

    @Override
    protected ExpertProfilePublicResponse doMapToPublicResponse(Expert expert) {
        return expertMapper.toPublicResponse(expert);
    }

    @Override
    protected void doPostCreate(Expert expert, CreateExpertRequest request) {
        // Hook for post-creation actions
        // e.g., send notification, create audit log, trigger verification workflow
        log.info("Expert created - triggering verification workflow for expertId: {}", expert.getExpertId());
    }

    @Override
    public Expert getExpertEntity(Long expertId) {
        return expertRepository.findById(expertId)
                .orElseThrow(() -> new ResourceNotFoundException("Expert not found"));
    }
}
