package com.carebridge.backend.expertverification.registry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.carebridge.backend.expertverification.registry.LicenseNumberNormalizer.NormalizedLicense;
import com.carebridge.backend.expertverification.registry.RegistryMatcher.MatchOutcome;
import com.carebridge.backend.expertverification.registry.RegistryMatcher.MatchResult;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Covers every branch of the matching table in MF-05 Spec 05 §7. */
class RegistryMatcherTest {

    private static NormalizedLicense declared(String raw) {
        return LicenseNumberNormalizer.normalize(raw).orElseThrow();
    }

    private static RegistryRow row(String fullName, String licenseNo) {
        return new RegistryRow(fullName, "Việt Nam", licenseNo, "Khám bệnh, chữa bệnh chuyên khoa Nhi.",
                "Đang hoạt động", "178224", "<tr></tr>");
    }

    @Test
    void exactNumberAndNameGivesTheHighestConfidence() {
        MatchOutcome outcome = RegistryMatcher.match(
                List.of(row("Phạm Thị Bình Minh", "000001/HCM-CCHN")),
                declared("000001/HCM-CCHN"),
                "Phạm Thị Bình Minh");

        assertThat(outcome.result()).isEqualTo(MatchResult.MATCHED);
        assertThat(outcome.confidence()).isEqualTo(0.95);
        assertThat(outcome.redFlag()).isFalse();
        assertThat(outcome.nameSimilarity()).isEqualTo(1.0);
    }

    @Test
    void toleratesMissingDiacriticsAndCasingBetweenSources() {
        MatchOutcome outcome = RegistryMatcher.match(
                List.of(row("PHAM THI BINH MINH", "000001/HCM-CCHN")),
                declared("000001/HCM-CCHN"),
                "Phạm Thị Bình Minh");

        assertThat(outcome.result()).isEqualTo(MatchResult.MATCHED);
    }

    /** The single most suspicious outcome: the number is real but registered to someone else. */
    @Test
    void raisesARedFlagWhenTheNumberExistsUnderADifferentName() {
        MatchOutcome outcome = RegistryMatcher.match(
                List.of(row("Nguyễn Văn An", "000001/HCM-CCHN")),
                declared("000001/HCM-CCHN"),
                "Phạm Thị Bình Minh");

        assertThat(outcome.result()).isEqualTo(MatchResult.FUZZY);
        assertThat(outcome.confidence()).isEqualTo(0.60);
        assertThat(outcome.redFlag()).isTrue();
        assertThat(outcome.matchedRow()).isNotNull();
    }

    @Test
    void treatsCchnAndGphnAsTheSameDocument() {
        MatchOutcome outcome = RegistryMatcher.match(
                List.of(row("Huỳnh Mỹ Anh", "000001/HCM-GPHN")),
                declared("000001/HCM-CCHN"),
                "Huỳnh Mỹ Anh");

        assertThat(outcome.result()).isEqualTo(MatchResult.MATCHED);
    }

    @Test
    void fallsBackToTheMaskedSerialWhenTheNameAlsoAgrees() {
        MatchOutcome outcome = RegistryMatcher.match(
                List.of(row("Trần Văn Bình", "..8146/ĐL-CCHN")),
                declared("128146/DL-CCHN"),
                "Trần Văn Bình");

        assertThat(outcome.result()).isEqualTo(MatchResult.FUZZY);
        assertThat(outcome.confidence()).isEqualTo(0.70, within(1e-9));
        assertThat(outcome.redFlag()).isFalse();
    }

    @Test
    void refusesToGuessOnAMaskedSerialWhenTheNameDiffers() {
        MatchOutcome outcome = RegistryMatcher.match(
                List.of(row("Người Khác", "..8146/ĐL-CCHN")),
                declared("128146/DL-CCHN"),
                "Trần Văn Bình");

        assertThat(outcome.result()).isEqualTo(MatchResult.NOT_FOUND);
        assertThat(outcome.matchedRow()).isNull();
    }

    @Test
    void doesNotMatchAMaskedSerialFromAnotherProvince() {
        MatchOutcome outcome = RegistryMatcher.match(
                List.of(row("Trần Văn Bình", "..8146/KG-CCHN")),
                declared("128146/DL-CCHN"),
                "Trần Văn Bình");

        assertThat(outcome.result()).isEqualTo(MatchResult.NOT_FOUND);
    }

    @Test
    void noRowsMeansNotFoundWithZeroConfidence() {
        MatchOutcome outcome = RegistryMatcher.match(List.of(), declared("000001/HCM-CCHN"), "Ai Đó");

        assertThat(outcome.result()).isEqualTo(MatchResult.NOT_FOUND);
        assertThat(outcome.confidence()).isZero();
        assertThat(outcome.redFlag()).isFalse();
    }

    @Test
    void unrelatedRowsAreNotForcedIntoAMatch() {
        MatchOutcome outcome = RegistryMatcher.match(
                List.of(row("Ai Đó", "000009/KG-CCHN")),
                declared("000001/HCM-CCHN"),
                "Phạm Thị Bình Minh");

        assertThat(outcome.result()).isEqualTo(MatchResult.NOT_FOUND);
    }
}
