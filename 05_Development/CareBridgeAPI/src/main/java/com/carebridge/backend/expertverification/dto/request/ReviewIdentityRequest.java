package com.carebridge.backend.expertverification.dto.request;

import com.carebridge.backend.expertverification.enums.IdentityReviewStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReviewIdentityRequest {
    @NotNull
    private IdentityReviewStatus reviewStatus;

    @Size(max = 2000)
    private String reason;
}
