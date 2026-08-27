package com.carebridge.backend.safety.service;

import com.carebridge.backend.safety.event.SafetyConfigChanged;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SafetyConfigChangedHandler {

    private final IFallDetectionService fallDetectionService;

    @EventListener
    public void onSafetyConfigChanged(SafetyConfigChanged event) {
        SafetyConfigChanged.Payload payload = event.payload();
        if (payload.fallDetectionEnabled()) {
            fallDetectionService.enable(payload.userId(), payload.sensitivityLevel());
        } else {
            fallDetectionService.disable(payload.userId());
        }
    }
}
