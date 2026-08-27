package com.carebridge.backend.safety;

public enum SafetyEventStatus {
    OPEN,
    TEST_OPEN,
    CONFIRMED_SAFE,
    FALSE_POSITIVE,
    TIMED_OUT,
    ESCALATION_REQUESTED,
    EMERGENCY_ALERT_SENT
}
