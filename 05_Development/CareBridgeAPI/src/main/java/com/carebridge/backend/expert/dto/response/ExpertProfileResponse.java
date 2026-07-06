package com.carebridge.backend.expert.dto.response;

import com.carebridge.backend.expert.verificationstatus.VerificationStatus;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpertProfileResponse {

    private UUID expertProfileId;
    private UUID userId;
    private String specialty;
    private String professionalTitle;
    private Integer experienceYears;
    private String workplace;
    private String consultationScope;
    private VerificationStatus verificationStatus;
    private LocalDateTime verifiedAt;
    private UUID verifiedBy;
    private java.math.BigDecimal ratingAvg;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
