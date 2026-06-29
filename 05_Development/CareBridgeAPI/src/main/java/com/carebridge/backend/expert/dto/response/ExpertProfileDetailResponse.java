package com.carebridge.backend.expert.dto.response;

import com.carebridge.backend.expert.enums.ExpertVerificationStatus;
import com.carebridge.backend.expert.enums.SettlementStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Detailed expert profile response for expert owner or admin.
 * Includes internal details like verification documents.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpertProfileDetailResponse {

    private Long expertId;
    private Long userId;
    private String specialty;
    private Integer experienceYears;
    private String professionalTitle;
    private String workplace;
    private String consultationScope;
    private ExpertVerificationStatus verificationStatus;
    private Instant verifiedAt;
    private Long verifiedBy;
    private Double ratingAvg;
    private Integer reviewCount;
    private List<VerificationDocumentSummary> verificationDocuments;
    private List<ExpertPriceSummary> pricing;
    private Instant createdAt;
    private Instant updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VerificationDocumentSummary {
        private Long credentialId;
        private String credentialType;
        private String fileName;
        private Long fileSize;
        private String reviewStatus;
        private Instant uploadedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExpertPriceSummary {
        private Long expertPriceId;
        private String channelType;
        private Integer durationMinutes;
        private Integer priceAmount;
        private String currency;
        private String status;
    }
}
