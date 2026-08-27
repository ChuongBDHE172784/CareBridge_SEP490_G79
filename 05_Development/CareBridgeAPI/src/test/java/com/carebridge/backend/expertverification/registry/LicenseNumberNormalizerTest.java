package com.carebridge.backend.expertverification.registry;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.expertverification.registry.LicenseNumberNormalizer.NormalizedLicense;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Covers every variant listed in MF-05 Spec 05 §5, including masked serials and the Đ character. */
class LicenseNumberNormalizerTest {

    @Test
    void stripsLeadingZerosAndDashedSuffix() {
        NormalizedLicense result = LicenseNumberNormalizer.normalize("001563/HP-CCHN").orElseThrow();

        assertThat(result.serial()).isEqualTo("1563");
        assertThat(result.provinceCode()).isEqualTo("HP");
        assertThat(result.type()).isEqualTo("CCHN");
        assertThat(result.masked()).isFalse();
        assertThat(result.canonical()).isEqualTo("1563/HP");
    }

    @ParameterizedTest
    @ValueSource(strings = {"001563/HP-CCHN", "001563/HPCCHN", "1563/HP CCHN", "  1563/hp-cchn  "})
    void acceptsEverySpacingAndCasingVariant(String raw) {
        assertThat(LicenseNumberNormalizer.normalize(raw).orElseThrow().canonical()).isEqualTo("1563/HP");
    }

    @Test
    void flagsMaskedSerialAndFoldsDStroke() {
        NormalizedLicense result = LicenseNumberNormalizer.normalize("..8146/ĐL-CCHN").orElseThrow();

        assertThat(result.serial()).isEqualTo("8146");
        assertThat(result.provinceCode()).isEqualTo("DL");
        assertThat(result.masked()).isTrue();
    }

    @Test
    void treatsGphnAsAnEquivalentDocumentType() {
        NormalizedLicense cchn = LicenseNumberNormalizer.normalize("000001/HCM-CCHN").orElseThrow();
        NormalizedLicense gphn = LicenseNumberNormalizer.normalize("000001/HCM-GPHN").orElseThrow();

        // The 2023 law renamed the document; the canonical key must not distinguish the two.
        assertThat(cchn.canonical()).isEqualTo(gphn.canonical());
        assertThat(cchn.type()).isEqualTo("CCHN");
        assertThat(gphn.type()).isEqualTo("GPHN");
    }

    @Test
    void keepsMultiLetterProvinceCodes() {
        assertThat(LicenseNumberNormalizer.normalize("000002/AG-GPHN").orElseThrow().canonical())
                .isEqualTo("2/AG");
        assertThat(LicenseNumberNormalizer.normalize("000001/QNG-GPHN").orElseThrow().canonical())
                .isEqualTo("1/QNG");
    }

    @Test
    void keepsASingleZeroRatherThanEmptyingTheSerial() {
        assertThat(LicenseNumberNormalizer.normalize("000/HCM-CCHN").orElseThrow().serial()).isEqualTo("0");
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "1563", "1563/HP-XYZ", "/HP-CCHN", "1563/", "..../HCM-CCHN"})
    void returnsEmptyForUnreadableInputInsteadOfThrowing(String raw) {
        assertThat(LicenseNumberNormalizer.normalize(raw)).isEmpty();
    }

    @Test
    void returnsEmptyForNullAndBlank() {
        assertThat(LicenseNumberNormalizer.normalize(null)).isEqualTo(Optional.empty());
        assertThat(LicenseNumberNormalizer.normalize("   ")).isEmpty();
    }

    @Test
    void normalizesNamesForComparisonAcrossSources() {
        assertThat(LicenseNumberNormalizer.normalizeName("  Phạm  Thị Bình Minh "))
                .isEqualTo("pham thi binh minh");
        assertThat(LicenseNumberNormalizer.normalizeName("TẠ ĐÔNG QUÂN")).isEqualTo("ta dong quan");
        assertThat(LicenseNumberNormalizer.normalizeName(null)).isEmpty();
    }
}
