package com.carebridge.backend.expert.enums;

/**
 * Consultation status enumeration.
 * State transitions:
 * PENDING_PAYMENT -> CONFIRMED (after payment) or CANCELLED
 * CONFIRMED -> IN_PROGRESS (session started) or CANCELLED
 * IN_PROGRESS -> COMPLETED or NO_SHOW
 */
public enum ConsultationStatus {
    PENDING_PAYMENT,
    CONFIRMED,
    CANCELLED,
    IN_PROGRESS,
    COMPLETED,
    NO_SHOW,
    RESCHEDULED
}
