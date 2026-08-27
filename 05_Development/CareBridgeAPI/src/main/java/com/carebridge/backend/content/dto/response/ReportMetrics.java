package com.carebridge.backend.content.dto.response;

import java.util.Map;

// avgHandlingTimeSeconds is nullable — null (not 0) when there are no resolved reports (TDS §6.3 invariant)
public record ReportMetrics(Map<String, Long> byStatus, Double avgHandlingTimeSeconds) {
}
