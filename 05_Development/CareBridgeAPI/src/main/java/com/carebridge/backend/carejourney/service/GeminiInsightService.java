package com.carebridge.backend.carejourney.service;

import com.carebridge.backend.carejourney.dto.LogTypeSummary;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class GeminiInsightService {

    public CompletableFuture<String> generateInsight(Map<String, LogTypeSummary> summaries, String period) {
        // Stub: fail-open — returns null (Gemini not yet integrated)
        return CompletableFuture.completedFuture(null);
    }
}
