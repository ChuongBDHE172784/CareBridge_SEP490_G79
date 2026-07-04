package com.carebridge.backend.expertverification.dto.response;

import com.carebridge.backend.expertverification.reviewstatus.ReviewStatus;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentReviewResponse {

    private UUID credentialId;
    private UUID expertProfileId;
    private String credentialType;
    private ReviewStatus reviewStatus;
    private String reviewNote;
    private UUID reviewedBy;
    private LocalDateTime reviewedAt;
}
