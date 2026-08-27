package com.carebridge.backend.aimoderation.dto.request;

import com.carebridge.backend.aimoderation.entity.AiFeedbackVerdict;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AiFeedbackRequest(
        @NotNull AiFeedbackVerdict verdict,
        @Size(max = 500) String note
) {
}
