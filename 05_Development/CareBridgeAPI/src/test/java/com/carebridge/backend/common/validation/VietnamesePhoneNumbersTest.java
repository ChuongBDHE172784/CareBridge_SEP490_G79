package com.carebridge.backend.common.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class VietnamesePhoneNumbersTest {

    @Test
    void normalizeToE164_AcceptsCanonicalLocalAndFormattedForms() {
        assertThat(VietnamesePhoneNumbers.normalizeToE164("+84901234567"))
                .isEqualTo("+84901234567");
        assertThat(VietnamesePhoneNumbers.normalizeToE164("0901234567"))
                .isEqualTo("+84901234567");
        assertThat(VietnamesePhoneNumbers.normalizeToE164("84 90 123 4567"))
                .isEqualTo("+84901234567");
        assertThat(VietnamesePhoneNumbers.normalizeToE164("(+84) 90-123.4567"))
                .isEqualTo("+84901234567");
    }

    @Test
    void normalizeToE164_ReturnsNullForMissingValues() {
        assertThat(VietnamesePhoneNumbers.normalizeToE164(null)).isNull();
        assertThat(VietnamesePhoneNumbers.normalizeToE164("   ")).isNull();
    }

    @Test
    void normalizeToE164_RejectsForeignMalformedAndInvalidPrefixNumbers() {
        assertThatThrownBy(() -> VietnamesePhoneNumbers.normalizeToE164("+14155552671"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> VietnamesePhoneNumbers.normalizeToE164("+84123456789"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> VietnamesePhoneNumbers.normalizeToE164("09012ABC67"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> VietnamesePhoneNumbers.normalizeToE164("+849012345678"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
