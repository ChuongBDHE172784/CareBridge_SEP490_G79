package com.carebridge.backend.journey.service;

import com.carebridge.backend.triage.event.IntakeSessionCompleted;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IntakeSafetyOutcomeProjectionHandler {
    private final ILifecycleSafetyOutcomeProjector projector;

    @EventListener
    public void onIntakeSessionCompleted(IntakeSessionCompleted event) {
        projector.ensureProjected(event.sessionId(), event.userId());
    }
}
