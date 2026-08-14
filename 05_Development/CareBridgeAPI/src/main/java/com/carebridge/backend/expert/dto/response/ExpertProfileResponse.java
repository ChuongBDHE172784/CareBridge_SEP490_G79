package com.carebridge.backend.expert.dto.response;

import com.carebridge.backend.expert.verificationstatus.VerificationStatus;
import com.carebridge.backend.expert.truststatus.TrustStatus;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpertProfileResponse {

	private UUID expertProfileId;
	private UUID userId;
	private String displayName;
	private String specialty;
	private String specialtyId;
	private List<String> specialtyIds;
	private String professionalTitle;
	private Integer experienceYears;
	private String workplace;
	private String workplaceProvinceId;
	private String hospitalId;
	private String consultationScope;
	private VerificationStatus verificationStatus;
	private TrustStatus trustStatus;
	private boolean isConsultationEligible;
	private LocalDateTime verifiedAt;
	private UUID verifiedBy;
	private BigDecimal ratingAvg;
	private BigDecimal consultationFeeVnd;
	private String avatarUrl;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
