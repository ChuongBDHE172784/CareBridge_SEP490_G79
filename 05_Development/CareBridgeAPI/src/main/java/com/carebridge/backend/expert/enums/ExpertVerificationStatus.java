package com.carebridge.backend.expert.enums;

/**
 * Expert verification status enumeration.
 * Follows state machine: PENDING_VERIFICATION -> APPROVED/REJECTED/SUSPENDED
 */
public enum ExpertVerificationStatus {
    PENDING_VERIFICATION,
    APPROVED,
    REJECTED,
    SUSPENDED
}
