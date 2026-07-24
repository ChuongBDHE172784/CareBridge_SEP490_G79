package com.carebridge.backend.triage;

public enum OriginAction {
    RETURN_TO_MOTHER_JOURNEY,
    RETURN_TO_BABY_PROFILE;

    public static OriginAction forDashboard(OriginDashboard dashboard) {
        return dashboard == OriginDashboard.MOTHER_JOURNEY
                ? RETURN_TO_MOTHER_JOURNEY
                : RETURN_TO_BABY_PROFILE;
    }
}
