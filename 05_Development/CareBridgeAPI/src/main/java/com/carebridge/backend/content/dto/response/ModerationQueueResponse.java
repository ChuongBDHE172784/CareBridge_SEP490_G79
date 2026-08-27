package com.carebridge.backend.content.dto.response;

import java.util.List;

public record ModerationQueueResponse(
        List<ModerationQueueItemResponse> content,
        long totalElements,
        int page,
        int size
) {}
