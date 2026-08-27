package com.carebridge.backend.aimoderation.dto.response;

import com.carebridge.backend.aimoderation.entity.AiFeedbackVerdict;
import java.time.Instant;
import java.util.UUID;

public record AiFeedbackResponse(
        UUID feedbackId,
        UUID assessmentId,
        AiFeedbackVerdict verdict,
        String note,
        Instant createdAt
) {
}
