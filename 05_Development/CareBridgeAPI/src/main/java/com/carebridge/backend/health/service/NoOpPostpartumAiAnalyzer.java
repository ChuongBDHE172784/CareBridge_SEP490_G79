package com.carebridge.backend.health.service;

import com.carebridge.backend.health.entity.BleedingLevel;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

/** Default no-op AI analyzer for postpartum logs. Override with @Primary for real Gemini integration. */
@Component
public class NoOpPostpartumAiAnalyzer implements PostpartumAiAnalyzer {

    @Override
    public CompletableFuture<InsightResult> analyze(Integer painLevel, BleedingLevel bleedingLevel,
                                                     Integer moodLevel, BigDecimal sleepHours) {
        return CompletableFuture.completedFuture(null);
    }
}
