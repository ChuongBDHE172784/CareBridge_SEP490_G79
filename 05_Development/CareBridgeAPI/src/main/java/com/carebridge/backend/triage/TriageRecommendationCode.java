package com.carebridge.backend.triage;

/** Maps a locked deterministic risk level to its public action code. */
public final class TriageRecommendationCode {
    private TriageRecommendationCode() {
    }

    public static String forRisk(String riskLevel) {
        return switch (String.valueOf(riskLevel)) {
            case "RED" -> "SEEK_EMERGENCY_CARE";
            case "YELLOW" -> "CONTACT_HEALTHCARE_PROVIDER";
            case "GREEN" -> "MONITOR_AT_HOME";
            default -> "PROVIDE_MORE_INFORMATION";
        };
    }
}
