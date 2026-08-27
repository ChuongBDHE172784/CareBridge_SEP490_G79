package com.carebridge.backend.content.dto.response;

import java.util.Map;

public record ContentCountMetrics(long total, Map<String, Long> byStatus, long newInPeriod) {
}
