package com.carebridge.backend.aimoderation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.carebridge.backend.aimoderation.dto.AiVerdict;
import com.carebridge.backend.aimoderation.entity.AiClassification;
import com.carebridge.backend.aimoderation.entity.AiModerationPolicy;
import com.carebridge.backend.aimoderation.entity.AiPolicySeverity;
import com.carebridge.backend.aimoderation.entity.AiViolationCategory;
import com.carebridge.backend.aimoderation.exception.AiVerdictParseException;
import com.carebridge.backend.aimoderation.policy.AiVerdictParser;
import com.carebridge.backend.content.entity.ReportCategory;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AiVerdictParserTest {

    private final AiVerdictParser parser = new AiVerdictParser(new ObjectMapper());

    private static AiModerationPolicy policy(String code, AiPolicySeverity severity) {
        return AiModerationPolicy.builder()
                .policyCode(code)
                .name(code)
                .detectionGuidance("guidance")
                .violationCategory(AiViolationCategory.SPAM_ADVERTISING)
                .reportCategory(ReportCategory.SPAM)
                .severity(severity)
                .applicableTargetTypes("QUESTION,ANSWER")
                .confidenceThreshold(new BigDecimal("0.700"))
                .build();
    }

    private static final Map<String, AiModerationPolicy> POLICIES =
            Map.of("SPAM_ADVERTISING", policy("SPAM_ADVERTISING", AiPolicySeverity.MEDIUM));

    private static final String CONTENT = "Mua ngay sữa thần kỳ giảm giá 90%! Liên hệ zalo 0900000000";

    // Scenario 3: valid structured output parses, with server-authoritative category/severity
    @Test
    void validJson_parsesWithServerSidePolicyAttributes() {
        String json = """
                {"classification":"VIOLATION","confidence":0.91,
                 "matchedPolicies":[{"policyCode":"SPAM_ADVERTISING","confidence":0.9,
                   "evidence":["Mua ngay sữa thần kỳ"],"explanation":"spam bán hàng"}],
                 "recommendedAction":"REVIEW","explanation":"quảng cáo trá hình"}
                """;
        AiVerdict verdict = parser.parse(json, POLICIES, CONTENT);
        assertThat(verdict.classification()).isEqualTo(AiClassification.VIOLATION);
        assertThat(verdict.matchedPolicies()).hasSize(1);
        assertThat(verdict.matchedPolicies().get(0).severity()).isEqualTo(AiPolicySeverity.MEDIUM);
        assertThat(verdict.matchedPolicies().get(0).evidence()).containsExactly("Mua ngay sữa thần kỳ");
    }

    // Scenario 4: malformed JSON is a parse failure — never silently SAFE
    @Test
    void malformedJson_throwsParseException() {
        assertThatThrownBy(() -> parser.parse("SAFE — everything fine!", POLICIES, CONTENT))
                .isInstanceOf(AiVerdictParseException.class);
    }

    @Test
    void missingClassification_throwsParseException() {
        assertThatThrownBy(() -> parser.parse(
                "{\"confidence\":0.5,\"matchedPolicies\":[],\"recommendedAction\":\"REVIEW\"}",
                POLICIES, CONTENT))
                .isInstanceOf(AiVerdictParseException.class);
    }

    @Test
    void confidenceOutOfRange_throwsParseException() {
        assertThatThrownBy(() -> parser.parse(
                "{\"classification\":\"SAFE\",\"confidence\":1.7,\"matchedPolicies\":[],"
                        + "\"recommendedAction\":\"NO_ACTION\"}",
                POLICIES, CONTENT))
                .isInstanceOf(AiVerdictParseException.class);
    }

    // Scenario 11: fabricated policy codes and fabricated evidence are dropped
    @Test
    void unknownPolicyCode_isDropped() {
        String json = """
                {"classification":"VIOLATION","confidence":0.9,
                 "matchedPolicies":[{"policyCode":"TOTALLY_FAKE","confidence":0.9}],
                 "recommendedAction":"ESCALATE"}
                """;
        AiVerdict verdict = parser.parse(json, POLICIES, CONTENT);
        assertThat(verdict.matchedPolicies()).isEmpty();
        // VIOLATION with no surviving matches is downgraded to UNCERTAIN, not trusted blindly
        assertThat(verdict.classification()).isEqualTo(AiClassification.UNCERTAIN);
    }

    @Test
    void fabricatedEvidence_isDropped_realEvidenceKept() {
        String json = """
                {"classification":"VIOLATION","confidence":0.9,
                 "matchedPolicies":[{"policyCode":"SPAM_ADVERTISING","confidence":0.9,
                   "evidence":["câu này không hề có trong nội dung","giảm giá 90%"]}],
                 "recommendedAction":"REVIEW"}
                """;
        AiVerdict verdict = parser.parse(json, POLICIES, CONTENT);
        assertThat(verdict.matchedPolicies().get(0).evidence()).containsExactly("giảm giá 90%");
    }

    @Test
    void invalidRecommendedAction_becomesNull_neverCrashes() {
        String json = """
                {"classification":"SAFE","confidence":0.2,"matchedPolicies":[],
                 "recommendedAction":"BAN_THE_USER_FOREVER"}
                """;
        AiVerdict verdict = parser.parse(json, POLICIES, CONTENT);
        assertThat(verdict.recommendedAction()).isNull();
        assertThat(verdict.classification()).isEqualTo(AiClassification.SAFE);
    }
}
