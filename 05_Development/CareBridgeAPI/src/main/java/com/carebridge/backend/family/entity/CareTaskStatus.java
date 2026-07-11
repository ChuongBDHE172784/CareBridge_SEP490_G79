package com.carebridge.backend.family.entity;

/** ADR-FAM-030 / ADR-FAM-005: UC-73 creates OPEN tasks; UC-85 drives transitions. */
public enum CareTaskStatus {
    OPEN,
    IN_PROGRESS,
    DONE,
    CANCELLED,
    NEEDS_SUPPORT;

    /**
     * UC-85 FSM — returns true if transitioning from this state to {@code target} is allowed.
     * DONE and CANCELLED are terminal states (no outbound transitions).
     * Self-transitions are allowed (idempotent — SRS E3).
     */
    public boolean canTransitionTo(CareTaskStatus target) {
        if (this == target) return true;
        return switch (this) {
            case OPEN -> target == IN_PROGRESS || target == DONE || target == NEEDS_SUPPORT;
            case IN_PROGRESS -> target == DONE || target == NEEDS_SUPPORT;
            case NEEDS_SUPPORT -> target == IN_PROGRESS || target == DONE;
            case DONE, CANCELLED -> false;
        };
    }
}
