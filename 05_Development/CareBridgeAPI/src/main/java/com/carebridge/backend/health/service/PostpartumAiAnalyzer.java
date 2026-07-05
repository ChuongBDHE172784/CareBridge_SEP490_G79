package com.carebridge.backend.health.service;

import com.carebridge.backend.health.entity.BleedingLevel;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

/** Port for async AI postpartum log analysis — implementations must be fail-open. */
public interface PostpartumAiAnalyzer {

    record InsightResult(String insight, boolean redFlag) {}

    CompletableFuture<InsightResult> analyze(Integer painLevel, BleedingLevel bleedingLevel,
                                              Integer moodLevel, BigDecimal sleepHours);
}
