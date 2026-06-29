package com.carebridge.backend.audit.entity;

public enum SecurityEventType {
    LOGIN_FAILED,
    PERMISSION_DENIED,
    SUSPICIOUS_ACTIVITY,
    TOKEN_REVOKED,
    OTP_ATTEMPT_LIMIT_EXCEEDED
}
