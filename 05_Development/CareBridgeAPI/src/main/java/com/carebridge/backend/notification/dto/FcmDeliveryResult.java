package com.carebridge.backend.notification.dto;

public record FcmDeliveryResult(
        boolean success,
        String messageId,
        String errorCode,
        int attempts
) {
    public static FcmDeliveryResult success(String messageId, int attempts) {
        return new FcmDeliveryResult(true, messageId, null, attempts);
    }

    public static FcmDeliveryResult failed(String errorCode, int attempts) {
        return new FcmDeliveryResult(false, null, errorCode, attempts);
    }
}
