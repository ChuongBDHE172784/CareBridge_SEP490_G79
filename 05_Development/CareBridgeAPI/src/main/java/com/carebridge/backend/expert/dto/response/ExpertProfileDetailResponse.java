package com.carebridge.backend.expert.dto.response;

import com.carebridge.backend.expert.verificationstatus.VerificationStatus;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpertProfileDetailResponse {

	private UUID expertProfileId;
	private UUID userId;
	private String displayName;
	private String specialty;
	private String professionalTitle;
	private Integer experienceYears;
	private String workplace;
	private String consultationScope;
	private VerificationStatus verificationStatus;
	private boolean isConsultationEligible;
	private LocalDateTime verifiedAt;
	private UUID verifiedBy;
	private BigDecimal ratingAvg;
	private String avatarUrl;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
