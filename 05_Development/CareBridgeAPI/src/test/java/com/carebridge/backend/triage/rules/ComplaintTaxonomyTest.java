package com.carebridge.backend.triage.rules;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * P1-T8 — out-of-scope complaint taxonomy, mirroring {@code tests/test_complaint_taxonomy.py}.
 */
class ComplaintTaxonomyTest {

    private final ComplaintTaxonomy taxonomy = new ComplaintTaxonomy();

    @ParameterizedTest
    @CsvSource({
            "'Tôi bị đau cổ tay sau khi tập thể thao',MUSCULOSKELETAL_NON_REPRODUCTIVE",
            "'Tôi bị đau răng mấy hôm nay',DENTAL",
            "'Em bị mụn trứng cá nhiều',DERMATOLOGY_NON_REPRODUCTIVE",
            "'Tôi bị đau mắt đỏ',OPHTHALMOLOGY_NON_REPRODUCTIVE",
            "'Tôi bị viêm xoang',ENT_NON_REPRODUCTIVE"})
    @DisplayName("Known non-reproductive complaints are classified")
    void knownComplaintsAreClassified(String message, String category) {
        assertThat(taxonomy.classify(message, Map.of()).categoryId()).isEqualTo(category);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Tôi thấy mệt", "Có gì đó không ổn", "Tôi lo lắng quá"})
    @DisplayName("An unrecognised complaint is not out of scope")
    void unrecognisedComplaintIsNotOutOfScope(String message) {
        // Unknown means unknown — never a dismissal.
        var classification = taxonomy.classify(message, Map.of());
        assertThat(classification.categoryId()).isNull();
        assertThat(classification.isConfirmedNonReproductive()).isFalse();
    }

    @Test
    @DisplayName("A swollen painful leg is not musculoskeletal here")
    void swollenLegIsNotMusculoskeletal() {
        // After birth this is a green blocker, not an orthopaedic complaint.
        var classification = taxonomy.classify("Tôi bị đau gối",
                Map.of("UNILATERAL_LEG_SWELLING_PAIN", "PRESENT"));
        assertThat(classification.categoryId()).isNull();
        assertThat(classification.disqualified()).contains("MUSCULOSKELETAL_NON_REPRODUCTIVE");
    }

    @Test
    @DisplayName("Blurred vision is not an eye complaint")
    void blurredVisionIsNotAnEyeComplaint() {
        assertThat(taxonomy.classify("Tôi bị đau mắt đỏ",
                Map.of("VISUAL_DISTURBANCE", "PRESENT")).categoryId()).isNull();
    }

    @Test
    @DisplayName("A fall with bleeding is not a sports injury")
    void fallWithBleedingIsNotASportsInjury() {
        var classification = taxonomy.classify("Tôi bị ngã xe hôm qua",
                Map.of("VAGINAL_BLEEDING", "PRESENT"));
        assertThat(classification.categoryId()).isNull();
        assertThat(classification.disqualified()).contains("INJURY_FROM_EXERCISE_OR_ACCIDENT");
    }

    @Test
    @DisplayName("Out of scope needs a complete safety screen")
    void outOfScopeNeedsCompleteSafetyScreen() {
        // "Đau cổ tay" is not instantly out of scope — first know they are not short of breath.
        var classification = taxonomy.classify("Tôi bị đau cổ tay sau khi tập thể thao", Map.of());
        assertThat(taxonomy.mayReturnOutOfScope(classification, false, false)).isFalse();
        assertThat(taxonomy.mayReturnOutOfScope(classification, true, false)).isTrue();
    }

    @Test
    @DisplayName("Reproductive evidence blocks out of scope")
    void reproductiveEvidenceBlocksOutOfScope() {
        var classification = taxonomy.classify("Tôi bị đau cổ tay sau khi tập thể thao", Map.of());
        assertThat(taxonomy.mayReturnOutOfScope(classification, true, true)).isFalse();
    }

    @Test
    @DisplayName("An unclassified complaint can never be out of scope")
    void unclassifiedCanNeverBeOutOfScope() {
        var classification = taxonomy.classify("Tôi thấy mệt", Map.of());
        assertThat(taxonomy.mayReturnOutOfScope(classification, true, false)).isFalse();
    }
}
