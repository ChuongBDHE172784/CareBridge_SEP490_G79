package com.carebridge.backend.ai.service;

import com.carebridge.backend.triage.event.IntakeSessionCompleted;

public interface IStructuredIntakeService {
    void extract(IntakeSessionCompleted event);
}
