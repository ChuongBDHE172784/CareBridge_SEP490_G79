package com.carebridge.backend.expertverification.enums;

public enum FaceVerificationStatus {
    DISABLED,
    MATCHED,
    NOT_MATCHED,
    RETRYABLE_ERROR,
    NO_FACE,
    MULTIPLE_FACES
}
