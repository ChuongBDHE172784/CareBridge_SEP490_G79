package com.carebridge.backend.expertverification.dto.response;

import com.carebridge.backend.expertverification.reviewstatus.ReviewStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.UUID;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentReviewResponse {

	private UUID credentialId;
	private UUID expertProfileId;
	private String credentialType;
	private String credentialNumber;
	private String issuer;
	private LocalDate issuedDate;
	private LocalDate expiryDate;
	private String fileUrl;
	private UUID fileId;
	private LocalDateTime createdAt;
	private ReviewStatus reviewStatus;
	private String reviewNote;
	private UUID reviewedBy;
	private LocalDateTime reviewedAt;
	private String expertName;
	private String specialty;
	private String professionalTitle;
	private Integer experienceYears;
	private String workplace;
	private String phone;
	private String email;
	private BigDecimal ratingAvg;
	private String avatarUrl;
}
