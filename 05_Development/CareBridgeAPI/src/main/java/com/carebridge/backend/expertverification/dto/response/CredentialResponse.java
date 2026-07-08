package com.carebridge.backend.expertverification.dto.response;

import com.carebridge.backend.expertverification.reviewstatus.ReviewStatus;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CredentialResponse {

    private UUID credentialId;
    private UUID expertProfileId;
    private String credentialType;
    private String credentialNumber;
    private String issuer;
    private LocalDate issuedDate;
    private LocalDate expiryDate;
    private String fileUrl;
    private ReviewStatus reviewStatus;
    private String reviewNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
