package com.carebridge.backend.expertverification.dto.request;

import com.carebridge.backend.expertverification.reviewstatus.ReviewStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewCredentialRequest {

    @NotNull
    private ReviewStatus reviewStatus;

    @Size(max = 2000)
    private String reviewNote;
}
