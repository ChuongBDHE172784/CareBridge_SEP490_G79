package com.carebridge.backend.emergency.service;

import com.carebridge.backend.notification.dto.FcmDeliveryResult;

/** Provider result and whether its immutable DELIVERY result was recorded. */
public record FencedAlertDelivery(FcmDeliveryResult result, boolean recorded) {
}
