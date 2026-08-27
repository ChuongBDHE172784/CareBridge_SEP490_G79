package com.carebridge.backend.emergency.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class EmergencyAlertDeliveryOwnerTest {
    @Test
    void deliveryRequiresEmergencyOwnerSeparateFromRecipient() {
        UUID owner = UUID.randomUUID();
        UUID recipient = UUID.randomUUID();
        EmergencyAlertDelivery delivery = EmergencyAlertDelivery.builder()
                .userId(owner).recipientUserId(recipient).build();
        delivery.prepareCanonicalAction();

        assertThat(delivery.getUserId()).isEqualTo(owner);
        assertThat(delivery.getRecipientUserId()).isEqualTo(recipient);
        assertThat(delivery.getUserId()).isNotEqualTo(delivery.getRecipientUserId());
    }

    @Test
    void missingOwnerIsRejectedInsteadOfFallingBackToRecipient() {
        EmergencyAlertDelivery delivery = EmergencyAlertDelivery.builder()
                .recipientUserId(UUID.randomUUID()).build();
        assertThatThrownBy(delivery::prepareCanonicalAction)
                .isInstanceOf(IllegalStateException.class);
    }
}
