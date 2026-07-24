package com.carebridge.backend.consultation.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.carebridge.backend.consultation.context.dto.TriageExpertHandoffCreateRequest;
import com.carebridge.backend.consultation.context.exception.TriageExpertHandoffException;
import com.carebridge.backend.consultation.context.policy.TriageExpertHandoffPolicy;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TriageExpertHandoffPolicyTest {

    private final TriageExpertHandoffPolicy policy = new TriageExpertHandoffPolicy();

    @Test
    void sanitizerNormalizesUnicodeWhitespaceAndControlOrFormatCharacters() {
        String raw = "  Cafe\u0301\u00a0\t risk\u200B summary\r\n ";

        String sanitized = policy.sanitizeSummary(raw);

        assertThat(sanitized).isEqualTo("Café risk summary");
    }

    @Test
    void sanitizerRejectsBlankCanonicalSummary() {
        assertThatThrownBy(() -> policy.sanitizeSummary(" \u00a0\u200B\r\n"))
                .isInstanceOfSatisfying(
                        TriageExpertHandoffException.class,
                        error -> {
                            assertThat(error.getCode()).isEqualTo("HANDOFF-008");
                            assertThat(error.getHttpStatus().value()).isEqualTo(422);
                        });
    }

    @Test
    void sanitizerUsesASeparatorForInvisibleControlBoundaries() {
        assertThat(policy.sanitizeSummary("no\u0000t pregnant"))
                .isEqualTo("no t pregnant");
    }

    @Test
    void sanitizerTruncatesByCodePointWithoutSplittingSurrogatePairs() {
        String raw = "a".repeat(498) + "😀bc";

        String sanitized = policy.sanitizeSummary(raw);

        assertThat(sanitized.codePointCount(0, sanitized.length())).isEqualTo(500);
        assertThat(sanitized).isEqualTo("a".repeat(498) + "😀…");
    }

    @Test
    void createPolicyRequiresExactVersionAndExplicitConsent() {
        UUID key = UUID.randomUUID();
        UUID expertId = UUID.randomUUID();

        policy.assertCreateRequest(new TriageExpertHandoffCreateRequest(
                key, expertId, true, TriageExpertHandoffPolicy.POLICY_VERSION));

        assertThatThrownBy(() -> policy.assertCreateRequest(
                        new TriageExpertHandoffCreateRequest(key, expertId, false,
                                TriageExpertHandoffPolicy.POLICY_VERSION)))
                .isInstanceOfSatisfying(
                        TriageExpertHandoffException.class,
                        error -> assertThat(error.getCode()).isEqualTo("HANDOFF-001"));
        assertThatThrownBy(() -> policy.assertCreateRequest(
                        new TriageExpertHandoffCreateRequest(key, expertId, true, "stale")))
                .isInstanceOfSatisfying(
                        TriageExpertHandoffException.class,
                        error -> assertThat(error.getCode()).isEqualTo("HANDOFF-005"));
    }

    @Test
    void consentLabelsAreFixedAndContainNoPatientData() {
        assertThat(policy.sharedFields()).containsExactly(
                "YELLOW risk",
                "Lifecycle stage",
                "Risk summary",
                "Approved source metadata");
        assertThat(policy.excludedFields()).containsExactly(
                "Raw answers or symptoms",
                "Normalized symptoms",
                "Red flags",
                "Claims",
                "Health notes",
                "AI payload",
                "Identifiers or tokens",
                "Route or origin data",
                "Pending or unreviewed sources",
                "Surplus health data");
    }
}
