package com.carebridge.backend.ai.service;

import com.carebridge.backend.triage.event.IntakeSessionCompleted;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IntakeSessionCompletedHandler {

    private final IStructuredIntakeService structuredIntakeService;

    @EventListener
    public void onIntakeSessionCompleted(IntakeSessionCompleted event) {
        structuredIntakeService.extract(event);
    }
}
