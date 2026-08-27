package com.carebridge.backend.health.service;

import com.carebridge.backend.health.entity.MetricType;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

/** Port for async AI metric analysis — implementations must be fail-open (never block metric save). */
public interface MetricAiAnalyzer {

    record InsightResult(String insight, boolean redFlag) {}

    CompletableFuture<InsightResult> analyze(MetricType type, BigDecimal valueNumeric, BigDecimal valueSecondary);
}
