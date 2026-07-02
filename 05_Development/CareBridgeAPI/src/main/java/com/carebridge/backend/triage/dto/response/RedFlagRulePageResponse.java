package com.carebridge.backend.triage.dto.response;

import java.util.List;

public record RedFlagRulePageResponse(
        List<RedFlagRuleResponse> content,
        long totalElements,
        int page,
        int size) {
}
