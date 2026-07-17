package com.carebridge.backend.triage;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TriageRecommendationCodeTest {

    @Test
    void mapsAllRiskOutcomesToSafeRecommendationCodes() {
        assertThat(TriageRecommendationCode.forRisk("RED")).isEqualTo("SEEK_EMERGENCY_CARE");
        assertThat(TriageRecommendationCode.forRisk("YELLOW")).isEqualTo("CONTACT_HEALTHCARE_PROVIDER");
        assertThat(TriageRecommendationCode.forRisk("GREEN")).isEqualTo("MONITOR_AT_HOME");
        assertThat(TriageRecommendationCode.forRisk("NEED_MORE_INFO")).isEqualTo("PROVIDE_MORE_INFORMATION");
        assertThat(TriageRecommendationCode.forRisk(null)).isEqualTo("PROVIDE_MORE_INFORMATION");
        assertThat(TriageRecommendationCode.forRisk("PREGNANCY_RULES_NEED_CLINICAL_REVIEW"))
                .isEqualTo("PROVIDE_MORE_INFORMATION");
    }
}
