package com.carebridge.backend.content.dto.response;

import java.util.List;

public record PendingContentQueueResponse(
        List<PendingContentItemResponse> content,
        long totalElements,
        int page,
        int size
) {}
