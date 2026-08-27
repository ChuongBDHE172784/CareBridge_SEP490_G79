package com.carebridge.backend.expertverification.adapter;

import com.carebridge.backend.expertverification.enums.FaceVerificationStatus;
import java.math.BigDecimal;

public record FaceVerificationResult(
        FaceVerificationStatus status,
        BigDecimal similarity,
        BigDecimal threshold,
        String providerErrorCode) {
}
