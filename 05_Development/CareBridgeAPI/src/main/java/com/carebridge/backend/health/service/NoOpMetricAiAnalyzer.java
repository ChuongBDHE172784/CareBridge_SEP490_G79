package com.carebridge.backend.health.service;

import com.carebridge.backend.health.entity.MetricType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

/** Default no-op AI analyzer — returns null insight immediately. Override with @Primary for real Gemini integration. */
@Component
public class NoOpMetricAiAnalyzer implements MetricAiAnalyzer {

    @Override
    public CompletableFuture<InsightResult> analyze(MetricType type, BigDecimal valueNumeric, BigDecimal valueSecondary) {
        return CompletableFuture.completedFuture(null);
    }
}
