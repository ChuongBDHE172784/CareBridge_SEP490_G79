package com.carebridge.backend.safety.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SafetyCountdownJob {
    private final IFallDetectionService fallDetectionService;

    @Scheduled(fixedDelayString = "${carebridge.safety.countdown-poll-ms:1000}")
    public void processExpiredCountdowns() {
        fallDetectionService.processExpiredCountdowns();
    }
}
