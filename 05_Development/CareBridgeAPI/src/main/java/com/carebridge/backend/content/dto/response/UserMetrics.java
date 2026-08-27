package com.carebridge.backend.content.dto.response;

import java.util.Map;

public record UserMetrics(long total, Map<String, Long> byRole, long active) {
}
