package com.carebridge.backend.common.dev;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DevDataSeederPasswordTest {

    @Test
    void blankAndHistoricalDefaultPasswordsFailClosed() {
        assertThatThrownBy(() -> DevDataSeeder.validateSeedPassword(" "))
                .isInstanceOf(IllegalStateException.class)
                .message().contains("explicit non-default");
        assertThatThrownBy(() -> DevDataSeeder.validateSeedPassword("Test" + "@1234"))
                .isInstanceOf(IllegalStateException.class)
                .message().contains("explicit non-default");
    }

    @Test
    void explicitNonDefaultPasswordIsAccepted() {
        assertThatCode(() -> DevDataSeeder.validateSeedPassword(
                "Synthetic-Only-Strong-Passphrase-6-10"))
                .doesNotThrowAnyException();
    }
}
