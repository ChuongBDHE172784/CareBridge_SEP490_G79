package com.carebridge.backend.content.dto.response;

import java.util.List;

public record ModerationHistoryResponse(
        List<ModerationHistoryItemResponse> content,
        long totalElements,
        int page,
        int size
) {}
