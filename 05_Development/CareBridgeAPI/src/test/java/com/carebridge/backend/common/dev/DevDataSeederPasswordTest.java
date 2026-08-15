package com.carebridge.backend.common.dev;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DevDataSeederPasswordTest {

    @Test
    void blankPasswordsFailClosed() {
        assertThatThrownBy(() -> DevDataSeeder.validateSeedPassword(" "))
                .isInstanceOf(IllegalStateException.class)
                .message().contains("non-blank");
    }

    @Test
    void configuredPasswordsAreAccepted() {
        assertThatCode(() -> DevDataSeeder.validateSeedPassword(
                "Test" + "@1234"))
                .doesNotThrowAnyException();
    }
}
