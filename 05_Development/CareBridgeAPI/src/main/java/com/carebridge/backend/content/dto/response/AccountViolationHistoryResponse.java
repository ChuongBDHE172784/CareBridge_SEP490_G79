package com.carebridge.backend.content.dto.response;

import java.util.List;

public record AccountViolationHistoryResponse(
        List<AccountViolationHistoryItemResponse> content,
        long totalElements,
        int page,
        int size
) {}
