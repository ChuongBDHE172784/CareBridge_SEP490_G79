package com.carebridge.backend.family.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class InviteStatusTest {

    @Test
    void inviteStatus_containsRejectedAndExpired() {
        assertThatCode(() -> InviteStatus.valueOf("REJECTED"))
                .doesNotThrowAnyException();
        assertThatCode(() -> InviteStatus.valueOf("EXPIRED"))
                .doesNotThrowAnyException();
    }

    @Test
    void inviteStatus_hasExactlyFiveValues() {
        assertThat(InviteStatus.values()).hasSize(5);
    }
}
