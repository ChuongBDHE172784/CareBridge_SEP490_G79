package com.carebridge.backend.aimoderation.dto.response;

import java.util.List;

public record AiPolicyPageResponse(
        List<AiPolicyResponse> content,
        long totalElements,
        int page,
        int size
) {
}
