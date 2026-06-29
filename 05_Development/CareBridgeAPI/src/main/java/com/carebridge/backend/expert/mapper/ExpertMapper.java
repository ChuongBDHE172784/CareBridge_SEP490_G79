package com.carebridge.backend.expert.mapper;

import com.carebridge.backend.expert.dto.response.AvailabilitySlotDTO;
import com.carebridge.backend.expert.dto.response.ExpertProfileDetailResponse;
import com.carebridge.backend.expert.dto.response.ExpertProfilePublicResponse;
import com.carebridge.backend.expert.dto.response.ExpertReviewDTO;
import com.carebridge.backend.expert.entity.AvailabilitySlot;
import com.carebridge.backend.expert.entity.Expert;
import com.carebridge.backend.expert.entity.ExpertReview;
import com.carebridge.backend.expert.entity.VerificationDocument;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for Expert module entities to DTOs.
 * Uses manual mapping for simplicity and control.
 */
@Component
public class ExpertMapper {

    /**
     * Map Expert entity to public response DTO.
     */
    public ExpertProfilePublicResponse toPublicResponse(Expert expert) {
        return ExpertProfilePublicResponse.builder()
                .expertId(expert.getExpertId())
                .specialty(expert.getSpecialty())
                .experienceYears(expert.getExperienceYears())
                .professionalTitle(expert.getProfessionalTitle())
                .workplace(expert.getWorkplace())
                .consultationScope(expert.getConsultationScope())
                .verificationStatus(expert.getVerificationStatus())
                .averageRating(expert.getRatingAvg())
                .reviewCount(expert.getReviewCount())
                .isAvailable(false) // TODO: compute from availability
                .build();
    }

    /**
     * Map Expert entity to detailed response DTO.
     */
    public ExpertProfileDetailResponse toDetailResponse(Expert expert,
                                                         List<VerificationDocument> documents,
                                                         List<ExpertPriceSummary> prices) {
        return ExpertProfileDetailResponse.builder()
                .expertId(expert.getExpertId())
                .userId(expert.getUserId())
                .specialty(expert.getSpecialty())
                .experienceYears(expert.getExperienceYears())
                .professionalTitle(expert.getProfessionalTitle())
                .workplace(expert.getWorkplace())
                .consultationScope(expert.getConsultationScope())
                .verificationStatus(expert.getVerificationStatus())
                .verifiedAt(expert.getVerifiedAt())
                .verifiedBy(expert.getVerifiedBy())
                .ratingAvg(expert.getRatingAvg())
                .reviewCount(expert.getReviewCount())
                .verificationDocuments(documents.stream()
                        .map(this::toVerificationDocumentSummary)
                        .collect(Collectors.toList()))
                .pricing(prices)
                .createdAt(expert.getCreatedAt())
                .updatedAt(expert.getUpdatedAt())
                .build();
    }

    /**
     * Map VerificationDocument to summary DTO.
     */
    public ExpertProfileDetailResponse.VerificationDocumentSummary toVerificationDocumentSummary(
            VerificationDocument doc) {
        return ExpertProfileDetailResponse.VerificationDocumentSummary.builder()
                .credentialId(doc.getCredentialId())
                .credentialType(doc.getCredentialType() != null ? doc.getCredentialType().name() : null)
                .fileName(doc.getFileName())
                .fileSize(doc.getFileSize())
                .reviewStatus(doc.getReviewStatus() != null ? doc.getReviewStatus().name() : null)
                .uploadedAt(doc.getCreatedAt())
                .build();
    }

    /**
     * Map ExpertReview entity to DTO.
     */
    public ExpertReviewDTO toReviewDTO(ExpertReview review) {
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

    /**
     * Map AvailabilitySlot to DTO.
     */
    public AvailabilitySlotDTO toSlotDTO(AvailabilitySlot slot) {
        return AvailabilitySlotDTO.builder()
                .availabilityId(slot.getAvailabilityId())
                .expertId(slot.getExpertId())
                .slotStart(slot.getSlotStart())
                .slotEnd(slot.getSlotEnd())
                .channelType(slot.getChannelType())
                .status(slot.getStatus())
                .bookingId(slot.getBookingId())
                .createdAt(slot.getCreatedAt())
                .updatedAt(slot.getUpdatedAt())
                .build();
    }

    /**
     * Create Expert entity from basic data (no user ID).
     * Used by service layer.
     */
    public Expert toEntity(String specialty, Integer experienceYears, String professionalTitle,
                           String workplace, String consultationScope) {
        return Expert.builder()
                .specialty(specialty)
                .experienceYears(experienceYears)
                .professionalTitle(professionalTitle)
                .workplace(workplace)
                .consultationScope(consultationScope)
                .verificationStatus(com.carebridge.backend.expert.enums.ExpertVerificationStatus.PENDING_VERIFICATION)
                .build();
    }
}
