package com.carebridge.backend.emergency.service;

import java.util.UUID;

public record PreparedAlertDelivery(
        UUID deliveryId,
        UUID notificationRecordId,
        boolean alreadySuccessful,
        int priorAttempts
) {}
